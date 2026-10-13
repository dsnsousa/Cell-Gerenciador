package com.example.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.DatabaseInitializer
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.repository.EcellRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val currentUser: UserEntity? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class AuthViewModel(private val repository: EcellRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Preencha usuário e senha")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val user = repository.authenticate(username, password)
            if (user != null) {
                _uiState.value = _uiState.value.copy(
                    currentUser = user,
                    isLoading = false,
                    errorMessage = null
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Credenciais inválidas ou usuário bloqueado."
                )
            }
        }
    }

    fun quickLoginAs(role: UserRole) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            val username = when (role) {
                UserRole.ADMIN -> "admin"
                UserRole.TECNICO -> "tecnico"
                UserRole.VENDEDOR -> "vendedor"
            }
            val password = when (role) {
                UserRole.ADMIN -> "admin123"
                UserRole.TECNICO -> "tec123"
                UserRole.VENDEDOR -> "ven123"
            }
            val user = repository.authenticate(username, password)
            _uiState.value = _uiState.value.copy(
                currentUser = user,
                isLoading = false,
                errorMessage = if (user == null) "Usuário não encontrado. Reinicialize os dados." else null
            )
        }
    }

    fun logout() {
        _uiState.value = AuthUiState()
    }

    fun clearMessages() {
        _uiState.value = _uiState.value.copy(errorMessage = null, successMessage = null)
    }

    fun requestPasswordReset(usernameOrEmail: String) {
        if (usernameOrEmail.isBlank()) {
            _uiState.value = _uiState.value.copy(errorMessage = "Informe seu usuário ou e-mail cadastrado.")
            return
        }
        _uiState.value = _uiState.value.copy(
            successMessage = "Instruções de recuperação enviadas para o e-mail cadastrado de '$usernameOrEmail'."
        )
    }
}
