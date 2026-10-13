package com.example.ui.users

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.database.DatabaseInitializer
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.repository.EcellRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UsersAdminScreen(
    repository: EcellRepository,
    currentUser: UserEntity,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    var users by remember { mutableStateOf<List<UserEntity>>(emptyList()) }
    var showUserForm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        repository.getAllUsers().collect { users = it }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showUserForm = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_user_fab")
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = "Novo Usuário")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Gerenciamento de Usuários e Permissões",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Cadastre técnicos e vendedores com comissões e perfis de segurança.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(users) { user ->
                Card(
                    shape = RoundedCornerShape(14.dp),
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
                            Text(user.fullName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = "Usuário: @${user.username} • ${user.role.label}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Comissão: ${user.commissionRate}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Switch(
                                checked = user.isActive,
                                onCheckedChange = {
                                    coroutineScope.launch {
                                        repository.toggleUserActive(user.id, currentUser)
                                    }
                                }
                            )
                            Text(
                                text = if (user.isActive) "Ativo" else "Bloqueado",
                                fontSize = 10.sp,
                                color = if (user.isActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }

    if (showUserForm) {
        NewUserDialog(
            onDismiss = { showUserForm = false },
            onSave = { username, fullName, role, password, email, phone, commission ->
                coroutineScope.launch {
                    val newUser = UserEntity(
                        username = username,
                        fullName = fullName,
                        role = role,
                        passwordHash = DatabaseInitializer.hashPassword(password),
                        email = email,
                        phone = phone,
                        commissionRate = commission,
                        isActive = true
                    )
                    repository.saveUser(newUser, currentUser)
                    showUserForm = false
                }
            }
        )
    }
}

@Composable
fun NewUserDialog(
    onDismiss: () -> Unit,
    onSave: (username: String, fullName: String, role: UserRole, password: String, email: String, phone: String, commission: Double) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var role by remember { mutableStateOf(UserRole.VENDEDOR) }
    var password by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var commissionStr by remember { mutableStateOf("3.0") }
    var expandedRole by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cadastrar Novo Usuário") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = fullName,
                    onValueChange = { fullName = it },
                    label = { Text("Nome Completo *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Nome de Usuário (Login) *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Senha *") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )

                // Role Dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = role.label,
                        onValueChange = {},
                        label = { Text("Perfil de Acesso") },
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { expandedRole = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    DropdownMenu(expanded = expandedRole, onDismissRequest = { expandedRole = false }) {
                        UserRole.values().forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r.label) },
                                onClick = {
                                    role = r
                                    expandedRole = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = commissionStr,
                    onValueChange = { commissionStr = it },
                    label = { Text("Percentual de Comissão (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("E-mail") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Telefone") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val comm = commissionStr.toDoubleOrNull() ?: 0.0
                    onSave(username, fullName, role, password, email, phone, comm)
                },
                enabled = username.isNotBlank() && fullName.isNotBlank() && password.isNotBlank()
            ) {
                Text("Cadastrar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
