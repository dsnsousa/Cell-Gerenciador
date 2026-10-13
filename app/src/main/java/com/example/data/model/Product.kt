package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class ProductCategory(val label: String) {
    TELAS("Telas"),
    BATERIAS("Baterias"),
    CONECTORES("Conectores de carga"),
    FLEXIVEIS("Flexíveis"),
    CAMERAS("Câmeras"),
    ALTO_FALANTES("Alto-falantes"),
    MICROFONES("Microfones"),
    CAPINHAS("Capinhas"),
    PELICULAS("Películas"),
    CARREGADORES("Carregadores"),
    CABOS("Cabos"),
    FONES("Fones de ouvido"),
    ACESSORIOS_DIVERSOS("Acessórios diversos"),
    OUTROS("Outros")
}

@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val sku: String,
    val barcode: String = "",
    val category: ProductCategory,
    val brand: String = "",
    val compatibleModel: String = "",
    val description: String = "",
    val supplier: String = "",
    val costPrice: Double,
    val salePrice: Double,
    val stockQuantity: Int,
    val minStock: Int = 3,
    val location: String = "",
    val imageUrl: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
