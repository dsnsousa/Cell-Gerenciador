package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class StockMovementType(val label: String) {
    ENTRADA_COMPRA("Entrada por Compra"),
    SAIDA_VENDA("Saída por Venda"),
    SAIDA_OS("Saída por Reparo (OS)"),
    DEVOLUCAO("Devolução ao Estoque"),
    AJUSTE_INVENTARIO("Ajuste de Inventário"),
    PERDA_AVARIA("Perda / Avaria")
}

@Entity(tableName = "stock_movements")
data class StockMovementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val productId: Long,
    val productName: String,
    val quantityChange: Int,
    val previousStock: Int,
    val newStock: Int,
    val type: StockMovementType,
    val reason: String = "",
    val userId: Long,
    val userName: String,
    val dateEpoch: Long = System.currentTimeMillis()
)
