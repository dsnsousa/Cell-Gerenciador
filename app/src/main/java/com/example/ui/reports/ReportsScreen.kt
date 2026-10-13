package com.example.ui.reports

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.repository.EcellRepository
import com.example.util.Formatters
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun ReportsScreen(
    repository: EcellRepository,
    currentUser: UserEntity,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var sales by remember { mutableStateOf<List<SaleEntity>>(emptyList()) }
    var products by remember { mutableStateOf<List<ProductEntity>>(emptyList()) }
    var users by remember { mutableStateOf<List<UserEntity>>(emptyList()) }
    var orders by remember { mutableStateOf<List<ServiceOrderEntity>>(emptyList()) }

    LaunchedEffect(Unit) {
        combine(
            repository.getAllSales(),
            repository.getAllProducts(),
            repository.getAllUsers(),
            repository.getAllServiceOrders()
        ) { s, p, u, o ->
            sales = s
            products = p
            users = u
            orders = o
        }.collect {}
    }

    val confirmedSales = sales.filter { it.status == SaleStatus.CONFIRMADA }
    val totalRevenue = confirmedSales.sumOf { it.total }
    val totalCost = confirmedSales.sumOf { it.totalCost }
    val grossProfit = totalRevenue - totalCost
    val totalStockInvested = products.sumOf { it.costPrice * it.stockQuantity }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Export CSV Action Card
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Exportar Relatórios Gerenciais",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Gere uma planilha CSV com vendas, comissões e estoque.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }

                    Button(
                        onClick = {
                            val csv = generateCsvReport(confirmedSales, products, users)
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/csv"
                                putExtra(Intent.EXTRA_SUBJECT, "Relatorio_ECELL_GESTAO.csv")
                                putExtra(Intent.EXTRA_TEXT, csv)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Exportar Relatório CSV"))
                        },
                        modifier = Modifier.testTag("export_csv_button")
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exportar CSV")
                    }
                }
            }
        }

        // Summary Cards
        item {
            Text(
                text = "Resumo Consolidado",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricMiniCard(
                    label = "Vendas Realizadas",
                    value = "${confirmedSales.size}",
                    modifier = Modifier.weight(1f)
                )
                MetricMiniCard(
                    label = "Estoque a Custo",
                    value = Formatters.formatCurrency(totalStockInvested),
                    modifier = Modifier.weight(1f)
                )
                MetricMiniCard(
                    label = "Lucro Bruto CMV",
                    value = Formatters.formatCurrency(grossProfit),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Seller Commissions Breakdown
        item {
            Text(
                text = "Comissões e Desempenho por Vendedor",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    users.filter { it.role == UserRole.VENDEDOR || it.role == UserRole.TECNICO }.forEach { user ->
                        val userSales = confirmedSales.filter { it.sellerId == user.id }
                        val userTotal = userSales.sumOf { it.total }
                        val commission = userTotal * (user.commissionRate / 100.0)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(user.fullName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "${userSales.size} vendas • Taxa: ${user.commissionRate}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = Formatters.formatCurrency(commission),
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = "Total: ${Formatters.formatCurrency(userTotal)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }

        // Top Selling Products / Ranking
        item {
            Text(
                text = "Catálogo com Maior Margem de Lucro",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
        }

        items(products.sortedByDescending { it.salePrice - it.costPrice }.take(5)) { product ->
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(product.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(
                            "Venda: ${Formatters.formatCurrency(product.salePrice)} | Custo: ${Formatters.formatCurrency(product.costPrice)}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val profit = product.salePrice - product.costPrice
                    val margin = Formatters.calculateMargin(product.costPrice, product.salePrice)
                    Text(
                        text = "+${Formatters.formatCurrency(profit)} (${"%.0f".format(margin)}%)",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MetricMiniCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}

fun generateCsvReport(sales: List<SaleEntity>, products: List<ProductEntity>, users: List<UserEntity>): String {
    val sb = StringBuilder()
    sb.append("RELATORIO GERENCIAL - ECELL GESTAO\n\n")

    sb.append("--- VENDAS CONFIRMADAS ---\n")
    sb.append("Numero,Data,Cliente,Vendedor,Forma Pagamento,Total,Custo,Lucro Bruto\n")
    sales.forEach { s ->
        val profit = s.total - s.totalCost
        sb.append("${s.saleNumber},\"${Formatters.formatDateTime(s.dateEpoch)}\",\"${s.customerName}\",\"${s.sellerName}\",${s.paymentMethod.label},${s.total},${s.totalCost},$profit\n")
    }

    sb.append("\n--- INVENTARIO ATUAL ---\n")
    sb.append("SKU,Produto,Categoria,Estoque,Custo Unitario,Preco Venda,Valor Total Custo\n")
    products.forEach { p ->
        sb.append("${p.sku},\"${p.name}\",${p.category.label},${p.stockQuantity},${p.costPrice},${p.salePrice},${p.costPrice * p.stockQuantity}\n")
    }

    return sb.toString()
}
