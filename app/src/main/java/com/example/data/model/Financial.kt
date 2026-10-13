package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ExpenseCategory(val label: String) {
    ALUGUEL("Aluguel"),
    ENERGIA("Energia elétrica"),
    INTERNET("Internet / Telefonia"),
    FRETE("Frete"),
    FERRAMENTAS("Ferramentas de bancada"),
    CONSUMO("Materiais de consumo"),
    MARKETING("Marketing / Publicidade"),
    TAXAS_CARTAO("Taxas de maquininha"),
    OUTRAS("Outras despesas")
}

enum class ExpenseStatus(val label: String) {
    PAGO("Pago"),
    PENDENTE("Pendente"),
    VENCIDO("Vencido")
}

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val description: String,
    val category: ExpenseCategory,
    val amount: Double,
    val dueDateEpoch: Long,
    val paidDateEpoch: Long? = null,
    val paymentMethod: PaymentMethod = PaymentMethod.PIX,
    val status: ExpenseStatus = ExpenseStatus.PAGO,
    val notes: String = ""
)

@Entity(tableName = "suppliers")
data class SupplierEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val companyName: String = "",
    val phone: String = "",
    val whatsapp: String = "",
    val email: String = "",
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "purchases")
data class PurchaseEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val supplierId: Long,
    val supplierName: String,
    val dateEpoch: Long = System.currentTimeMillis(),
    val invoiceNumber: String = "",
    val totalAmount: Double,
    val paymentMethod: PaymentMethod = PaymentMethod.PIX,
    val paymentStatus: String = "PAGO",
    val notes: String = ""
)
