package com.example.ui.serviceorders

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.data.repository.EcellRepository
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServiceOrdersScreen(
    viewModel: ServiceOrdersViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::openNewOrderForm,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("new_os_fab")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nova Ordem de Serviço")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search and Status filters
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    placeholder = { Text("Buscar por OS, cliente, aparelho ou IMEI...") },
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
                        .testTag("os_search_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    item {
                        FilterChip(
                            selected = state.selectedStatusFilter == null,
                            onClick = { viewModel.onStatusFilterChanged(null) },
                            label = { Text("Todas") }
                        )
                    }
                    items(ServiceOrderStatus.values()) { status ->
                        FilterChip(
                            selected = state.selectedStatusFilter == status,
                            onClick = { viewModel.onStatusFilterChanged(status) },
                            label = { Text(status.label, fontSize = 11.sp) }
                        )
                    }
                }
            }

            // Orders List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.filteredOrders) { order ->
                    ServiceOrderCard(
                        order = order,
                        onStatusChange = { newStatus -> viewModel.updateOrderStatus(order.id, newStatus) },
                        onEdit = { viewModel.openEditOrderForm(order) },
                        onNotifyWhatsApp = {
                            val msg = buildWhatsAppNotification(order)
                            val cleanNumber = Formatters.cleanPhone(order.customerPhone)
                            val url = "https://api.whatsapp.com/send?phone=55$cleanNumber&text=${Uri.encode(msg)}"
                            try {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                // fallback generic share
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, msg)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Enviar Aviso"))
                            }
                        }
                    )
                }

                if (state.filteredOrders.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nenhuma Ordem de Serviço encontrada.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }

    // New / Edit OS Form Dialog
    if (state.isFormOpen) {
        ServiceOrderFormDialog(
            editingOrder = state.editingOrder,
            customers = state.customers,
            products = state.products,
            onDismiss = viewModel::closeForm,
            onSave = { name, phone, brand, model, imei, defect, diag, labor, parts, notes ->
                viewModel.saveServiceOrder(name, phone, brand, model, imei, defect, diag, labor, parts, notes)
            }
        )
    }

    // Error Alert
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
fun ServiceOrderCard(
    order: ServiceOrderEntity,
    onStatusChange: (ServiceOrderStatus) -> Unit,
    onEdit: () -> Unit,
    onNotifyWhatsApp: () -> Unit
) {
    var expandedMenu by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: OS Number and Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = order.orderNumber,
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "• ${order.phoneBrand} ${order.phoneModel}",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Status Badge (clickable to change status)
                Box {
                    val statusColor = Color(order.status.colorHex)
                    AssistChip(
                        onClick = { expandedMenu = true },
                        label = {
                            Text(
                                text = order.status.label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        },
                        trailingIcon = {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Alterar status",
                                tint = statusColor,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = statusColor.copy(alpha = 0.12f)
                        )
                    )

                    DropdownMenu(
                        expanded = expandedMenu,
                        onDismissRequest = { expandedMenu = false }
                    ) {
                        ServiceOrderStatus.values().forEach { st ->
                            DropdownMenuItem(
                                text = { Text(st.label) },
                                onClick = {
                                    onStatusChange(st)
                                    expandedMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Customer and Phone
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${order.customerName} (${Formatters.formatPhone(order.customerPhone)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (order.imei.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "IMEI: ${order.imei}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Defect and diagnosis
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text(
                        text = "Defeito: ${order.reportedDefect}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    if (order.technicalDiagnosis.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Diagnóstico: ${order.technicalDiagnosis}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Financial & Dates row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Mão de Obra: ${Formatters.formatCurrency(order.laborPrice)} | Peças: ${Formatters.formatCurrency(order.partsPrice)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Total: ${Formatters.formatCurrency(order.totalPrice)}",
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                Row {
                    // WhatsApp notify button
                    IconButton(
                        onClick = onNotifyWhatsApp,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF25D366).copy(alpha = 0.15f))
                    ) {
                        Icon(
                            Icons.Default.Chat,
                            contentDescription = "Notificar WhatsApp",
                            tint = Color(0xFF128C7E),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Editar OS",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ServiceOrderFormDialog(
    editingOrder: ServiceOrderEntity?,
    customers: List<CustomerEntity>,
    products: List<ProductEntity>,
    onDismiss: () -> Unit,
    onSave: (
        customerName: String,
        customerPhone: String,
        phoneBrand: String,
        phoneModel: String,
        imei: String,
        defect: String,
        diagnosis: String,
        laborPrice: Double,
        parts: List<EcellRepository.CartItem>,
        notes: String
    ) -> Unit
) {
    var customerName by remember { mutableStateOf(editingOrder?.customerName ?: "") }
    var customerPhone by remember { mutableStateOf(editingOrder?.customerPhone ?: "") }
    var phoneBrand by remember { mutableStateOf(editingOrder?.phoneBrand ?: "Samsung") }
    var phoneModel by remember { mutableStateOf(editingOrder?.phoneModel ?: "") }
    var imei by remember { mutableStateOf(editingOrder?.imei ?: "") }
    var reportedDefect by remember { mutableStateOf(editingOrder?.reportedDefect ?: "") }
    var technicalDiagnosis by remember { mutableStateOf(editingOrder?.technicalDiagnosis ?: "") }
    var laborPriceStr by remember { mutableStateOf(editingOrder?.laborPrice?.let { if (it > 0) it.toString() else "" } ?: "") }
    var notes by remember { mutableStateOf(editingOrder?.notes ?: "") }

    val selectedParts = remember { mutableStateListOf<EcellRepository.CartItem>() }
    var showPartPicker by remember { mutableStateOf(false) }

    val laborPrice = laborPriceStr.toDoubleOrNull() ?: 0.0
    val partsPrice = selectedParts.sumOf { it.product.salePrice * it.quantity }
    val totalPrice = laborPrice + partsPrice

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editingOrder == null) "Nova Ordem de Serviço" else "Editar ${editingOrder.orderNumber}") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = customerName,
                    onValueChange = { customerName = it },
                    label = { Text("Nome do Cliente *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = customerPhone,
                    onValueChange = { customerPhone = it },
                    label = { Text("Telefone / WhatsApp *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = phoneBrand,
                        onValueChange = { phoneBrand = it },
                        label = { Text("Marca") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = phoneModel,
                        onValueChange = { phoneModel = it },
                        label = { Text("Modelo *") },
                        modifier = Modifier.weight(1.5f)
                    )
                }

                OutlinedTextField(
                    value = imei,
                    onValueChange = { imei = it },
                    label = { Text("IMEI (Opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reportedDefect,
                    onValueChange = { reportedDefect = it },
                    label = { Text("Defeito Informado pelo Cliente *") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = technicalDiagnosis,
                    onValueChange = { technicalDiagnosis = it },
                    label = { Text("Diagnóstico Técnico") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = laborPriceStr,
                    onValueChange = { laborPriceStr = it },
                    label = { Text("Valor Mão de Obra (R$)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                // Parts section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Peças do Estoque:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    TextButton(onClick = { showPartPicker = true }) {
                        Text("+ Adicionar Peça")
                    }
                }

                selectedParts.forEach { part ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(part.product.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            Text("${part.quantity}x ${Formatters.formatCurrency(part.product.salePrice)}", fontSize = 11.sp)
                        }
                        IconButton(onClick = { selectedParts.remove(part) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Remover", modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Total indicator
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total do Orçamento:", fontWeight = FontWeight.Bold)
                        Text(Formatters.formatCurrency(totalPrice), fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    }
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observações Internas") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(customerName, customerPhone, phoneBrand, phoneModel, imei, reportedDefect, technicalDiagnosis, laborPrice, selectedParts.toList(), notes)
                }
            ) {
                Text("Salvar OS")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )

    if (showPartPicker) {
        AlertDialog(
            onDismissRequest = { showPartPicker = false },
            title = { Text("Selecionar Peça do Estoque") },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    products.filter { it.stockQuantity > 0 }.forEach { p ->
                        ListItem(
                            headlineContent = { Text(p.name, fontSize = 13.sp) },
                            supportingContent = { Text("Estoque: ${p.stockQuantity} | ${Formatters.formatCurrency(p.salePrice)}") },
                            modifier = Modifier.clickable {
                                selectedParts.add(EcellRepository.CartItem(p, 1))
                                showPartPicker = false
                            }
                        )
                        HorizontalDivider()
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPartPicker = false }) { Text("Fechar") }
            }
        )
    }
}

fun buildWhatsAppNotification(order: ServiceOrderEntity): String {
    return when (order.status) {
        ServiceOrderStatus.PRONTO ->
            "Olá ${order.customerName}! Seu aparelho ${order.phoneBrand} ${order.phoneModel} já está PRONTO para retirada na ECELL GESTÃO!\n\nValor total: ${Formatters.formatCurrency(order.totalPrice)}.\nAguardamos você!"
        ServiceOrderStatus.AGUARDANDO_APROVACAO ->
            "Olá ${order.customerName}! Concluímos o diagnóstico do seu ${order.phoneModel}.\nDefeito: ${order.reportedDefect}\nServiço: ${order.technicalDiagnosis}\nOrçamento Total: ${Formatters.formatCurrency(order.totalPrice)}.\nPodemos iniciar o reparo?"
        else ->
            "Olá ${order.customerName}! Atualização da sua OS ${order.orderNumber} (${order.phoneModel}): Status atual: ${order.status.label}. Equipe ECELL GESTÃO."
    }
}
