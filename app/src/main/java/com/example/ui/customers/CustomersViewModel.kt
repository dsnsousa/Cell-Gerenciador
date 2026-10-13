package com.example.ui.customers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CustomerEntity
import com.example.data.model.SaleEntity
import com.example.data.model.ServiceOrderEntity
import com.example.data.repository.EcellRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CustomersUiState(
    val customers: List<CustomerEntity> = emptyList(),
    val filteredCustomers: List<CustomerEntity> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isFormOpen: Boolean = false,
    val editingCustomer: CustomerEntity? = null,
    val selectedCustomerHistory: CustomerEntity? = null,
    val customerSales: List<SaleEntity> = emptyList(),
    val customerOrders: List<ServiceOrderEntity> = emptyList()
)

class CustomersViewModel(private val repository: EcellRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(CustomersUiState())
    val uiState: StateFlow<CustomersUiState> = _uiState.asStateFlow()

    init {
        loadCustomers()
    }

    private fun loadCustomers() {
        viewModelScope.launch {
            repository.getAllCustomers().collect { list ->
                _uiState.update { state ->
                    state.copy(
                        customers = list,
                        filteredCustomers = filterCustomers(list, state.searchQuery)
                    )
                }
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { state ->
            state.copy(
                searchQuery = query,
                filteredCustomers = filterCustomers(state.customers, query)
            )
        }
    }

    private fun filterCustomers(list: List<CustomerEntity>, query: String): List<CustomerEntity> {
        if (query.isBlank()) return list
        return list.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.phone.contains(query, ignoreCase = true) ||
                    it.cpf.contains(query, ignoreCase = true) ||
                    it.email.contains(query, ignoreCase = true)
        }
    }

    fun openNewCustomerForm() {
        _uiState.update { it.copy(isFormOpen = true, editingCustomer = null) }
    }

    fun openEditCustomerForm(customer: CustomerEntity) {
        _uiState.update { it.copy(isFormOpen = true, editingCustomer = customer) }
    }

    fun closeForm() {
        _uiState.update { it.copy(isFormOpen = false, editingCustomer = null) }
    }

    fun saveCustomer(
        name: String,
        cpf: String,
        phone: String,
        whatsapp: String,
        email: String,
        address: String,
        notes: String
    ) {
        if (name.isBlank() || phone.isBlank()) return

        viewModelScope.launch {
            val customer = (_uiState.value.editingCustomer ?: CustomerEntity(name = name)).copy(
                name = name,
                cpf = cpf,
                phone = phone,
                whatsapp = if (whatsapp.isBlank()) phone else whatsapp,
                email = email,
                address = address,
                notes = notes
            )
            repository.saveCustomer(customer)
            _uiState.update { it.copy(isFormOpen = false, editingCustomer = null) }
        }
    }

    fun openCustomerHistory(customer: CustomerEntity) {
        viewModelScope.launch {
            combine(
                repository.getAllSales(),
                repository.getAllServiceOrders()
            ) { sales, orders ->
                Pair(
                    sales.filter { it.customerId == customer.id },
                    orders.filter { it.customerId == customer.id }
                )
            }.first().let { (sales, orders) ->
                _uiState.update {
                    it.copy(
                        selectedCustomerHistory = customer,
                        customerSales = sales,
                        customerOrders = orders
                    )
                }
            }
        }
    }

    fun closeCustomerHistory() {
        _uiState.update { it.copy(selectedCustomerHistory = null, customerSales = emptyList(), customerOrders = emptyList()) }
    }
}
