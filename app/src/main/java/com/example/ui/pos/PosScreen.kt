package com.example.ui.pos

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
    viewModel: PosViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0 = PDV, 1 = Histórico
    var showCustomerPicker by remember { mutableStateOf(false) }
    var cancelReason by remember { mutableStateOf("") }

    val subtotal = state.cart.sumOf { it.product.salePrice * it.quantity }
    val total = (subtotal - state.discount).coerceAtLeast(0.0)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("PDV / Caixa", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.PointOfSale, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Histórico", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (selectedTab == 0) {
                // PDV TAB
                Column(modifier = Modifier.fillMaxSize()) {
                    // Search & Customer header
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        // Customer selection bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { showCustomerPicker = true }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = state.selectedCustomer?.let { "Cliente: ${it.name}" } ?: "Consumidor Final (Balcão)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = "Alterar",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Search Field
                        OutlinedTextField(
                            value = state.searchQuery,
                            onValueChange = viewModel::onSearchQueryChanged,
                            placeholder = { Text("Buscar peça, acessório ou SKU...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            trailingIcon = {
                                if (state.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Limpar")
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("product_search_input")
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Category Chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            item {
                                FilterChip(
                                    selected = state.selectedCategory == null,
                                    onClick = { viewModel.onCategorySelected(null) },
                                    label = { Text("Todos") }
                                )
                            }
                            items(ProductCategory.values()) { cat ->
                                FilterChip(
                                    selected = state.selectedCategory == cat,
                                    onClick = { viewModel.onCategorySelected(cat) },
                                    label = { Text(cat.label) }
                                )
                            }
                        }
                    }

                    // Product List
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(state.filteredProducts) { product ->
                            ProductPosCard(
                                product = product,
                                onAdd = { viewModel.addToCart(product) }
                            )
                        }
                        if (state.filteredProducts.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Nenhum produto encontrado.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }

                    // Bottom Cart Sheet Card
                    Card(
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Carrinho (${state.cart.sumOf { it.quantity }} itens)",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                if (state.cart.isNotEmpty()) {
                                    TextButton(onClick = viewModel::clearCart) {
                                        Text("Esvaziar", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }

                            if (state.cart.isNotEmpty()) {
                                // Cart Items preview
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 140.dp)
                                        .verticalScroll(rememberScrollState()),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    state.cart.forEach { item ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = item.product.name,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    text = "${Formatters.formatCurrency(item.product.salePrice)} un.",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = { viewModel.updateCartQuantity(item.product.id, -1) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Remove, contentDescription = "Diminuir", modifier = Modifier.size(16.dp))
                                                }
                                                Text(
                                                    text = "${item.quantity}",
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp)
                                                )
                                                IconButton(
                                                    onClick = { viewModel.updateCartQuantity(item.product.id, 1) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(Icons.Default.Add, contentDescription = "Aumentar", modifier = Modifier.size(16.dp))
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = Formatters.formatCurrency(item.product.salePrice * item.quantity),
                                                    fontWeight = FontWeight.Bold,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                        }
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                // Payment Method Picker
                                Text(
                                    text = "Forma de Pagamento:",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                ) {
                                    items(PaymentMethod.values()) { method ->
                                        FilterChip(
                                            selected = state.paymentMethod == method,
                                            onClick = { viewModel.setPaymentMethod(method) },
                                            label = { Text(method.label, fontSize = 11.sp) }
                                        )
                                    }
                                }

                                // Total Row & Finalize Button
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "Total a Pagar",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = Formatters.formatCurrency(total),
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        )
                                    }

                                    Button(
                                        onClick = viewModel::finalizeSale,
                                        modifier = Modifier
                                            .height(48.dp)
                                            .testTag("confirm_sale_button"),
                                        shape = RoundedCornerShape(12.dp),
                                        enabled = !state.isLoading
                                    ) {
                                        if (state.isLoading) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(20.dp),
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                        } else {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("Finalizar Venda", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    text = "Toque em um produto acima para adicionar ao carrinho.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            }
                        }
                    }
                }
            } else {
                // SALES HISTORY TAB
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.salesHistory) { sale ->
                        SaleHistoryCard(
                            sale = sale,
                            isAdmin = viewModel.currentUser.role == UserRole.ADMIN,
                            onCancel = { viewModel.openCancelDialog(sale) },
                            onShareReceipt = {
                                val receiptText = generateReceiptText(sale)
                                shareViaWhatsApp(context, receiptText)
                            }
                        )
                    }
                    if (state.salesHistory.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("Nenhuma venda realizada ainda.")
                            }
                        }
                    }
                }
            }
        }
    }

    // Customer Picker Dialog
    if (showCustomerPicker) {
        AlertDialog(
            onDismissRequest = { showCustomerPicker = false },
            title = { Text("Selecionar Cliente") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 350.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    ListItem(
                        headlineContent = { Text("Consumidor Final (Sem cadastro)") },
                        modifier = Modifier.clickable {
                            viewModel.selectCustomer(null)
                            showCustomerPicker = false
                        }
                    )
                    HorizontalDivider()
                    state.customers.forEach { customer ->
                        ListItem(
                            headlineContent = { Text(customer.name) },
                            supportingContent = { Text(customer.phone) },
                            modifier = Modifier.clickable {
                                viewModel.selectCustomer(customer)
                                showCustomerPicker = false
                            }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCustomerPicker = false }) {
                    Text("Fechar")
                }
            }
        )
    }

    // Error Snackbar / Alert
    if (state.errorMessage != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            icon = { Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Atenção") },
            text = { Text(state.errorMessage ?: "") },
            confirmButton = {
                Button(onClick = viewModel::dismissError) {
                    Text("OK")
                }
            }
        )
    }

    // Sale Receipt Dialog
    if (state.showReceiptDialog && state.lastCompletedSale != null) {
        val sale = state.lastCompletedSale!!
        val receiptText = generateReceiptText(sale)

        AlertDialog(
            onDismissRequest = viewModel::closeReceiptDialog,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Venda Confirmada!")
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "Comprovante de Venda:",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = receiptText,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        shareViaWhatsApp(context, receiptText)
                        viewModel.closeReceiptDialog()
                    }
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("WhatsApp")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::closeReceiptDialog) {
                    Text("Concluir")
                }
            }
        )
    }

    // Cancel Sale Dialog (Admin only)
    if (state.showCancelDialog && state.saleToCancel != null) {
        AlertDialog(
            onDismissRequest = viewModel::closeCancelDialog,
            title = { Text("Cancelar e Estornar Venda") },
            text = {
                Column {
                    Text(
                        text = "Tem certeza que deseja cancelar a venda ${state.saleToCancel?.saleNumber}? As peças serão devolvidas automaticamente ao estoque.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = cancelReason,
                        onValueChange = { cancelReason = it },
                        label = { Text("Motivo do cancelamento (obrigatório)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.cancelSale(cancelReason)
                        cancelReason = ""
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Confirmar Estorno")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::closeCancelDialog) {
                    Text("Voltar")
                }
            }
        )
    }
}

@Composable
fun ProductPosCard(
    product: ProductEntity,
    onAdd: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${product.category.label} • SKU: ${product.sku}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = Formatters.formatCurrency(product.salePrice),
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    val stockColor = if (product.stockQuantity <= product.minStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                    Text(
                        text = "Estoque: ${product.stockQuantity}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = stockColor
                    )
                }
            }

            IconButton(
                onClick = onAdd,
                enabled = product.stockQuantity > 0,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (product.stockQuantity > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Icon(
                    Icons.Default.AddShoppingCart,
                    contentDescription = "Adicionar",
                    tint = if (product.stockQuantity > 0) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun SaleHistoryCard(
    sale: SaleEntity,
    isAdmin: Boolean,
    onCancel: () -> Unit,
    onShareReceipt: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${sale.saleNumber} • ${sale.customerName}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                val statusColor = if (sale.status == SaleStatus.CONFIRMADA) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                SuggestionChip(
                    onClick = {},
                    label = { Text(sale.status.label, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = statusColor.copy(alpha = 0.15f),
                        labelColor = statusColor
                    )
                )
            }

            Text(
                text = "${Formatters.formatDateTime(sale.dateEpoch)} • Vendedor: ${sale.sellerName}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Pagamento: ${sale.paymentMethod.label}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total: ${Formatters.formatCurrency(sale.total)}",
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.titleMedium
                )

                Row {
                    IconButton(onClick = onShareReceipt) {
                        Icon(Icons.Default.Share, contentDescription = "Compartilhar comprovante")
                    }
                    if (isAdmin && sale.status == SaleStatus.CONFIRMADA) {
                        IconButton(onClick = onCancel) {
                            Icon(Icons.Default.Cancel, contentDescription = "Cancelar venda", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            if (sale.status == SaleStatus.CANCELADA) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Cancelada por ${sale.cancelledBy}: ${sale.cancellationReason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

fun generateReceiptText(sale: SaleEntity): String {
    return """
========================================
           ECELL GESTÃO
  Assistência Técnica de Celulares
========================================
Venda: ${sale.saleNumber}
Data: ${Formatters.formatDateTime(sale.dateEpoch)}
Cliente: ${sale.customerName}
Vendedor: ${sale.sellerName}
----------------------------------------
Subtotal: ${Formatters.formatCurrency(sale.subtotal)}
Desconto: ${Formatters.formatCurrency(sale.discount)}
TOTAL: ${Formatters.formatCurrency(sale.total)}
Pagamento: ${sale.paymentMethod.label}
Valor Pago: ${Formatters.formatCurrency(sale.amountPaid)}
----------------------------------------
Obrigado pela preferência!
Garantia de 90 dias com este cupom.
========================================
    """.trimIndent()
}

fun shareViaWhatsApp(context: android.content.Context, message: String) {
    try {
        val sendIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_TEXT, message)
            type = "text/plain"
        }
        val shareIntent = Intent.createChooser(sendIntent, "Compartilhar Comprovante")
        context.startActivity(shareIntent)
    } catch (_: Exception) {
        // Fallback
    }
}
