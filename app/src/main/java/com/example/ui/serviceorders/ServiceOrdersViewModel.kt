package com.example.ui.serviceorders

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.EcellRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ServiceOrdersUiState(
    val orders: List<ServiceOrderEntity> = emptyList(),
    val filteredOrders: List<ServiceOrderEntity> = emptyList(),
    val customers: List<CustomerEntity> = emptyList(),
    val products: List<ProductEntity> = emptyList(),
    val selectedStatusFilter: ServiceOrderStatus? = null,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isFormOpen: Boolean = false,
    val editingOrder: ServiceOrderEntity? = null
)

class ServiceOrdersViewModel(
    private val repository: EcellRepository,
    val currentUser: UserEntity
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServiceOrdersUiState())
    val uiState: StateFlow<ServiceOrdersUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                repository.getAllServiceOrders(),
                repository.getAllCustomers(),
                repository.getAllProducts()
            ) { orders, customers, products ->
                Triple(orders, customers, products)
            }.collect { (orders, customers, products) ->
                _uiState.update { state ->
                    state.copy(
                        orders = orders,
                        filteredOrders = filterOrders(orders, state.searchQuery, state.selectedStatusFilter),
                        customers = customers,
                        products = products
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredOrders = filterOrders(state.orders, query, state.selectedStatusFilter)
            )
        }
    }

    fun onStatusFilterChanged(status: ServiceOrderStatus?) {
        _uiState.update { state ->
            state.copy(
                selectedStatusFilter = status,
                filteredOrders = filterOrders(state.orders, state.searchQuery, status)
            )
        }
    }

    private fun filterOrders(
        orders: List<ServiceOrderEntity>,
        query: String,
        status: ServiceOrderStatus?
    ): List<ServiceOrderEntity> {
        return orders.filter { order ->
            val matchesStatus = status == null || order.status == status
            val matchesQuery = query.isBlank() ||
                    order.orderNumber.contains(query, ignoreCase = true) ||
                    order.customerName.contains(query, ignoreCase = true) ||
                    order.phoneModel.contains(query, ignoreCase = true) ||
                    order.phoneBrand.contains(query, ignoreCase = true) ||
                    order.imei.contains(query, ignoreCase = true)
            matchesStatus && matchesQuery
        }
    }

    fun updateOrderStatus(orderId: Long, newStatus: ServiceOrderStatus) {
        viewModelScope.launch {
            repository.updateServiceOrderStatus(orderId, newStatus, currentUser)
        }
    }

    fun openNewOrderForm() {
        _uiState.update { it.copy(isFormOpen = true, editingOrder = null) }
    }

    fun openEditOrderForm(order: ServiceOrderEntity) {
        _uiState.update { it.copy(isFormOpen = true, editingOrder = order) }
    }

    fun closeForm() {
        _uiState.update { it.copy(isFormOpen = false, editingOrder = null) }
    }

    fun saveServiceOrder(
        customerName: String,
        customerPhone: String,
        phoneBrand: String,
        phoneModel: String,
        imei: String,
        reportedDefect: String,
        diagnosis: String,
        laborPrice: Double,
        parts: List<EcellRepository.CartItem>,
        notes: String
    ) {
        if (customerName.isBlank() || phoneModel.isBlank() || reportedDefect.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Preencha cliente, modelo do celular e defeito") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val partsPrice = parts.sumOf { it.product.salePrice * it.quantity }
            val totalPrice = laborPrice + partsPrice

            val orderNumber = _uiState.value.editingOrder?.orderNumber ?: "OS-${System.currentTimeMillis() % 100000}"

            val order = (_uiState.value.editingOrder ?: ServiceOrderEntity(
                orderNumber = orderNumber,
                customerName = customerName,
                customerPhone = customerPhone,
                phoneBrand = phoneBrand,
                phoneModel = phoneModel,
                reportedDefect = reportedDefect,
                technicianId = currentUser.id,
                technicianName = currentUser.fullName
            )).copy(
                customerName = customerName,
                customerPhone = customerPhone,
                phoneBrand = phoneBrand,
                phoneModel = phoneModel,
                imei = imei,
                reportedDefect = reportedDefect,
                technicalDiagnosis = diagnosis,
                laborPrice = laborPrice,
                partsPrice = partsPrice,
                totalPrice = totalPrice,
                notes = notes
            )

            try {
                repository.saveServiceOrder(order, parts, currentUser)
                _uiState.update { it.copy(isLoading = false, isFormOpen = false, editingOrder = null) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Erro ao salvar OS") }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
