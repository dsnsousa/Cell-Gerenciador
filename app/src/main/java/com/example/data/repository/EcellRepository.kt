package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.database.AppDatabase
import com.example.data.database.DatabaseInitializer
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class EcellRepository(private val db: AppDatabase) {

    // --- USERS & AUTH ---
    fun getAllUsers(): Flow<List<UserEntity>> = db.userDao().getAllUsers()

    suspend fun getUserById(id: Long): UserEntity? = db.userDao().getUserById(id)

    suspend fun authenticate(username: String, password: String): UserEntity? {
        val user = db.userDao().getUserByUsername(username.trim()) ?: return null
        val hash = DatabaseInitializer.hashPassword(password)
        return if (user.passwordHash == hash && user.isActive) user else null
    }

    suspend fun saveUser(user: UserEntity, currentAdmin: UserEntity): Long {
        val id = if (user.id == 0L) {
            db.userDao().insert(user)
        } else {
            db.userDao().update(user)
            user.id
        }
        db.auditLogDao().insert(
            AuditLogEntity(
                userId = currentAdmin.id,
                userName = currentAdmin.fullName,
                action = if (user.id == 0L) "CRIAR_USUARIO" else "EDITAR_USUARIO",
                details = "Usuário ${user.username} (${user.role.label}) salvo"
            )
        )
        return id
    }

    suspend fun toggleUserActive(userId: Long, currentAdmin: UserEntity) {
        val user = db.userDao().getUserById(userId) ?: return
        val updated = user.copy(isActive = !user.isActive)
        db.userDao().update(updated)
        db.auditLogDao().insert(
            AuditLogEntity(
                userId = currentAdmin.id,
                userName = currentAdmin.fullName,
                action = "STATUS_USUARIO",
                details = "Usuário ${user.username} agora está ${if (updated.isActive) "Ativo" else "Inativo"}"
            )
        )
    }

    // --- CUSTOMERS ---
    fun getAllCustomers(): Flow<List<CustomerEntity>> = db.customerDao().getAll()

    fun searchCustomers(query: String): Flow<List<CustomerEntity>> = db.customerDao().search(query)

    suspend fun getCustomerById(id: Long): CustomerEntity? = db.customerDao().getById(id)

    suspend fun saveCustomer(customer: CustomerEntity): Long {
        return if (customer.id == 0L) {
            db.customerDao().insert(customer)
        } else {
            db.customerDao().update(customer)
            customer.id
        }
    }

    // --- PRODUCTS & INVENTORY ---
    fun getAllProducts(): Flow<List<ProductEntity>> = db.productDao().getAll()

    fun getLowStockProducts(): Flow<List<ProductEntity>> = db.productDao().getLowStock()

    fun searchProducts(query: String): Flow<List<ProductEntity>> = db.productDao().search(query)

    suspend fun getProductById(id: Long): ProductEntity? = db.productDao().getById(id)

    suspend fun saveProduct(product: ProductEntity, user: UserEntity): Long {
        val isNew = product.id == 0L
        val id = if (isNew) {
            val insertedId = db.productDao().insert(product)
            db.stockMovementDao().insert(
                StockMovementEntity(
                    productId = insertedId,
                    productName = product.name,
                    quantityChange = product.stockQuantity,
                    previousStock = 0,
                    newStock = product.stockQuantity,
                    type = StockMovementType.ENTRADA_COMPRA,
                    reason = "Cadastro inicial do produto",
                    userId = user.id,
                    userName = user.fullName
                )
            )
            insertedId
        } else {
            val old = db.productDao().getById(product.id)
            db.productDao().update(product)
            if (old != null && old.stockQuantity != product.stockQuantity) {
                val diff = product.stockQuantity - old.stockQuantity
                db.stockMovementDao().insert(
                    StockMovementEntity(
                        productId = product.id,
                        productName = product.name,
                        quantityChange = diff,
                        previousStock = old.stockQuantity,
                        newStock = product.stockQuantity,
                        type = StockMovementType.AJUSTE_INVENTARIO,
                        reason = "Ajuste manual de estoque no cadastro",
                        userId = user.id,
                        userName = user.fullName
                    )
                )
            }
            product.id
        }
        return id
    }

    suspend fun adjustStock(
        productId: Long,
        quantityChange: Int,
        type: StockMovementType,
        reason: String,
        user: UserEntity
    ): Result<Unit> = runCatching {
        val product = db.productDao().getById(productId) ?: error("Produto não encontrado")
        val newStock = product.stockQuantity + quantityChange
        if (newStock < 0 && user.role != UserRole.ADMIN) {
            error("Estoque não pode ficar negativo sem autorização de Administrador")
        }
        db.productDao().updateStock(productId, newStock)
        db.stockMovementDao().insert(
            StockMovementEntity(
                productId = productId,
                productName = product.name,
                quantityChange = quantityChange,
                previousStock = product.stockQuantity,
                newStock = newStock,
                type = type,
                reason = reason,
                userId = user.id,
                userName = user.fullName
            )
        )
    }

    fun getAllStockMovements(): Flow<List<StockMovementEntity>> = db.stockMovementDao().getAll()

    // --- SALES (PDV) ---
    data class CartItem(
        val product: ProductEntity,
        var quantity: Int
    )

    fun getAllSales(): Flow<List<SaleEntity>> = db.saleDao().getAllSales()

    suspend fun getSaleItems(saleId: Long): List<SaleItemEntity> = db.saleDao().getSaleItems(saleId)

    suspend fun confirmSale(
        customer: CustomerEntity?,
        seller: UserEntity,
        items: List<CartItem>,
        discount: Double,
        paymentMethod: PaymentMethod
    ): Result<SaleEntity> = runCatching {
        if (items.isEmpty()) error("Carrinho vazio")

        // 1. Validate real-time inventory
        for (item in items) {
            val freshProduct = db.productDao().getById(item.product.id)
                ?: error("Produto ${item.product.name} não encontrado")
            if (freshProduct.stockQuantity < item.quantity) {
                error("Estoque insuficiente para ${freshProduct.name}. Disponível: ${freshProduct.stockQuantity}")
            }
        }

        // 2. Validate discount rules
        val subtotal = items.sumOf { it.product.salePrice * it.quantity }
        val maxDiscountPercent = when (seller.role) {
            UserRole.ADMIN -> 100.0
            UserRole.VENDEDOR -> 10.0 // Vendedores até 10%
            UserRole.TECNICO -> 15.0
        }
        val discountPercent = if (subtotal > 0) (discount / subtotal) * 100.0 else 0.0
        if (discountPercent > maxDiscountPercent) {
            error("Desconto de ${"%.1f".format(discountPercent)}% excede o limite permitido de ${maxDiscountPercent}% para seu perfil.")
        }

        val total = (subtotal - discount).coerceAtLeast(0.0)
        val totalCost = items.sumOf { it.product.costPrice * it.quantity }
        val amountPaid = if (paymentMethod == PaymentMethod.FIADO) 0.0 else total
        val pendingBalance = if (paymentMethod == PaymentMethod.FIADO) total else 0.0

        val saleNumber = "VND-${System.currentTimeMillis() % 1000000}"

        // Room transaction ensures atomicity
        db.withTransaction {
            val sale = SaleEntity(
                saleNumber = saleNumber,
                customerId = customer?.id,
                customerName = customer?.name ?: "Consumidor Final",
                sellerId = seller.id,
                sellerName = seller.fullName,
                dateEpoch = System.currentTimeMillis(),
                subtotal = subtotal,
                discount = discount,
                total = total,
                totalCost = totalCost,
                paymentMethod = paymentMethod,
                amountPaid = amountPaid,
                pendingBalance = pendingBalance,
                status = SaleStatus.CONFIRMADA
            )
            val saleId = db.saleDao().insertSale(sale)

            val saleItems = items.map { item ->
                SaleItemEntity(
                    saleId = saleId,
                    productId = item.product.id,
                    productName = item.product.name,
                    quantity = item.quantity,
                    unitCost = item.product.costPrice,
                    unitPrice = item.product.salePrice,
                    subtotal = item.product.salePrice * item.quantity,
                    totalProfit = (item.product.salePrice - item.product.costPrice) * item.quantity
                )
            }
            db.saleDao().insertSaleItems(saleItems)

            // Decrement stock and record movements
            for (item in items) {
                val current = db.productDao().getById(item.product.id)!!
                val newStock = current.stockQuantity - item.quantity
                db.productDao().updateStock(item.product.id, newStock)
                db.stockMovementDao().insert(
                    StockMovementEntity(
                        productId = item.product.id,
                        productName = item.product.name,
                        quantityChange = -item.quantity,
                        previousStock = current.stockQuantity,
                        newStock = newStock,
                        type = StockMovementType.SAIDA_VENDA,
                        reason = "Venda $saleNumber",
                        userId = seller.id,
                        userName = seller.fullName
                    )
                )
            }

            // Update customer totals if identified
            customer?.let {
                val updatedCustomer = it.copy(
                    totalSpent = it.totalSpent + total,
                    ordersCount = it.ordersCount + 1,
                    lastVisitEpoch = System.currentTimeMillis()
                )
                db.customerDao().update(updatedCustomer)
            }

            db.auditLogDao().insert(
                AuditLogEntity(
                    userId = seller.id,
                    userName = seller.fullName,
                    action = "VENDA_REALIZADA",
                    details = "Venda $saleNumber no valor de R$ ${"%.2f".format(total)} para ${sale.customerName}"
                )
            )

            sale.copy(id = saleId)
        }
    }

    suspend fun cancelSale(saleId: Long, reason: String, currentAdmin: UserEntity): Result<Unit> = runCatching {
        if (currentAdmin.role != UserRole.ADMIN) {
            error("Apenas Administradores podem cancelar vendas")
        }
        val sale = db.saleDao().getSaleById(saleId) ?: error("Venda não encontrada")
        if (sale.status == SaleStatus.CANCELADA) error("Esta venda já foi cancelada")

        db.withTransaction {
            val updated = sale.copy(
                status = SaleStatus.CANCELADA,
                cancellationReason = reason,
                cancelledBy = currentAdmin.fullName,
                cancelledAtEpoch = System.currentTimeMillis()
            )
            db.saleDao().updateSale(updated)

            // Restore items to stock
            val items = db.saleDao().getSaleItems(saleId)
            for (item in items) {
                val product = db.productDao().getById(item.productId)
                if (product != null) {
                    val newStock = product.stockQuantity + item.quantity
                    db.productDao().updateStock(product.id, newStock)
                    db.stockMovementDao().insert(
                        StockMovementEntity(
                            productId = product.id,
                            productName = product.name,
                            quantityChange = item.quantity,
                            previousStock = product.stockQuantity,
                            newStock = newStock,
                            type = StockMovementType.DEVOLUCAO,
                            reason = "Estorno por cancelamento da venda ${sale.saleNumber}: $reason",
                            userId = currentAdmin.id,
                            userName = currentAdmin.fullName
                        )
                    )
                }
            }

            // Reverse customer totalSpent
            if (sale.customerId != null) {
                val customer = db.customerDao().getById(sale.customerId)
                if (customer != null) {
                    db.customerDao().update(
                        customer.copy(
                            totalSpent = (customer.totalSpent - sale.total).coerceAtLeast(0.0),
                            ordersCount = (customer.ordersCount - 1).coerceAtLeast(0)
                        )
                    )
                }
            }

            db.auditLogDao().insert(
                AuditLogEntity(
                    userId = currentAdmin.id,
                    userName = currentAdmin.fullName,
                    action = "CANCELAMENTO_VENDA",
                    details = "Cancelada venda ${sale.saleNumber} (R$ ${"%.2f".format(sale.total)}). Motivo: $reason"
                )
            )
        }
    }

    // --- SERVICE ORDERS (ASSISTÊNCIA TÉCNICA) ---
    fun getAllServiceOrders(): Flow<List<ServiceOrderEntity>> = db.serviceOrderDao().getAll()

    suspend fun getServiceOrderById(id: Long): ServiceOrderEntity? = db.serviceOrderDao().getById(id)

    suspend fun getServiceOrderItems(orderId: Long): List<ServiceOrderItemEntity> =
        db.serviceOrderDao().getItems(orderId)

    suspend fun saveServiceOrder(
        order: ServiceOrderEntity,
        parts: List<CartItem>,
        user: UserEntity
    ): Long {
        return db.withTransaction {
            val isNew = order.id == 0L
            val orderId = if (isNew) {
                db.serviceOrderDao().insertOrder(order)
            } else {
                db.serviceOrderDao().updateOrder(order)
                order.id
            }

            if (parts.isNotEmpty()) {
                val items = parts.map { part ->
                    ServiceOrderItemEntity(
                        serviceOrderId = orderId,
                        productId = part.product.id,
                        productName = part.product.name,
                        quantity = part.quantity,
                        unitCost = part.product.costPrice,
                        unitPrice = part.product.salePrice
                    )
                }
                db.serviceOrderDao().insertOrderItems(items)

                // Decrement stock for parts used
                for (part in parts) {
                    val current = db.productDao().getById(part.product.id)
                    if (current != null) {
                        val newStock = current.stockQuantity - part.quantity
                        db.productDao().updateStock(current.id, newStock)
                        db.stockMovementDao().insert(
                            StockMovementEntity(
                                productId = current.id,
                                productName = current.name,
                                quantityChange = -part.quantity,
                                previousStock = current.stockQuantity,
                                newStock = newStock,
                                type = StockMovementType.SAIDA_OS,
                                reason = "Utilizado na OS ${order.orderNumber}",
                                userId = user.id,
                                userName = user.fullName
                            )
                        )
                    }
                }
            }

            db.auditLogDao().insert(
                AuditLogEntity(
                    userId = user.id,
                    userName = user.fullName,
                    action = if (isNew) "CRIAR_OS" else "EDITAR_OS",
                    details = "OS ${order.orderNumber} para ${order.customerName} (${order.phoneModel})"
                )
            )

            orderId
        }
    }

    suspend fun updateServiceOrderStatus(
        orderId: Long,
        newStatus: ServiceOrderStatus,
        user: UserEntity
    ) {
        val order = db.serviceOrderDao().getById(orderId) ?: return
        val updated = order.copy(
            status = newStatus,
            completedDateEpoch = if (newStatus == ServiceOrderStatus.ENTREGUE || newStatus == ServiceOrderStatus.PRONTO) System.currentTimeMillis() else order.completedDateEpoch
        )
        db.serviceOrderDao().updateOrder(updated)
        db.auditLogDao().insert(
            AuditLogEntity(
                userId = user.id,
                userName = user.fullName,
                action = "STATUS_OS",
                details = "OS ${order.orderNumber} alterada para ${newStatus.label}"
            )
        )
    }

    // --- EXPENSES ---
    fun getAllExpenses(): Flow<List<ExpenseEntity>> = db.expenseDao().getAll()

    suspend fun saveExpense(expense: ExpenseEntity, user: UserEntity): Long {
        val id = if (expense.id == 0L) {
            db.expenseDao().insert(expense)
        } else {
            db.expenseDao().update(expense)
            expense.id
        }
        db.auditLogDao().insert(
            AuditLogEntity(
                userId = user.id,
                userName = user.fullName,
                action = "DESPESA_REGISTRADA",
                details = "${expense.description} - R$ ${"%.2f".format(expense.amount)} (${expense.status.label})"
            )
        )
        return id
    }

    suspend fun deleteExpense(expense: ExpenseEntity, user: UserEntity) {
        db.expenseDao().delete(expense)
        db.auditLogDao().insert(
            AuditLogEntity(
                userId = user.id,
                userName = user.fullName,
                action = "DESPESA_EXCLUIDA",
                details = "${expense.description} - R$ ${"%.2f".format(expense.amount)}"
            )
        )
    }

    // --- SUPPLIERS & PURCHASES ---
    fun getAllSuppliers(): Flow<List<SupplierEntity>> = db.supplierDao().getAll()

    suspend fun saveSupplier(supplier: SupplierEntity): Long {
        return if (supplier.id == 0L) {
            db.supplierDao().insert(supplier)
        } else {
            db.supplierDao().update(supplier)
            supplier.id
        }
    }

    fun getAllPurchases(): Flow<List<PurchaseEntity>> = db.supplierDao().getAllPurchases()

    suspend fun registerPurchase(
        purchase: PurchaseEntity,
        items: List<Pair<Long, Int>>, // productId to quantity
        user: UserEntity
    ): Long = db.withTransaction {
        val purchaseId = db.supplierDao().insertPurchase(purchase)

        for ((productId, quantity) in items) {
            val product = db.productDao().getById(productId)
            if (product != null) {
                val newStock = product.stockQuantity + quantity
                db.productDao().updateStock(productId, newStock)
                db.stockMovementDao().insert(
                    StockMovementEntity(
                        productId = productId,
                        productName = product.name,
                        quantityChange = quantity,
                        previousStock = product.stockQuantity,
                        newStock = newStock,
                        type = StockMovementType.ENTRADA_COMPRA,
                        reason = "Compra NF ${purchase.invoiceNumber} (${purchase.supplierName})",
                        userId = user.id,
                        userName = user.fullName
                    )
                )
            }
        }

        db.auditLogDao().insert(
            AuditLogEntity(
                userId = user.id,
                userName = user.fullName,
                action = "COMPRA_MERCADORIA",
                details = "Compra de R$ ${"%.2f".format(purchase.totalAmount)} do fornecedor ${purchase.supplierName}"
            )
        )

        purchaseId
    }

    // --- AUDIT LOGS ---
    fun getAllAuditLogs(): Flow<List<AuditLogEntity>> = db.auditLogDao().getAll()
}
