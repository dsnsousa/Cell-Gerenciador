package com.example.ui.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    viewModel: InventoryViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0 = Produtos, 1 = Movimentações
    val isAdmin = viewModel.currentUser.role == UserRole.ADMIN

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
                    text = { Text("Catálogo (${state.products.size})", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Inventory2, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Movimentações", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.SwapVert, contentDescription = null) }
                )
            }
        },
        floatingActionButton = {
            if (isAdmin && selectedTab == 0) {
                FloatingActionButton(
                    onClick = viewModel::openNewProductForm,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("new_product_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Cadastrar Produto")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (selectedTab == 0) {
                // PRODUCTS CATALOG TAB
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    OutlinedTextField(
                        value = state.searchQuery,
                        onValueChange = viewModel::onSearchQueryChanged,
                        placeholder = { Text("Buscar por nome, SKU, modelo compatível...") },
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
                            .testTag("inventory_search_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        item {
                            FilterChip(
                                selected = state.onlyLowStock,
                                onClick = { viewModel.toggleOnlyLowStock(!state.onlyLowStock) },
                                label = { Text("⚠️ Estoque Baixo") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            )
                        }
                        item {
                            FilterChip(
                                selected = state.selectedCategory == null && !state.onlyLowStock,
                                onClick = {
                                    viewModel.toggleOnlyLowStock(false)
                                    viewModel.onCategorySelected(null)
                                },
                                label = { Text("Todos") }
                            )
                        }
                        items(ProductCategory.values()) { cat ->
                            FilterChip(
                                selected = state.selectedCategory == cat,
                                onClick = {
                                    viewModel.toggleOnlyLowStock(false)
                                    viewModel.onCategorySelected(cat)
                                },
                                label = { Text(cat.label, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.filteredProducts) { product ->
                        ProductItemCard(
                            product = product,
                            isAdmin = isAdmin,
                            onEdit = { viewModel.openEditProductForm(product) },
                            onAdjustStock = { viewModel.openMovementForm(product) }
                        )
                    }

                    if (state.filteredProducts.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(40.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Nenhum produto cadastrado com os filtros atuais.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                    }
                }
            } else {
                // MOVEMENTS TAB
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.stockMovements) { mov ->
                        StockMovementCard(movement = mov)
                    }

                    if (state.stockMovements.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                Text("Nenhuma movimentação de estoque registrada.")
                            }
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Product Dialog
    if (state.isProductFormOpen) {
        ProductFormDialog(
            editingProduct = state.editingProduct,
            onDismiss = viewModel::closeProductForm,
            onSave = viewModel::saveProduct
        )
    }

    // Stock Movement Dialog
    if (state.isMovementFormOpen && state.selectedProductForMovement != null) {
        StockMovementDialog(
            product = state.selectedProductForMovement!!,
            isAdmin = isAdmin,
            onDismiss = viewModel::closeMovementForm,
            onConfirm = { qty, type, reason ->
                viewModel.registerStockMovement(state.selectedProductForMovement!!.id, qty, type, reason)
            }
        )
    }

    if (state.errorMessage != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text("Atenção") },
            text = { Text(state.errorMessage ?: "") },
            confirmButton = {
                Button(onClick = viewModel::dismissError) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun ProductItemCard(
    product: ProductEntity,
    isAdmin: Boolean,
    onEdit: () -> Unit,
    onAdjustStock: () -> Unit
) {
    val isLowStock = product.stockQuantity <= product.minStock

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
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "SKU: ${product.sku} • ${product.category.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (product.compatibleModel.isNotBlank()) {
                        Text(
                            text = "Compatível: ${product.compatibleModel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Stock Badge
                val stockBg = if (isLowStock) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer
                val stockText = if (isLowStock) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(stockBg)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${product.stockQuantity} un.",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = stockText
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Pricing & Margins row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Preço de Venda", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(Formatters.formatCurrency(product.salePrice), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                }

                // If admin, show acquisition cost, estimated unit profit and gross margin %
                if (isAdmin) {
                    val unitProfit = Formatters.calculateUnitProfit(product.costPrice, product.salePrice)
                    val margin = Formatters.calculateMargin(product.costPrice, product.salePrice)

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Custo", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(Formatters.formatCurrency(product.costPrice), fontSize = 12.sp)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Lucro / Margem", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = "${Formatters.formatCurrency(unitProfit)} (${"%.0f".format(margin)}%)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (product.location.isNotBlank()) {
                    Text(
                        text = "Local: ${product.location}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Row {
                    OutlinedButton(
                        onClick = onAdjustStock,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.SwapVert, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Movimentar", fontSize = 11.sp)
                    }

                    if (isAdmin) {
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onEdit,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductFormDialog(
    editingProduct: ProductEntity?,
    onDismiss: () -> Unit,
    onSave: (
        name: String, sku: String, barcode: String, category: ProductCategory,
        brand: String, model: String, description: String, supplier: String,
        costPrice: Double, salePrice: Double, stockQuantity: Int, minStock: Int, location: String
    ) -> Unit
) {
    var name by remember { mutableStateOf(editingProduct?.name ?: "") }
    var sku by remember { mutableStateOf(editingProduct?.sku ?: "") }
    var barcode by remember { mutableStateOf(editingProduct?.barcode ?: "") }
    var category by remember { mutableStateOf(editingProduct?.category ?: ProductCategory.TELAS) }
    var brand by remember { mutableStateOf(editingProduct?.brand ?: "") }
    var model by remember { mutableStateOf(editingProduct?.compatibleModel ?: "") }
    var description by remember { mutableStateOf(editingProduct?.description ?: "") }
    var supplier by remember { mutableStateOf(editingProduct?.supplier ?: "") }
    var costPriceStr by remember { mutableStateOf(editingProduct?.costPrice?.let { if (it > 0) it.toString() else "" } ?: "") }
    var salePriceStr by remember { mutableStateOf(editingProduct?.salePrice?.let { if (it > 0) it.toString() else "" } ?: "") }
    var stockStr by remember { mutableStateOf(editingProduct?.stockQuantity?.toString() ?: "5") }
    var minStockStr by remember { mutableStateOf(editingProduct?.minStock?.toString() ?: "3") }
    var location by remember { mutableStateOf(editingProduct?.location ?: "") }

    var expandedCat by remember { mutableStateOf(false) }

    val cost = costPriceStr.toDoubleOrNull() ?: 0.0
    val sale = salePriceStr.toDoubleOrNull() ?: 0.0
    val unitProfit = Formatters.calculateUnitProfit(cost, sale)
    val margin = Formatters.calculateMargin(cost, sale)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editingProduct == null) "Cadastrar Produto / Peça" else "Editar Produto") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do Produto / Peça *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = sku,
                        onValueChange = { sku = it },
                        label = { Text("Código SKU *") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = barcode,
                        onValueChange = { barcode = it },
                        label = { Text("Cód. Barras") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Category selector
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = category.label,
                        onValueChange = {},
                        label = { Text("Categoria") },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { expandedCat = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expandedCat = true }
                    )
                    DropdownMenu(
                        expanded = expandedCat,
                        onDismissRequest = { expandedCat = false }
                    ) {
                        ProductCategory.values().forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.label) },
                                onClick = {
                                    category = cat
                                    expandedCat = false
                                }
                            )
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = { Text("Marca") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = { Text("Modelo Compatível") },
                        modifier = Modifier.weight(1.5f)
                    )
                }

                // Cost and Sale price inputs
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = costPriceStr,
                        onValueChange = { costPriceStr = it },
                        label = { Text("Custo Aquisição (R$) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = salePriceStr,
                        onValueChange = { salePriceStr = it },
                        label = { Text("Preço Venda (R$) *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Live automated Profit & Margin feedback card (Section 4 prompt requirement)
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Lucro Bruto Unitário:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(Formatters.formatCurrency(unitProfit), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Margem Bruta Estimada:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("${"%.1f".format(margin)}%", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = stockStr,
                        onValueChange = { stockStr = it },
                        label = { Text("Estoque Atual *") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = minStockStr,
                        onValueChange = { minStockStr = it },
                        label = { Text("Estoque Mínimo") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Localização Física (ex: Gaveta A1)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = supplier,
                    onValueChange = { supplier = it },
                    label = { Text("Fornecedor") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição detalhada") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val stock = stockStr.toIntOrNull() ?: 0
                    val minStock = minStockStr.toIntOrNull() ?: 3
                    onSave(name, sku, barcode, category, brand, model, description, supplier, cost, sale, stock, minStock, location)
                },
                enabled = name.isNotBlank() && sku.isNotBlank() && sale > 0.0
            ) {
                Text("Salvar Produto")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun StockMovementDialog(
    product: ProductEntity,
    isAdmin: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (quantityChange: Int, type: StockMovementType, reason: String) -> Unit
) {
    var quantityStr by remember { mutableStateOf("1") }
    var selectedType by remember { mutableStateOf(StockMovementType.ENTRADA_COMPRA) }
    var reason by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Movimentar Estoque") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "${product.name}\nEstoque Atual: ${product.stockQuantity} unidades",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )

                Text("Tipo de Movimentação:", style = MaterialTheme.typography.labelSmall)
                Column {
                    val types = listOf(
                        StockMovementType.ENTRADA_COMPRA,
                        StockMovementType.AJUSTE_INVENTARIO,
                        StockMovementType.DEVOLUCAO,
                        StockMovementType.PERDA_AVARIA
                    )
                    types.forEach { type ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedType = type }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedType == type,
                                onClick = { selectedType = type }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(type.label, fontSize = 13.sp)
                        }
                    }
                }

                OutlinedTextField(
                    value = quantityStr,
                    onValueChange = { quantityStr = it },
                    label = { Text("Quantidade (Use negativo para saídas)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Motivo / Justificativa *") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val rawQty = quantityStr.toIntOrNull() ?: 0
                    val finalQty = if (selectedType == StockMovementType.PERDA_AVARIA && rawQty > 0) -rawQty else rawQty
                    onConfirm(finalQty, selectedType, reason.ifBlank { selectedType.label })
                },
                enabled = quantityStr.isNotBlank() && reason.isNotBlank()
            ) {
                Text("Confirmar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun StockMovementCard(movement: StockMovementEntity) {
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
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = movement.productName,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "${movement.type.label} • ${Formatters.formatDateTime(movement.dateEpoch)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Motivo: ${movement.reason} (Por: ${movement.userName})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            val isPositive = movement.quantityChange > 0
            val badgeColor = if (isPositive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(badgeColor.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = (if (isPositive) "+ " else "") + "${movement.quantityChange} un.",
                    fontWeight = FontWeight.ExtraBold,
                    color = badgeColor,
                    fontSize = 12.sp
                )
            }
        }
    }
}
