package com.example.data.database

import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

object DatabaseInitializer {

    fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    suspend fun populateInitialDataIfEmpty(db: AppDatabase) = withContext(Dispatchers.IO) {
        if (db.userDao().count() > 0) return@withContext

        // 1. Users
        val adminUser = UserEntity(
            username = "admin",
            fullName = "Carlos Mendes (ADM)",
            role = UserRole.ADMIN,
            passwordHash = hashPassword("admin123"),
            email = "admin@ecell.com.br",
            phone = "(11) 99999-0001",
            commissionRate = 0.0
        )
        val tecnicoUser = UserEntity(
            username = "tecnico",
            fullName = "Lucas Silva (Técnico)",
            role = UserRole.TECNICO,
            passwordHash = hashPassword("tec123"),
            email = "lucas.tec@ecell.com.br",
            phone = "(11) 99999-0002",
            commissionRate = 5.0
        )
        val vendedorUser = UserEntity(
            username = "vendedor",
            fullName = "Mariana Costa (Vendedora)",
            role = UserRole.VENDEDOR,
            passwordHash = hashPassword("ven123"),
            email = "mariana.vendas@ecell.com.br",
            phone = "(11) 99999-0003",
            commissionRate = 4.0
        )

        val adminId = db.userDao().insert(adminUser)
        val tecnicoId = db.userDao().insert(tecnicoUser)
        val vendedorId = db.userDao().insert(vendedorUser)

        // 2. Customers
        val customers = listOf(
            CustomerEntity(
                name = "Beatriz Fernandes",
                cpf = "234.567.890-11",
                phone = "11991238877",
                whatsapp = "11991238877",
                email = "beatriz.f@gmail.com",
                address = "Rua das Flores, 142 - Centro",
                notes = "Cliente pontual, tem iPhone 12",
                totalSpent = 485.0,
                ordersCount = 2
            ),
            CustomerEntity(
                name = "João Pedro Almeida",
                cpf = "123.456.789-00",
                phone = "11987654321",
                whatsapp = "11987654321",
                email = "joao.pedro@yahoo.com",
                address = "Av. Paulista, 1000 - Bela Vista",
                notes = "Costuma comprar películas e cabos",
                totalSpent = 230.0,
                ordersCount = 3
            ),
            CustomerEntity(
                name = "Rodrigo Silveira",
                cpf = "345.678.901-22",
                phone = "11976541122",
                whatsapp = "11976541122",
                email = "rodrigo.s@outlook.com",
                address = "Rua Augusta, 520 - Consolação",
                notes = "Empresarial - aparelhos corporativos",
                totalSpent = 740.0,
                ordersCount = 4
            ),
            CustomerEntity(
                name = "Camila Rocha",
                cpf = "456.789.012-33",
                phone = "11988332211",
                whatsapp = "11988332211",
                email = "camila.rocha@gmail.com",
                address = "Alameda Santos, 321 - Jardins",
                notes = "Poco X5 e Galaxy A54",
                totalSpent = 160.0,
                ordersCount = 1
            ),
            CustomerEntity(
                name = "Marcos Vinícius",
                cpf = "567.890.123-44",
                phone = "11977114433",
                whatsapp = "11977114433",
                email = "marcos.v@hotmail.com",
                address = "Rua Vergueiro, 890 - Paraíso",
                notes = "Prefere pagamento via PIX",
                totalSpent = 350.0,
                ordersCount = 2
            )
        )
        val customerIds = customers.map { db.customerDao().insert(it) }

        // 3. Products
        val products = listOf(
            ProductEntity(
                name = "Tela OLED iPhone 13 Incell Premium",
                sku = "TEL-IP13-01",
                barcode = "789100100201",
                category = ProductCategory.TELAS,
                brand = "Apple",
                compatibleModel = "iPhone 13 / 13 Pro",
                description = "Display OLED de altíssima fidelidade de cores e resposta táctil",
                supplier = "Atacado Peças SP",
                costPrice = 180.0,
                salePrice = 420.0,
                stockQuantity = 8,
                minStock = 3,
                location = "Gaveta A1"
            ),
            ProductEntity(
                name = "Tela Frontal Samsung Galaxy A54 5G",
                sku = "TEL-SMA54-01",
                barcode = "789100100202",
                category = ProductCategory.TELAS,
                brand = "Samsung",
                compatibleModel = "Galaxy A54 5G",
                description = "Tela Super AMOLED original nacional com aro",
                supplier = "Atacado Peças SP",
                costPrice = 110.0,
                salePrice = 260.0,
                stockQuantity = 5,
                minStock = 2,
                location = "Gaveta A2"
            ),
            ProductEntity(
                name = "Tela Frontal Motorola Moto G84",
                sku = "TEL-MOTG84-01",
                barcode = "789100100203",
                category = ProductCategory.TELAS,
                brand = "Motorola",
                compatibleModel = "Moto G84 5G",
                description = "Display OLED original importado com excelente brilho",
                supplier = "Distribuidora Mobile Sul",
                costPrice = 95.0,
                salePrice = 230.0,
                stockQuantity = 4,
                minStock = 2,
                location = "Gaveta A3"
            ),
            ProductEntity(
                name = "Bateria Original iPhone 11 (Sem Mensagem)",
                sku = "BAT-IP11-01",
                barcode = "789100100204",
                category = ProductCategory.BATERIAS,
                brand = "Apple",
                compatibleModel = "iPhone 11",
                description = "Bateria com chip reprogramável saúde 100% 3110mAh",
                supplier = "Atacado Peças SP",
                costPrice = 65.0,
                salePrice = 170.0,
                stockQuantity = 7,
                minStock = 3,
                location = "Gaveta B1"
            ),
            ProductEntity(
                name = "Bateria Xiaomi Redmi Note 12 BN50",
                sku = "BAT-XM12-01",
                barcode = "789100100205",
                category = ProductCategory.BATERIAS,
                brand = "Xiaomi",
                compatibleModel = "Redmi Note 12 4G / 5G",
                description = "Bateria de longa duração 5000mAh qualidade Gold",
                supplier = "Distribuidora Mobile Sul",
                costPrice = 50.0,
                salePrice = 140.0,
                stockQuantity = 6,
                minStock = 2,
                location = "Gaveta B2"
            ),
            ProductEntity(
                name = "Conector de Carga USB-C Moto G52 Sub-placa",
                sku = "CON-MG52-01",
                barcode = "789100100206",
                category = ProductCategory.CONECTORES,
                brand = "Motorola",
                compatibleModel = "Moto G52 / G82",
                description = "Sub-placa completa com microfone e conector USB-C",
                supplier = "Atacado Peças SP",
                costPrice = 15.0,
                salePrice = 85.0,
                stockQuantity = 12,
                minStock = 3,
                location = "Gaveta C1"
            ),
            ProductEntity(
                name = "Conector de Carga iPhone Lightning Flex",
                sku = "CON-IPLGT-01",
                barcode = "789100100207",
                category = ProductCategory.CONECTORES,
                brand = "Apple",
                compatibleModel = "iPhone 12 / 12 Pro",
                description = "Flex dock de carga com microfones integrados",
                supplier = "Atacado Peças SP",
                costPrice = 25.0,
                salePrice = 110.0,
                stockQuantity = 2, // Estoque baixo! minStock = 3
                minStock = 3,
                location = "Gaveta C2"
            ),
            ProductEntity(
                name = "Película Vidro 3D Cerâmica iPhone 13/14",
                sku = "PEL-IP13-3D",
                barcode = "789100100208",
                category = ProductCategory.PELICULAS,
                brand = "Ecell",
                compatibleModel = "iPhone 13, 14",
                description = "Película de cerâmica 9D inquebrável com borda preta",
                supplier = "Distribuidora Mobile Sul",
                costPrice = 4.5,
                salePrice = 25.0,
                stockQuantity = 25,
                minStock = 5,
                location = "Prateleira 1"
            ),
            ProductEntity(
                name = "Película Privacidade Samsung Galaxy A54",
                sku = "PEL-SMA54-PRV",
                barcode = "789100100209",
                category = ProductCategory.PELICULAS,
                brand = "Ecell",
                compatibleModel = "Samsung Galaxy A54",
                description = "Película anti-espião fosca com bloqueio lateral",
                supplier = "Distribuidora Mobile Sul",
                costPrice = 7.0,
                salePrice = 35.0,
                stockQuantity = 18,
                minStock = 5,
                location = "Prateleira 1"
            ),
            ProductEntity(
                name = "Capa Anti-Impacto MagSafe Transparente iPhone 13",
                sku = "CAP-MAG-IP13",
                barcode = "789100100210",
                category = ProductCategory.CAPINHAS,
                brand = "Ecell Armor",
                compatibleModel = "iPhone 13",
                description = "Bordas reforçadas anti-queda e anel magnético MagSafe",
                supplier = "Distribuidora Mobile Sul",
                costPrice = 16.0,
                salePrice = 65.0,
                stockQuantity = 15,
                minStock = 4,
                location = "Prateleira 2"
            ),
            ProductEntity(
                name = "Carregador Turbo 20W Power Delivery USB-C",
                sku = "CAR-PD20W-01",
                barcode = "789100100211",
                category = ProductCategory.CARREGADORES,
                brand = "Ecell Power",
                compatibleModel = "Universal Type-C / iPhone / Android",
                description = "Fonte bivolt 20W homologada Anatel com proteção contra sobrecarga",
                supplier = "Distribuidora Mobile Sul",
                costPrice = 22.0,
                salePrice = 75.0,
                stockQuantity = 14,
                minStock = 4,
                location = "Prateleira 3"
            ),
            ProductEntity(
                name = "Cabo Trançado Reforçado Tipo-C para Lightning 1.2m",
                sku = "CAB-C2L-12M",
                barcode = "789100100212",
                category = ProductCategory.CABOS,
                brand = "Ecell Cable",
                compatibleModel = "iPhone / iPad",
                description = "Cabo em nylon trançado com conector metálico 3A rápido",
                supplier = "Distribuidora Mobile Sul",
                costPrice = 8.0,
                salePrice = 35.0,
                stockQuantity = 20,
                minStock = 5,
                location = "Prateleira 3"
            ),
            ProductEntity(
                name = "Fone Bluetooth TWS Ecell Bass Pro",
                sku = "FON-TWS-BASS",
                barcode = "789100100213",
                category = ProductCategory.FONES,
                brand = "Ecell Audio",
                compatibleModel = "Bluetooth 5.3 Universal",
                description = "Fone sem fio com cancelamento de ruído passivo e case com display",
                supplier = "Distribuidora Mobile Sul",
                costPrice = 35.0,
                salePrice = 120.0,
                stockQuantity = 10,
                minStock = 3,
                location = "Vitrine Central"
            )
        )
        val productIds = products.map { db.productDao().insert(it) }

        // 4. Initial Stock Movements
        products.forEachIndexed { index, product ->
            db.stockMovementDao().insert(
                StockMovementEntity(
                    productId = productIds[index],
                    productName = product.name,
                    quantityChange = product.stockQuantity,
                    previousStock = 0,
                    newStock = product.stockQuantity,
                    type = StockMovementType.ENTRADA_COMPRA,
                    reason = "Estoque inicial de implantação",
                    userId = adminId,
                    userName = adminUser.fullName
                )
            )
        }

        // 5. Initial Service Orders
        val now = System.currentTimeMillis()
        val os1 = ServiceOrderEntity(
            orderNumber = "OS-1001",
            customerId = customerIds[0],
            customerName = "Beatriz Fernandes",
            customerPhone = "11991238877",
            phoneBrand = "Apple",
            phoneModel = "iPhone 12",
            imei = "356789102345678",
            reportedDefect = "Tela quebrada com listras verdes verticais após queda.",
            technicalDiagnosis = "Display OLED quebrado internamente. Aro e placa intactos.",
            laborPrice = 120.0,
            partsPrice = 280.0,
            discount = 0.0,
            totalPrice = 400.0,
            technicianId = tecnicoId,
            technicianName = tecnicoUser.fullName,
            entryDateEpoch = now - 86400000L * 2,
            estimatedCompletionEpoch = now - 3600000L * 3,
            status = ServiceOrderStatus.PRONTO,
            notes = "Cliente avisada. Aguardando retirada na recepção."
        )
        val os1Id = db.serviceOrderDao().insertOrder(os1)
        db.serviceOrderDao().insertOrderItems(
            listOf(
                ServiceOrderItemEntity(
                    serviceOrderId = os1Id,
                    productId = productIds[0],
                    productName = "Tela OLED iPhone 13 Incell Premium",
                    quantity = 1,
                    unitCost = 180.0,
                    unitPrice = 280.0
                )
            )
        )

        val os2 = ServiceOrderEntity(
            orderNumber = "OS-1002",
            customerId = customerIds[1],
            customerName = "João Pedro Almeida",
            customerPhone = "11987654321",
            phoneBrand = "Motorola",
            phoneModel = "Moto G52",
            imei = "359876543210987",
            reportedDefect = "Aparelho não carrega nem reconhece cabo turbo.",
            technicalDiagnosis = "Conector de carga oxidado e pinos centrais rompidos.",
            laborPrice = 90.0,
            partsPrice = 45.0,
            discount = 0.0,
            totalPrice = 135.0,
            technicianId = tecnicoId,
            technicianName = tecnicoUser.fullName,
            entryDateEpoch = now - 86400000L,
            estimatedCompletionEpoch = now + 86400000L,
            status = ServiceOrderStatus.EM_MANUTENCAO,
            notes = "Bancada 02. Aguardando testes de carga após solda."
        )
        val os2Id = db.serviceOrderDao().insertOrder(os2)
        db.serviceOrderDao().insertOrderItems(
            listOf(
                ServiceOrderItemEntity(
                    serviceOrderId = os2Id,
                    productId = productIds[5],
                    productName = "Conector de Carga USB-C Moto G52 Sub-placa",
                    quantity = 1,
                    unitCost = 15.0,
                    unitPrice = 45.0
                )
            )
        )

        val os3 = ServiceOrderEntity(
            orderNumber = "OS-1003",
            customerId = customerIds[2],
            customerName = "Rodrigo Silveira",
            customerPhone = "11976541122",
            phoneBrand = "Apple",
            phoneModel = "iPhone 11",
            imei = "354321098765432",
            reportedDefect = "Bateria estufada, descarregando com 30% e desligando.",
            technicalDiagnosis = "Saúde da bateria em 68% com ciclos esgotados. Necessita troca urgente.",
            laborPrice = 70.0,
            partsPrice = 170.0,
            discount = 10.0,
            totalPrice = 230.0,
            technicianId = tecnicoId,
            technicianName = tecnicoUser.fullName,
            entryDateEpoch = now - 3600000L * 5,
            estimatedCompletionEpoch = now + 86400000L * 2,
            status = ServiceOrderStatus.AGUARDANDO_APROVACAO,
            notes = "Orçamento enviado via WhatsApp."
        )
        val os3Id = db.serviceOrderDao().insertOrder(os3)

        val os4 = ServiceOrderEntity(
            orderNumber = "OS-1004",
            customerId = customerIds[3],
            customerName = "Camila Rocha",
            customerPhone = "11988332211",
            phoneBrand = "Apple",
            phoneModel = "iPhone 12",
            imei = "351122334455667",
            reportedDefect = "Dock de carga não segura cabo.",
            technicalDiagnosis = "Conector Lightning frouxo com desgaste nas travas.",
            laborPrice = 80.0,
            partsPrice = 110.0,
            discount = 0.0,
            totalPrice = 190.0,
            technicianId = tecnicoId,
            technicianName = tecnicoUser.fullName,
            entryDateEpoch = now - 3600000L * 2,
            estimatedCompletionEpoch = now + 86400000L * 3,
            status = ServiceOrderStatus.AGUARDANDO_PECA,
            notes = "Peça solicitada ao fornecedor SP."
        )
        val os4Id = db.serviceOrderDao().insertOrder(os4)

        // 6. Sample Sales
        val sale1 = SaleEntity(
            saleNumber = "VND-1001",
            customerId = customerIds[1],
            customerName = "João Pedro Almeida",
            sellerId = vendedorId,
            sellerName = vendedorUser.fullName,
            dateEpoch = now - 86400000L * 2,
            subtotal = 110.0,
            discount = 10.0,
            total = 100.0,
            totalCost = 30.0,
            paymentMethod = PaymentMethod.PIX,
            amountPaid = 100.0,
            pendingBalance = 0.0,
            status = SaleStatus.CONFIRMADA
        )
        val sale1Id = db.saleDao().insertSale(sale1)
        db.saleDao().insertSaleItems(
            listOf(
                SaleItemEntity(
                    saleId = sale1Id,
                    productId = productIds[10], // Carregador 20W
                    productName = "Carregador Turbo 20W Power Delivery USB-C",
                    quantity = 1,
                    unitCost = 22.0,
                    unitPrice = 75.0,
                    subtotal = 75.0,
                    totalProfit = 53.0
                ),
                SaleItemEntity(
                    saleId = sale1Id,
                    productId = productIds[11], // Cabo
                    productName = "Cabo Trançado Reforçado Tipo-C para Lightning 1.2m",
                    quantity = 1,
                    unitCost = 8.0,
                    unitPrice = 35.0,
                    subtotal = 35.0,
                    totalProfit = 27.0
                )
            )
        )

        val sale2 = SaleEntity(
            saleNumber = "VND-1002",
            customerId = customerIds[4],
            customerName = "Marcos Vinícius",
            sellerId = vendedorId,
            sellerName = vendedorUser.fullName,
            dateEpoch = now - 86400000L,
            subtotal = 210.0,
            discount = 0.0,
            total = 210.0,
            totalCost = 55.5,
            paymentMethod = PaymentMethod.CARTAO_CREDITO,
            amountPaid = 210.0,
            pendingBalance = 0.0,
            status = SaleStatus.CONFIRMADA
        )
        val sale2Id = db.saleDao().insertSale(sale2)
        db.saleDao().insertSaleItems(
            listOf(
                SaleItemEntity(
                    saleId = sale2Id,
                    productId = productIds[12], // Fone Bluetooth
                    productName = "Fone Bluetooth TWS Ecell Bass Pro",
                    quantity = 1,
                    unitCost = 35.0,
                    unitPrice = 120.0,
                    subtotal = 120.0,
                    totalProfit = 85.0
                ),
                SaleItemEntity(
                    saleId = sale2Id,
                    productId = productIds[9], // Capa MagSafe
                    productName = "Capa Anti-Impacto MagSafe Transparente iPhone 13",
                    quantity = 1,
                    unitCost = 16.0,
                    unitPrice = 65.0,
                    subtotal = 65.0,
                    totalProfit = 49.0
                ),
                SaleItemEntity(
                    saleId = sale2Id,
                    productId = productIds[7], // Película Vidro
                    productName = "Película Vidro 3D Cerâmica iPhone 13/14",
                    quantity = 1,
                    unitCost = 4.5,
                    unitPrice = 25.0,
                    subtotal = 25.0,
                    totalProfit = 20.5
                )
            )
        )

        val sale3 = SaleEntity(
            saleNumber = "VND-1003",
            customerId = null,
            customerName = "Consumidor Final (Balcão)",
            sellerId = adminId,
            sellerName = adminUser.fullName,
            dateEpoch = now - 3600000L * 4,
            subtotal = 60.0,
            discount = 0.0,
            total = 60.0,
            totalCost = 11.5,
            paymentMethod = PaymentMethod.DINHEIRO,
            amountPaid = 60.0,
            pendingBalance = 0.0,
            status = SaleStatus.CONFIRMADA
        )
        val sale3Id = db.saleDao().insertSale(sale3)
        db.saleDao().insertSaleItems(
            listOf(
                SaleItemEntity(
                    saleId = sale3Id,
                    productId = productIds[7], // Película
                    productName = "Película Vidro 3D Cerâmica iPhone 13/14",
                    quantity = 1,
                    unitCost = 4.5,
                    unitPrice = 25.0,
                    subtotal = 25.0,
                    totalProfit = 20.5
                ),
                SaleItemEntity(
                    saleId = sale3Id,
                    productId = productIds[8], // Película Privacidade
                    productName = "Película Privacidade Samsung Galaxy A54",
                    quantity = 1,
                    unitCost = 7.0,
                    unitPrice = 35.0,
                    subtotal = 35.0,
                    totalProfit = 28.0
                )
            )
        )

        // 7. Expenses
        val expenses = listOf(
            ExpenseEntity(
                description = "Aluguel Loja e Assistência Técnica",
                category = ExpenseCategory.ALUGUEL,
                amount = 2200.0,
                dueDateEpoch = now + 86400000L * 10,
                paidDateEpoch = null,
                paymentMethod = PaymentMethod.TRANSFERENCIA,
                status = ExpenseStatus.PENDENTE,
                notes = "Imobiliária Central"
            ),
            ExpenseEntity(
                description = "Energia Elétrica Enel",
                category = ExpenseCategory.ENERGIA,
                amount = 345.80,
                dueDateEpoch = now - 86400000L * 2,
                paidDateEpoch = now - 86400000L * 2,
                paymentMethod = PaymentMethod.PIX,
                status = ExpenseStatus.PAGO,
                notes = "Conta com ar condicionado e bancada de solda"
            ),
            ExpenseEntity(
                description = "Internet Fibra Óptica 500MB",
                category = ExpenseCategory.INTERNET,
                amount = 119.90,
                dueDateEpoch = now - 86400000L * 5,
                paidDateEpoch = now - 86400000L * 5,
                paymentMethod = PaymentMethod.PIX,
                status = ExpenseStatus.PAGO,
                notes = "Conexão dedicada"
            ),
            ExpenseEntity(
                description = "Insumos de Bancada (Álcool Isopropílico, Fitas, Estanho)",
                category = ExpenseCategory.CONSUMO,
                amount = 165.00,
                dueDateEpoch = now - 86400000L * 8,
                paidDateEpoch = now - 86400000L * 8,
                paymentMethod = PaymentMethod.DINHEIRO,
                status = ExpenseStatus.PAGO,
                notes = "Loja de eletrônica local"
            )
        )
        expenses.forEach { db.expenseDao().insert(it) }

        // 8. Suppliers
        val sup1 = SupplierEntity(
            name = "Carlos Peças",
            companyName = "Atacado Peças SP Ltda",
            phone = "11988881111",
            whatsapp = "11988881111",
            email = "vendas@atacadotechsp.com.br",
            notes = "Fornecedor oficial de telas OLED e baterias premium"
        )
        val sup2 = SupplierEntity(
            name = "Fernando Oliveira",
            companyName = "Distribuidora Mobile Sul",
            phone = "11977772222",
            whatsapp = "11977772222",
            email = "contato@mobilesul.com.br",
            notes = "Acessórios, carregadores e películas no atacado"
        )
        db.supplierDao().insert(sup1)
        db.supplierDao().insert(sup2)

        // 9. Initial Audit Log
        db.auditLogDao().insert(
            AuditLogEntity(
                userId = adminId,
                userName = adminUser.fullName,
                action = "IMPLANTACAO_SISTEMA",
                details = "Inicialização da base de dados e cadastro de dados operacionais e de demonstração do ECELL GESTÃO"
            )
        )
    }
}
