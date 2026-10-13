package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ServiceOrderStatus(val label: String, val colorHex: Long) {
    RECEBIDO("Aparelho recebido", 0xFF64748B),
    DIAGNOSTICO("Em diagnóstico", 0xFF3B82F6),
    AGUARDANDO_APROVACAO("Aguardando aprovação", 0xFFF59E0B),
    AGUARDANDO_PECA("Aguardando peça", 0xFFEC4899),
    EM_MANUTENCAO("Em manutenção", 0xFF8B5CF6),
    AGUARDANDO_PAGAMENTO("Aguardando pagamento", 0xFF0284C7),
    PRONTO("Pronto para retirada", 0xFF10B981),
    ENTREGUE("Entregue", 0xFF059669),
    CANCELADO("Cancelado", 0xFFEF4444)
}

@Entity(tableName = "service_orders")
data class ServiceOrderEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val orderNumber: String,
    val customerId: Long? = null,
    val customerName: String,
    val customerPhone: String,
    val phoneBrand: String,
    val phoneModel: String,
    val imei: String = "",
    val reportedDefect: String,
    val technicalDiagnosis: String = "",
    val laborPrice: Double = 0.0,
    val partsPrice: Double = 0.0,
    val discount: Double = 0.0,
    val totalPrice: Double = 0.0,
    val technicianId: Long,
    val technicianName: String,
    val entryDateEpoch: Long = System.currentTimeMillis(),
    val estimatedCompletionEpoch: Long = System.currentTimeMillis() + 86400000L * 2,
    val completedDateEpoch: Long? = null,
    val status: ServiceOrderStatus = ServiceOrderStatus.RECEBIDO,
    val notes: String = ""
)

@Entity(
    tableName = "service_order_items",
    indices = [Index(value = ["serviceOrderId"])]
)
data class ServiceOrderItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val serviceOrderId: Long,
    val productId: Long,
    val productName: String,
    val quantity: Int,
    val unitCost: Double,
    val unitPrice: Double
)
