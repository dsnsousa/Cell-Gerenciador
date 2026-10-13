package com.example.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.EcellRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Calendar

data class SellerRankingItem(
    val sellerId: Long,
    val sellerName: String,
    val salesCount: Int,
    val totalRevenue: Double,
    val totalCommission: Double
)

data class DailyRevenueItem(
    val dayLabel: String,
    val revenue: Double
)

data class DashboardUiState(
    val todayRevenue: Double = 0.0,
    val monthRevenue: Double = 0.0,
    val totalSalesCount: Int = 0,
    val totalItemsSold: Int = 0,
    val estimatedGrossProfit: Double = 0.0,
    val totalExpensesMonth: Double = 0.0,
    val estimatedNetProfit: Double = 0.0,
    val totalStockCostValue: Double = 0.0,
    val lowStockCount: Int = 0,
    val activeServiceOrdersCount: Int = 0,
    val recentSales: List<SaleEntity> = emptyList(),
    val lowStockProducts: List<ProductEntity> = emptyList(),
    val sellerRanking: List<SellerRankingItem> = emptyList(),
    val chartData: List<DailyRevenueItem> = emptyList(),
    val userRole: UserRole = UserRole.ADMIN,
    val myPersonalSalesCount: Int = 0,
    val myPersonalRevenue: Double = 0.0,
    val myPersonalCommission: Double = 0.0,
    val myActiveServiceOrdersCount: Int = 0
)

class DashboardViewModel(
    private val repository: EcellRepository,
    private val currentUser: UserEntity
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState(userRole = currentUser.role))
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                repository.getAllSales(),
                repository.getAllProducts(),
                repository.getAllServiceOrders(),
                repository.getAllExpenses()
            ) { sales, products, orders, expenses ->
                computeDashboard(sales, products, orders, expenses)
            }.collect { calculatedState ->
                _uiState.value = calculatedState
            }
        }
    }

    private fun computeDashboard(
        sales: List<SaleEntity>,
        products: List<ProductEntity>,
        orders: List<ServiceOrderEntity>,
        expenses: List<ExpenseEntity>
    ): DashboardUiState {
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

        val confirmedSales = sales.filter { it.status == SaleStatus.CONFIRMADA }

        val todayRevenue = confirmedSales
            .filter { it.dateEpoch >= startOfToday }
            .sumOf { it.total }

        val monthSales = confirmedSales.filter { it.dateEpoch >= startOfMonth }
        val monthRevenue = monthSales.sumOf { it.total }
        val monthCmv = monthSales.sumOf { it.totalCost }

        val estimatedGrossProfit = monthRevenue - monthCmv

        val monthPaidExpenses = expenses
            .filter { it.status == ExpenseStatus.PAGO && (it.paidDateEpoch ?: it.dueDateEpoch) >= startOfMonth }
            .sumOf { it.amount }

        val estimatedNetProfit = estimatedGrossProfit - monthPaidExpenses

        val totalStockCostValue = products.filter { it.isActive }.sumOf { it.costPrice * it.stockQuantity }
        val lowStockProducts = products.filter { it.isActive && it.stockQuantity <= it.minStock }

        val activeOrders = orders.filter {
            it.status != ServiceOrderStatus.ENTREGUE && it.status != ServiceOrderStatus.CANCELADO
        }

        // Seller Ranking
        val sellerRanking = confirmedSales
            .groupBy { it.sellerId }
            .map { (sellerId, sList) ->
                val sellerName = sList.firstOrNull()?.sellerName ?: "Vendedor"
                val rev = sList.sumOf { it.total }
                val comm = rev * (currentUser.commissionRate / 100.0)
                SellerRankingItem(
                    sellerId = sellerId,
                    sellerName = sellerName,
                    salesCount = sList.size,
                    totalRevenue = rev,
                    totalCommission = comm
                )
            }.sortedByDescending { it.totalRevenue }

        // Chart Data (Last 7 days)
        val chartData = mutableListOf<DailyRevenueItem>()
        for (i in 6 downTo 0) {
            val dayCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
            }
            val dayStart = Calendar.getInstance().apply {
                timeInMillis = dayCal.timeInMillis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val dayEnd = dayStart + 86400000L - 1

            val dayRev = confirmedSales
                .filter { it.dateEpoch in dayStart..dayEnd }
                .sumOf { it.total }

            val label = String.format("%02d/%02d", dayCal.get(Calendar.DAY_OF_MONTH), dayCal.get(Calendar.MONTH) + 1)
            chartData.add(DailyRevenueItem(dayLabel = label, revenue = dayRev))
        }

        // Personal metrics for Vendedor / Técnico
        val mySales = confirmedSales.filter { it.sellerId == currentUser.id }
        val myRev = mySales.sumOf { it.total }
        val myComm = myRev * (currentUser.commissionRate / 100.0)

        val myActiveOrders = activeOrders.filter { it.technicianId == currentUser.id }

        return DashboardUiState(
            todayRevenue = todayRevenue,
            monthRevenue = monthRevenue,
            totalSalesCount = confirmedSales.size,
            totalItemsSold = confirmedSales.size * 2, // approximate items
            estimatedGrossProfit = estimatedGrossProfit,
            totalExpensesMonth = monthPaidExpenses,
            estimatedNetProfit = estimatedNetProfit,
            totalStockCostValue = totalStockCostValue,
            lowStockCount = lowStockProducts.size,
            activeServiceOrdersCount = activeOrders.size,
            recentSales = confirmedSales.take(5),
            lowStockProducts = lowStockProducts,
            sellerRanking = sellerRanking,
            chartData = chartData,
            userRole = currentUser.role,
            myPersonalSalesCount = mySales.size,
            myPersonalRevenue = myRev,
            myPersonalCommission = myComm,
            myActiveServiceOrdersCount = myActiveOrders.size
        )
    }
}
