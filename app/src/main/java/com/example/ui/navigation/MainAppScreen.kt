package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.repository.EcellRepository
import com.example.ui.customers.CustomersScreen
import com.example.ui.customers.CustomersViewModel
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.dashboard.DashboardViewModel
import com.example.ui.financial.FinancialScreen
import com.example.ui.financial.FinancialViewModel
import com.example.ui.inventory.InventoryScreen
import com.example.ui.inventory.InventoryViewModel
import com.example.ui.pos.PosScreen
import com.example.ui.pos.PosViewModel
import com.example.ui.reports.ReportsScreen
import com.example.ui.serviceorders.ServiceOrdersScreen
import com.example.ui.serviceorders.ServiceOrdersViewModel
import com.example.ui.users.UsersAdminScreen

enum class AppDestination(val label: String, val icon: ImageVector) {
    DASHBOARD("Início", Icons.Default.Dashboard),
    POS("PDV", Icons.Default.PointOfSale),
    ORDERS("Ordens (OS)", Icons.Default.Build),
    CUSTOMERS("Clientes", Icons.Default.People),
    INVENTORY("Estoque", Icons.Default.Inventory2),
    FINANCIAL("Financeiro", Icons.Default.AccountBalance),
    REPORTS("Relatórios", Icons.Default.Assessment),
    USERS("Usuários", Icons.Default.ManageAccounts)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    repository: EcellRepository,
    currentUser: UserEntity,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val initialDestination = when (currentUser.role) {
        UserRole.ADMIN -> AppDestination.DASHBOARD
        UserRole.TECNICO -> AppDestination.ORDERS
        UserRole.VENDEDOR -> AppDestination.POS
    }

    var currentDestination by remember { mutableStateOf(initialDestination) }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showMoreSheet by remember { mutableStateOf(false) }

    // Intercept Back button
    BackHandler(enabled = currentDestination != initialDestination) {
        currentDestination = initialDestination
    }

    // ViewModels scoped to repository & user
    val dashboardViewModel = remember(currentUser.id) { DashboardViewModel(repository, currentUser) }
    val posViewModel = remember(currentUser.id) { PosViewModel(repository, currentUser) }
    val serviceOrdersViewModel = remember(currentUser.id) { ServiceOrdersViewModel(repository, currentUser) }
    val customersViewModel = remember { CustomersViewModel(repository) }
    val inventoryViewModel = remember(currentUser.id) { InventoryViewModel(repository, currentUser) }
    val financialViewModel = remember(currentUser.id) { FinancialViewModel(repository, currentUser) }

    // Available bottom navigation destinations according to role
    val bottomDestinations = when (currentUser.role) {
        UserRole.ADMIN -> listOf(
            AppDestination.DASHBOARD,
            AppDestination.POS,
            AppDestination.ORDERS,
            AppDestination.CUSTOMERS,
            AppDestination.INVENTORY
        )
        UserRole.TECNICO -> listOf(
            AppDestination.ORDERS,
            AppDestination.INVENTORY,
            AppDestination.CUSTOMERS,
            AppDestination.DASHBOARD
        )
        UserRole.VENDEDOR -> listOf(
            AppDestination.POS,
            AppDestination.CUSTOMERS,
            AppDestination.INVENTORY,
            AppDestination.DASHBOARD
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "ECELL GESTÃO",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val roleColor = when (currentUser.role) {
                                UserRole.ADMIN -> MaterialTheme.colorScheme.primary
                                UserRole.TECNICO -> MaterialTheme.colorScheme.secondary
                                UserRole.VENDEDOR -> MaterialTheme.colorScheme.tertiary
                            }
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(roleColor)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "${currentUser.fullName} (${currentUser.role.label.split(" ").first()})",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    if (currentUser.role == UserRole.ADMIN) {
                        IconButton(
                            onClick = { showMoreSheet = true },
                            modifier = Modifier.testTag("more_menu_button")
                        ) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Mais opções")
                        }
                    }

                    IconButton(
                        onClick = { showLogoutDialog = true },
                        modifier = Modifier.testTag("logout_button")
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = "Sair da conta", tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                bottomDestinations.forEach { dest ->
                    val isSelected = currentDestination == dest
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentDestination = dest },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label, fontSize = 10.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentDestination) {
                AppDestination.DASHBOARD -> DashboardScreen(
                    viewModel = dashboardViewModel,
                    onNavigateToPos = { currentDestination = AppDestination.POS },
                    onNavigateToOrders = { currentDestination = AppDestination.ORDERS },
                    onNavigateToCustomers = { currentDestination = AppDestination.CUSTOMERS },
                    onNavigateToInventory = { currentDestination = AppDestination.INVENTORY }
                )
                AppDestination.POS -> PosScreen(viewModel = posViewModel)
                AppDestination.ORDERS -> ServiceOrdersScreen(viewModel = serviceOrdersViewModel)
                AppDestination.CUSTOMERS -> CustomersScreen(viewModel = customersViewModel)
                AppDestination.INVENTORY -> InventoryScreen(viewModel = inventoryViewModel)
                AppDestination.FINANCIAL -> FinancialScreen(viewModel = financialViewModel)
                AppDestination.REPORTS -> ReportsScreen(repository = repository, currentUser = currentUser)
                AppDestination.USERS -> UsersAdminScreen(repository = repository, currentUser = currentUser)
            }
        }
    }

    // Admin More Menu ModalBottomSheet
    if (showMoreSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMoreSheet = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Módulos Administrativos",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )

                ListItem(
                    leadingContent = { Icon(Icons.Default.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    headlineContent = { Text("Módulo Financeiro & DRE") },
                    supportingContent = { Text("Faturamento, contas a pagar, CMV e despesas") },
                    modifier = Modifier.clickable {
                        currentDestination = AppDestination.FINANCIAL
                        showMoreSheet = false
                    }
                )

                ListItem(
                    leadingContent = { Icon(Icons.Default.Assessment, contentDescription = null, tint = MaterialTheme.colorScheme.secondary) },
                    headlineContent = { Text("Relatórios & Comissões") },
                    supportingContent = { Text("Comissões por vendedor e exportação CSV") },
                    modifier = Modifier.clickable {
                        currentDestination = AppDestination.REPORTS
                        showMoreSheet = false
                    }
                )

                ListItem(
                    leadingContent = { Icon(Icons.Default.ManageAccounts, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary) },
                    headlineContent = { Text("Gerenciamento de Usuários") },
                    supportingContent = { Text("Criar e configurar técnicos e vendedores") },
                    modifier = Modifier.clickable {
                        currentDestination = AppDestination.USERS
                        showMoreSheet = false
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Logout Confirmation Dialog
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon = { Icon(Icons.Default.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Sair da Conta") },
            text = { Text("Deseja realmente sair da sessão de ${currentUser.fullName}?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLogoutDialog = false
                        onLogout()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Sair")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
