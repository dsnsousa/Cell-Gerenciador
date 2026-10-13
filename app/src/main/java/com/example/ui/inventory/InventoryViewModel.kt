package com.example.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.EcellRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class InventoryUiState(
    val products: List<ProductEntity> = emptyList(),
    val filteredProducts: List<ProductEntity> = emptyList(),
    val stockMovements: List<StockMovementEntity> = emptyList(),
    val selectedCategory: ProductCategory? = null,
    val searchQuery: String = "",
    val onlyLowStock: Boolean = false,
    val isProductFormOpen: Boolean = false,
    val editingProduct: ProductEntity? = null,
    val isMovementFormOpen: Boolean = false,
    val selectedProductForMovement: ProductEntity? = null,
    val errorMessage: String? = null
)

class InventoryViewModel(
    private val repository: EcellRepository,
    val currentUser: UserEntity
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryUiState())
    val uiState: StateFlow<InventoryUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                repository.getAllProducts(),
                repository.getAllStockMovements()
            ) { products, movements ->
                Pair(products, movements)
            }.collect { (products, movements) ->
                _uiState.update { state ->
                    state.copy(
                        products = products,
                        stockMovements = movements,
                        filteredProducts = filterProducts(products, state.searchQuery, state.selectedCategory, state.onlyLowStock)
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredProducts = filterProducts(state.products, query, state.selectedCategory, state.onlyLowStock)
            )
        }
    }

    fun onCategorySelected(category: ProductCategory?) {
        _uiState.update { state ->
            state.copy(
                selectedCategory = category,
                filteredProducts = filterProducts(state.products, state.searchQuery, category, state.onlyLowStock)
            )
        }
    }

    fun toggleOnlyLowStock(enabled: Boolean) {
        _uiState.update { state ->
            state.copy(
                onlyLowStock = enabled,
                filteredProducts = filterProducts(state.products, state.searchQuery, state.selectedCategory, enabled)
            )
        }
    }

    private fun filterProducts(
        products: List<ProductEntity>,
        query: String,
        category: ProductCategory?,
        onlyLowStock: Boolean
    ): List<ProductEntity> {
        return products.filter { p ->
            val matchesCategory = category == null || p.category == category
            val matchesLowStock = !onlyLowStock || p.stockQuantity <= p.minStock
            val matchesQuery = query.isBlank() ||
                    p.name.contains(query, ignoreCase = true) ||
                    p.sku.contains(query, ignoreCase = true) ||
                    p.barcode.contains(query, ignoreCase = true) ||
                    p.compatibleModel.contains(query, ignoreCase = true)
            matchesCategory && matchesLowStock && matchesQuery
        }
    }

    fun openNewProductForm() {
        _uiState.update { it.copy(isProductFormOpen = true, editingProduct = null) }
    }

    fun openEditProductForm(product: ProductEntity) {
        _uiState.update { it.copy(isProductFormOpen = true, editingProduct = product) }
    }

    fun closeProductForm() {
        _uiState.update { it.copy(isProductFormOpen = false, editingProduct = null) }
    }

    fun openMovementForm(product: ProductEntity) {
        _uiState.update { it.copy(isMovementFormOpen = true, selectedProductForMovement = product) }
    }

    fun closeMovementForm() {
        _uiState.update { it.copy(isMovementFormOpen = false, selectedProductForMovement = null) }
    }

    fun saveProduct(
        name: String,
        sku: String,
        barcode: String,
        category: ProductCategory,
        brand: String,
        model: String,
        description: String,
        supplier: String,
        costPrice: Double,
        salePrice: Double,
        stockQuantity: Int,
        minStock: Int,
        location: String
    ) {
        if (name.isBlank() || sku.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Preencha o nome e o código SKU") }
            return
        }

        viewModelScope.launch {
            val product = (_uiState.value.editingProduct ?: ProductEntity(
                name = name,
                sku = sku,
                category = category,
                costPrice = costPrice,
                salePrice = salePrice,
                stockQuantity = stockQuantity
            )).copy(
                name = name,
                sku = sku,
                barcode = barcode,
                category = category,
                brand = brand,
                compatibleModel = model,
                description = description,
                supplier = supplier,
                costPrice = costPrice,
                salePrice = salePrice,
                stockQuantity = stockQuantity,
                minStock = minStock,
                location = location
            )

            repository.saveProduct(product, currentUser)
            _uiState.update { it.copy(isProductFormOpen = false, editingProduct = null) }
        }
    }

    fun registerStockMovement(
        productId: Long,
        quantityChange: Int,
        type: StockMovementType,
        reason: String
    ) {
        if (quantityChange == 0) return

        viewModelScope.launch {
            val result = repository.adjustStock(productId, quantityChange, type, reason, currentUser)
            result.onSuccess {
                _uiState.update { it.copy(isMovementFormOpen = false, selectedProductForMovement = null) }
            }.onFailure { ex ->
                _uiState.update { it.copy(errorMessage = ex.message ?: "Falha ao movimentar estoque") }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
