package com.example.ui.customers

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CustomerEntity
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomersScreen(
    viewModel: CustomersViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::openNewCustomerForm,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("new_customer_fab")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Cadastrar Cliente")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp)
            ) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::onSearchQueryChanged,
                    placeholder = { Text("Buscar cliente por nome, telefone ou CPF...") },
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
                        .testTag("customer_search_input")
                )
            }

            // Customer List
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.filteredCustomers) { customer ->
                    CustomerCard(
                        customer = customer,
                        onEdit = { viewModel.openEditCustomerForm(customer) },
                        onHistory = { viewModel.openCustomerHistory(customer) },
                        onWhatsApp = {
                            val cleanPhone = Formatters.cleanPhone(customer.whatsapp.ifBlank { customer.phone })
                            val url = "https://api.whatsapp.com/send?phone=55$cleanPhone&text=${Uri.encode("Olá ${customer.name}! Tudo bem? Entramos em contato da ECELL GESTÃO.")}"
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            } catch (_: Exception) {
                                // fallback
                            }
                        }
                    )
                }

                if (state.filteredCustomers.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nenhum cliente cadastrado ou encontrado.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }
    }

    // Add / Edit Customer Dialog
    if (state.isFormOpen) {
        CustomerFormDialog(
            editingCustomer = state.editingCustomer,
            onDismiss = viewModel::closeForm,
            onSave = viewModel::saveCustomer
        )
    }

    // Customer History Dialog
    if (state.selectedCustomerHistory != null) {
        CustomerHistoryDialog(
            customer = state.selectedCustomerHistory!!,
            sales = state.customerSales,
            orders = state.customerOrders,
            onDismiss = viewModel::closeCustomerHistory
        )
    }
}

@Composable
fun CustomerCard(
    customer: CustomerEntity,
    onEdit: () -> Unit,
    onHistory: () -> Unit,
    onWhatsApp: () -> Unit
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
                    text = customer.name,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = Formatters.formatPhone(customer.phone),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (customer.cpf.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "CPF: ${customer.cpf}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (customer.address.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Endereço: ${customer.address}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Metrics row: Total spent and orders count
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total Gasto", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(Formatters.formatCurrency(customer.totalSpent), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Atendimentos", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${customer.ordersCount}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Última Visita", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(Formatters.formatDate(customer.lastVisitEpoch), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                // WhatsApp Button
                IconButton(
                    onClick = onWhatsApp,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color(0xFF25D366).copy(alpha = 0.15f))
                ) {
                    Icon(
                        Icons.Default.Chat,
                        contentDescription = "WhatsApp",
                        tint = Color(0xFF128C7E),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // History Button
                IconButton(
                    onClick = onHistory,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = "Histórico",
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Edit Button
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Editar",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CustomerFormDialog(
    editingCustomer: CustomerEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, cpf: String, phone: String, whatsapp: String, email: String, address: String, notes: String) -> Unit
) {
    var name by remember { mutableStateOf(editingCustomer?.name ?: "") }
    var cpf by remember { mutableStateOf(editingCustomer?.cpf ?: "") }
    var phone by remember { mutableStateOf(editingCustomer?.phone ?: "") }
    var whatsapp by remember { mutableStateOf(editingCustomer?.whatsapp ?: "") }
    var email by remember { mutableStateOf(editingCustomer?.email ?: "") }
    var address by remember { mutableStateOf(editingCustomer?.address ?: "") }
    var notes by remember { mutableStateOf(editingCustomer?.notes ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editingCustomer == null) "Novo Cliente" else "Editar Cliente") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome Completo *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = {
                        phone = it
                        if (whatsapp.isBlank()) whatsapp = it
                    },
                    label = { Text("Telefone *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = whatsapp,
                    onValueChange = { whatsapp = it },
                    label = { Text("WhatsApp") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cpf,
                    onValueChange = { cpf = it },
                    label = { Text("CPF (Opcional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-mail (Opcional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address,
                    onValueChange = { address = it },
                    label = { Text("Endereço (Opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observações") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, cpf, phone, whatsapp, email, address, notes) },
                enabled = name.isNotBlank() && phone.isNotBlank()
            ) {
                Text("Salvar")
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
fun CustomerHistoryDialog(
    customer: CustomerEntity,
    sales: List<com.example.data.model.SaleEntity>,
    orders: List<com.example.data.model.ServiceOrderEntity>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Histórico: ${customer.name}") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Ordens de Serviço (${orders.size}):",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                if (orders.isEmpty()) {
                    Text("Nenhum conserto registrado.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                } else {
                    orders.forEach { ord ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("${ord.orderNumber} • ${ord.phoneBrand} ${ord.phoneModel}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Status: ${ord.status.label} | Total: ${Formatters.formatCurrency(ord.totalPrice)}", fontSize = 11.sp)
                                Text("Defeito: ${ord.reportedDefect}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Text(
                    text = "Compras de Produtos (${sales.size}):",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                if (sales.isEmpty()) {
                    Text("Nenhuma compra registrada.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                } else {
                    sales.forEach { s ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("${s.saleNumber} • ${Formatters.formatDate(s.dateEpoch)}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("Total: ${Formatters.formatCurrency(s.total)} (${s.paymentMethod.label})", fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Fechar") }
        }
    )
}
