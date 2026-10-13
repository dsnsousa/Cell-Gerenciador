package com.example.ui.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.EcellRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PosUiState(
    val products: List<ProductEntity> = emptyList(),
    val filteredProducts: List<ProductEntity> = emptyList(),
    val customers: List<CustomerEntity> = emptyList(),
    val salesHistory: List<SaleEntity> = emptyList(),
    val selectedCategory: ProductCategory? = null,
    val searchQuery: String = "",
    val cart: List<EcellRepository.CartItem> = emptyList(),
    val selectedCustomer: CustomerEntity? = null,
    val discount: Double = 0.0,
    val paymentMethod: PaymentMethod = PaymentMethod.PIX,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val lastCompletedSale: SaleEntity? = null,
    val showReceiptDialog: Boolean = false,
    val showCancelDialog: Boolean = false,
    val saleToCancel: SaleEntity? = null
)

class PosViewModel(
    private val repository: EcellRepository,
    val currentUser: UserEntity
) : ViewModel() {

    private val _uiState = MutableStateFlow(PosUiState())
    val uiState: StateFlow<PosUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                repository.getAllProducts(),
                repository.getAllCustomers(),
                repository.getAllSales()
            ) { products, customers, sales ->
                Triple(products.filter { it.isActive }, customers, sales)
            }.collect { (products, customers, sales) ->
                _uiState.update { state ->
                    state.copy(
                        products = products,
                        filteredProducts = filterProducts(products, state.searchQuery, state.selectedCategory),
                        customers = customers,
                        salesHistory = sales
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredProducts = filterProducts(state.products, query, state.selectedCategory)
            )
        }
    }

    fun onCategorySelected(category: ProductCategory?) {
        _uiState.update { state ->
            state.copy(
                selectedCategory = category,
                filteredProducts = filterProducts(state.products, state.searchQuery, category)
            )
        }
    }

    private fun filterProducts(
        products: List<ProductEntity>,
        query: String,
        category: ProductCategory?
    ): List<ProductEntity> {
        return products.filter { p ->
            val matchesCategory = category == null || p.category == category
            val matchesQuery = query.isBlank() ||
                    p.name.contains(query, ignoreCase = true) ||
                    p.sku.contains(query, ignoreCase = true) ||
                    p.barcode.contains(query, ignoreCase = true) ||
                    p.compatibleModel.contains(query, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    fun addToCart(product: ProductEntity) {
        _uiState.update { state ->
            val existing = state.cart.find { it.product.id == product.id }
            val currentQty = existing?.quantity ?: 0
            if (currentQty + 1 > product.stockQuantity) {
                return@update state.copy(errorMessage = "Estoque insuficiente para ${product.name} (Disponível: ${product.stockQuantity})")
            }

            val newCart = if (existing != null) {
                state.cart.map {
                    if (it.product.id == product.id) it.copy(quantity = it.quantity + 1) else it
                }
            } else {
                state.cart + EcellRepository.CartItem(product, 1)
            }
            state.copy(cart = newCart, errorMessage = null)
        }
    }

    fun updateCartQuantity(productId: Long, delta: Int) {
        _uiState.update { state ->
            val item = state.cart.find { it.product.id == productId } ?: return@update state
            val newQty = item.quantity + delta
            if (newQty > item.product.stockQuantity) {
                return@update state.copy(errorMessage = "Estoque insuficiente para ${item.product.name} (Disponível: ${item.product.stockQuantity})")
            }

            val newCart = if (newQty <= 0) {
                state.cart.filterNot { it.product.id == productId }
            } else {
                state.cart.map {
                    if (it.product.id == productId) it.copy(quantity = newQty) else it
                }
            }
            state.copy(cart = newCart, errorMessage = null)
        }
    }

    fun removeFromCart(productId: Long) {
        _uiState.update { state ->
            state.copy(cart = state.cart.filterNot { it.product.id == productId })
        }
    }

    fun clearCart() {
        _uiState.update { state ->
            state.copy(
                cart = emptyList(),
                selectedCustomer = null,
                discount = 0.0,
                errorMessage = null
            )
        }
    }

    fun selectCustomer(customer: CustomerEntity?) {
        _uiState.update { it.copy(selectedCustomer = customer) }
    }

    fun setDiscount(discount: Double) {
        _uiState.update { it.copy(discount = discount.coerceAtLeast(0.0)) }
    }

    fun setPaymentMethod(method: PaymentMethod) {
        _uiState.update { it.copy(paymentMethod = method) }
    }

    fun finalizeSale() {
        val state = _uiState.value
        if (state.cart.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Adicione produtos ao carrinho") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.confirmSale(
                customer = state.selectedCustomer,
                seller = currentUser,
                items = state.cart,
                discount = state.discount,
                paymentMethod = state.paymentMethod
            )

            result.onSuccess { completedSale ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        cart = emptyList(),
                        selectedCustomer = null,
                        discount = 0.0,
                        lastCompletedSale = completedSale,
                        showReceiptDialog = true,
                        errorMessage = null
                    )
                }
            }.onFailure { ex ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = ex.message ?: "Erro ao confirmar venda"
                    )
                }
            }
        }
    }

    fun closeReceiptDialog() {
        _uiState.update { it.copy(showReceiptDialog = false, lastCompletedSale = null) }
    }

    fun openCancelDialog(sale: SaleEntity) {
        _uiState.update { it.copy(showCancelDialog = true, saleToCancel = sale) }
    }

    fun closeCancelDialog() {
        _uiState.update { it.copy(showCancelDialog = false, saleToCancel = null) }
    }

    fun cancelSale(reason: String) {
        val sale = _uiState.value.saleToCancel ?: return
        if (reason.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Informe o motivo do cancelamento") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = repository.cancelSale(sale.id, reason, currentUser)
            result.onSuccess {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        showCancelDialog = false,
                        saleToCancel = null,
                        errorMessage = null
                    )
                }
            }.onFailure { ex ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = ex.message ?: "Falha ao estornar venda"
                    )
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
