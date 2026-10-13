package com.example.ui.financial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.EcellRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

data class FinancialUiState(
    val periodRevenue: Double = 0.0,
    val periodReceived: Double = 0.0,
    val periodPending: Double = 0.0,
    val periodDiscounts: Double = 0.0,
    val periodCancelledAmount: Double = 0.0,
    val periodPartsRevenue: Double = 0.0,
    val periodServicesRevenue: Double = 0.0,
    val periodCmv: Double = 0.0,
    val periodGrossProfit: Double = 0.0,
    val periodExpensesPaid: Double = 0.0,
    val periodNetEstimatedResult: Double = 0.0,
    val expenses: List<ExpenseEntity> = emptyList(),
    val filteredExpenses: List<ExpenseEntity> = emptyList(),
    val suppliers: List<SupplierEntity> = emptyList(),
    val purchases: List<PurchaseEntity> = emptyList(),
    val paymentMethodBreakdown: Map<PaymentMethod, Double> = emptyMap(),
    val selectedPeriod: Int = 1, // 0 = Hoje, 1 = Mês Atual, 2 = Todos
    val selectedExpenseFilter: ExpenseStatus? = null,
    val isExpenseFormOpen: Boolean = false,
    val isSupplierFormOpen: Boolean = false
)

class FinancialViewModel(
    private val repository: EcellRepository,
    val currentUser: UserEntity
) : ViewModel() {

    private val _uiState = MutableStateFlow(FinancialUiState())
    val uiState: StateFlow<FinancialUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                repository.getAllSales(),
                repository.getAllServiceOrders(),
                repository.getAllExpenses(),
                repository.getAllSuppliers(),
                repository.getAllPurchases()
            ) { sales, orders, expenses, suppliers, purchases ->
                calculateFinancials(sales, orders, expenses, suppliers, purchases, _uiState.value.selectedPeriod, _uiState.value.selectedExpenseFilter)
            }.collect { calculatedState ->
                _uiState.value = calculatedState
            }
        }
    }

    fun setPeriod(period: Int) {
        _uiState.update { it.copy(selectedPeriod = period) }
        loadData()
    }

    fun setExpenseFilter(status: ExpenseStatus?) {
        _uiState.update { state ->
            state.copy(
                selectedExpenseFilter = status,
                filteredExpenses = if (status == null) state.expenses else state.expenses.filter { it.status == status }
            )
        }
    }

    private fun calculateFinancials(
        sales: List<SaleEntity>,
        orders: List<ServiceOrderEntity>,
        expenses: List<ExpenseEntity>,
        suppliers: List<SupplierEntity>,
        purchases: List<PurchaseEntity>,
        period: Int,
        expenseFilter: ExpenseStatus?
    ): FinancialUiState {
        val now = Calendar.getInstance()
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val startOfMonth = Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val filterStartTime = when (period) {
            0 -> startOfToday
            1 -> startOfMonth
            else -> 0L
        }

        val filteredSales = sales.filter { it.dateEpoch >= filterStartTime }
        val confirmedSales = filteredSales.filter { it.status == SaleStatus.CONFIRMADA }
        val cancelledSales = filteredSales.filter { it.status == SaleStatus.CANCELADA }

        val revenue = confirmedSales.sumOf { it.total }
        val received = confirmedSales.sumOf { it.amountPaid }
        val pending = confirmedSales.sumOf { it.pendingBalance }
        val discounts = confirmedSales.sumOf { it.discount }
        val cancelledAmount = cancelledSales.sumOf { it.total }

        // Service orders finished/revenue in period
        val finishedOrders = orders.filter {
            (it.completedDateEpoch ?: it.entryDateEpoch) >= filterStartTime &&
                    (it.status == ServiceOrderStatus.PRONTO || it.status == ServiceOrderStatus.ENTREGUE)
        }
        val servicesRevenue = finishedOrders.sumOf { it.laborPrice }
        val partsRevenue = revenue + finishedOrders.sumOf { it.partsPrice }

        // CMV: cost of goods sold (without stock purchase duplicate)
        val cmv = confirmedSales.sumOf { it.totalCost }
        val grossProfit = (revenue + servicesRevenue) - cmv

        // Operating Expenses
        val periodExpenses = expenses.filter {
            (it.paidDateEpoch ?: it.dueDateEpoch) >= filterStartTime
        }
        val expensesPaid = periodExpenses
            .filter { it.status == ExpenseStatus.PAGO }
            .sumOf { it.amount }

        val netEstimated = grossProfit - expensesPaid

        val paymentBreakdown = confirmedSales
            .groupBy { it.paymentMethod }
            .mapValues { entry -> entry.value.sumOf { it.total } }

        return FinancialUiState(
            periodRevenue = revenue + servicesRevenue,
            periodReceived = received,
            periodPending = pending,
            periodDiscounts = discounts,
            periodCancelledAmount = cancelledAmount,
            periodPartsRevenue = partsRevenue,
            periodServicesRevenue = servicesRevenue,
            periodCmv = cmv,
            periodGrossProfit = grossProfit,
            periodExpensesPaid = expensesPaid,
            periodNetEstimatedResult = netEstimated,
            expenses = expenses,
            filteredExpenses = if (expenseFilter == null) expenses else expenses.filter { it.status == expenseFilter },
            suppliers = suppliers,
            purchases = purchases,
            paymentMethodBreakdown = paymentBreakdown,
            selectedPeriod = period,
            selectedExpenseFilter = expenseFilter
        )
    }

    fun openExpenseForm() {
        _uiState.update { it.copy(isExpenseFormOpen = true) }
    }

    fun closeExpenseForm() {
        _uiState.update { it.copy(isExpenseFormOpen = false) }
    }

    fun openSupplierForm() {
        _uiState.update { it.copy(isSupplierFormOpen = true) }
    }

    fun closeSupplierForm() {
        _uiState.update { it.copy(isSupplierFormOpen = false) }
    }

    fun saveExpense(
        description: String,
        category: ExpenseCategory,
        amount: Double,
        dueDate: Long,
        isPaid: Boolean,
        notes: String
    ) {
        if (description.isBlank() || amount <= 0.0) return

        viewModelScope.launch {
            val expense = ExpenseEntity(
                description = description,
                category = category,
                amount = amount,
                dueDateEpoch = dueDate,
                paidDateEpoch = if (isPaid) System.currentTimeMillis() else null,
                status = if (isPaid) ExpenseStatus.PAGO else ExpenseStatus.PENDENTE,
                notes = notes
            )
            repository.saveExpense(expense, currentUser)
            _uiState.update { it.copy(isExpenseFormOpen = false) }
        }
    }

    fun markExpensePaid(expense: ExpenseEntity) {
        viewModelScope.launch {
            val updated = expense.copy(
                status = ExpenseStatus.PAGO,
                paidDateEpoch = System.currentTimeMillis()
            )
            repository.saveExpense(updated, currentUser)
        }
    }

    fun saveSupplier(name: String, company: String, phone: String, email: String, notes: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.saveSupplier(
                SupplierEntity(
                    name = name,
                    companyName = company,
                    phone = phone,
                    email = email,
                    notes = notes
                )
            )
            _uiState.update { it.copy(isSupplierFormOpen = false) }
        }
    }
}
