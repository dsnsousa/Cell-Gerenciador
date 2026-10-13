package com.example.ui.financial

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinancialScreen(
    viewModel: FinancialViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0 = DRE / Visão Geral, 1 = Despesas, 2 = Fornecedores

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
                    text = { Text("Demonstrativo", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.AccountBalance, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Despesas", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Fornecedores", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.LocalShipping, contentDescription = null) }
                )
            }
        },
        floatingActionButton = {
            if (selectedTab == 1) {
                FloatingActionButton(
                    onClick = viewModel::openExpenseForm,
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.testTag("new_expense_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nova Despesa")
                }
            } else if (selectedTab == 2) {
                FloatingActionButton(
                    onClick = viewModel::openSupplierForm,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.testTag("new_supplier_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Novo Fornecedor")
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
                // DRE & GENERAL FINANCIAL TAB
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Period Selector
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = state.selectedPeriod == 0,
                                onClick = { viewModel.setPeriod(0) },
                                label = { Text("Hoje") }
                            )
                            FilterChip(
                                selected = state.selectedPeriod == 1,
                                onClick = { viewModel.setPeriod(1) },
                                label = { Text("Mês Atual") }
                            )
                            FilterChip(
                                selected = state.selectedPeriod == 2,
                                onClick = { viewModel.setPeriod(2) },
                                label = { Text("Todo Histórico") }
                            )
                        }
                    }

                    // Managerial DRE Card
                    item {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Demonstrativo Gerencial de Resultados",
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "*Estimativas gerenciais internas para controle da assistência técnica.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.outline
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                DreRow(label = "(+) Faturamento Bruto:", value = Formatters.formatCurrency(state.periodRevenue), isPositive = true)
                                DreRow(label = "   • Vendas de Peças e Acessórios:", value = Formatters.formatCurrency(state.periodPartsRevenue), isSub = true)
                                DreRow(label = "   • Mão de Obra de Serviços (OS):", value = Formatters.formatCurrency(state.periodServicesRevenue), isSub = true)
                                DreRow(label = "(-) Descontos Concedidos:", value = Formatters.formatCurrency(state.periodDiscounts), isNegative = true)
                                DreRow(label = "(-) Custo das Peças Vendidas (CMV):", value = Formatters.formatCurrency(state.periodCmv), isNegative = true)

                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                DreRow(
                                    label = "(=) Lucro Bruto Estimado:",
                                    value = Formatters.formatCurrency(state.periodGrossProfit),
                                    isBold = true,
                                    highlightColor = MaterialTheme.colorScheme.tertiary
                                )

                                DreRow(label = "(-) Despesas Operacionais Pagas:", value = Formatters.formatCurrency(state.periodExpensesPaid), isNegative = true)

                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                val netColor = if (state.periodNetEstimatedResult >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                                DreRow(
                                    label = "(=) Resultado Líquido Estimado:",
                                    value = Formatters.formatCurrency(state.periodNetEstimatedResult),
                                    isBold = true,
                                    highlightColor = netColor,
                                    isLarge = true
                                )
                            }
                        }
                    }

                    // Cashflow received vs pending
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Efetivamente Recebido", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = Formatters.formatCurrency(state.periodReceived),
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.tertiary,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }

                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Valores a Receber (Fiado)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = Formatters.formatCurrency(state.periodPending),
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    }

                    // Payment Method Breakdown
                    if (state.paymentMethodBreakdown.isNotEmpty()) {
                        item {
                            Text(
                                text = "Vendas por Forma de Pagamento",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }

                        item {
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    state.paymentMethodBreakdown.forEach { (method, amount) ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(method.label, style = MaterialTheme.typography.bodyMedium)
                                            Text(
                                                text = Formatters.formatCurrency(amount),
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (selectedTab == 1) {
                // EXPENSES TAB
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = state.selectedExpenseFilter == null,
                            onClick = { viewModel.setExpenseFilter(null) },
                            label = { Text("Todas") }
                        )
                        FilterChip(
                            selected = state.selectedExpenseFilter == ExpenseStatus.PENDENTE,
                            onClick = { viewModel.setExpenseFilter(ExpenseStatus.PENDENTE) },
                            label = { Text("Pendentes") }
                        )
                        FilterChip(
                            selected = state.selectedExpenseFilter == ExpenseStatus.PAGO,
                            onClick = { viewModel.setExpenseFilter(ExpenseStatus.PAGO) },
                            label = { Text("Pagas") }
                        )
                    }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.filteredExpenses) { expense ->
                            ExpenseCard(
                                expense = expense,
                                onPay = { viewModel.markExpensePaid(expense) }
                            )
                        }

                        if (state.filteredExpenses.isEmpty()) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                    Text("Nenhuma despesa encontrada.")
                                }
                            }
                        }
                    }
                }
            } else {
                // SUPPLIERS TAB
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(state.suppliers) { supplier ->
                        SupplierCard(supplier = supplier)
                    }

                    if (state.suppliers.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                                Text("Nenhum fornecedor cadastrado.")
                            }
                        }
                    }
                }
            }
        }
    }

    // Expense Form Dialog
    if (state.isExpenseFormOpen) {
        ExpenseFormDialog(
            onDismiss = viewModel::closeExpenseForm,
            onSave = viewModel::saveExpense
        )
    }

    // Supplier Form Dialog
    if (state.isSupplierFormOpen) {
        SupplierFormDialog(
            onDismiss = viewModel::closeSupplierForm,
            onSave = viewModel::saveSupplier
        )
    }
}

@Composable
fun DreRow(
    label: String,
    value: String,
    isPositive: Boolean = false,
    isNegative: Boolean = false,
    isSub: Boolean = false,
    isBold: Boolean = false,
    isLarge: Boolean = false,
    highlightColor: Color? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = if (isLarge) 15.sp else if (isSub) 12.sp else 13.sp,
            fontWeight = if (isBold || isLarge) FontWeight.Bold else FontWeight.Normal,
            color = if (isSub) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = value,
            fontSize = if (isLarge) 17.sp else if (isSub) 12.sp else 14.sp,
            fontWeight = if (isBold || isLarge) FontWeight.ExtraBold else FontWeight.SemiBold,
            color = highlightColor ?: if (isNegative) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ExpenseCard(
    expense: ExpenseEntity,
    onPay: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(expense.description, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = "${expense.category.label} • Vencimento: ${Formatters.formatDate(expense.dueDateEpoch)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (expense.notes.isNotBlank()) {
                    Text(expense.notes, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = Formatters.formatCurrency(expense.amount),
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (expense.status == ExpenseStatus.PENDENTE) {
                    Button(
                        onClick = onPay,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Pagar", fontSize = 11.sp)
                    }
                } else {
                    AssistChip(
                        onClick = {},
                        label = { Text("Pago", fontSize = 10.sp, color = MaterialTheme.colorScheme.tertiary) },
                        modifier = Modifier.height(26.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SupplierCard(supplier: SupplierEntity) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(supplier.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            if (supplier.companyName.isNotBlank()) {
                Text(supplier.companyName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("Telefone: ${Formatters.formatPhone(supplier.phone)}", style = MaterialTheme.typography.bodySmall)
            if (supplier.notes.isNotBlank()) {
                Text(supplier.notes, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun ExpenseFormDialog(
    onDismiss: () -> Unit,
    onSave: (description: String, category: ExpenseCategory, amount: Double, dueDate: Long, isPaid: Boolean, notes: String) -> Unit
) {
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf(ExpenseCategory.ALUGUEL) }
    var amountStr by remember { mutableStateOf("") }
    var isPaid by remember { mutableStateOf(true) }
    var notes by remember { mutableStateOf("") }
    var expandedCat by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cadastrar Despesa") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descrição (ex: Aluguel, Luz) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

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
                    DropdownMenu(expanded = expandedCat, onDismissRequest = { expandedCat = false }) {
                        ExpenseCategory.values().forEach { cat ->
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

                OutlinedTextField(
                    value = amountStr,
                    onValueChange = { amountStr = it },
                    label = { Text("Valor (R$) *") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = isPaid, onCheckedChange = { isPaid = it })
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Já foi paga?")
                }

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observações") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountStr.toDoubleOrNull() ?: 0.0
                    onSave(description, category, amount, System.currentTimeMillis(), isPaid, notes)
                },
                enabled = description.isNotBlank() && amountStr.toDoubleOrNull() != null
            ) {
                Text("Salvar Despesa")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun SupplierFormDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, company: String, phone: String, email: String, notes: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var company by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cadastrar Fornecedor") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nome do Contato *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text("Empresa / Razão Social") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Telefone / WhatsApp") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-mail") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Observações (Peças fornecidas, prazos)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, company, phone, email, notes) },
                enabled = name.isNotBlank()
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
