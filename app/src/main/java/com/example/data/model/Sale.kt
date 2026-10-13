package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class PaymentMethod(val label: String) {
    DINHEIRO("Dinheiro"),
    PIX("PIX"),
    CARTAO_DEBITO("Cartão de Débito"),
    CARTAO_CREDITO("Cartão de Crédito"),
    TRANSFERENCIA("Transferência"),
    MISTO("Pagamento Misto"),
    FIADO("Fiado / A Receber")
}

enum class SaleStatus(val label: String) {
    CONFIRMADA("Confirmada"),
    CANCELADA("Cancelada")
}

@Entity(tableName = "sales")
data class SaleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val saleNumber: String,
    val customerId: Long? = null,
    val customerName: String = "Consumidor Final",
    val sellerId: Long,
    val sellerName: String,
    val dateEpoch: Long = System.currentTimeMillis(),
    val subtotal: Double,
    val discount: Double = 0.0,
    val total: Double,
    val totalCost: Double = 0.0,
    val paymentMethod: PaymentMethod,
    val amountPaid: Double,
    val pendingBalance: Double = 0.0,
    val status: SaleStatus = SaleStatus.CONFIRMADA,
    val cancellationReason: String = "",
    val cancelledBy: String = "",
    val cancelledAtEpoch: Long? = null
)

@Entity(
    tableName = "sale_items",
    indices = [Index(value = ["saleId"])]
)
data class SaleItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val saleId: Long,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val unitCost: Double,
    val unitPrice: Double,
    val subtotal: Double,
    val totalProfit: Double
)
