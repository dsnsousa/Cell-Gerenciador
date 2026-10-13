package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole(val label: String, val description: String) {
    ADMIN("Administrador (ADM)", "Acesso completo a todas as funções e relatórios"),
    TECNICO("Técnico", "Ordens de serviço, manutenção, peças e clientes"),
    VENDEDOR("Vendedor", "PDV rápido, vendas de peças e acessórios, clientes")
}

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val username: String,
    val fullName: String,
    val role: UserRole,
    val passwordHash: String,
    val email: String = "",
    val phone: String = "",
    val commissionRate: Double = 3.0, // default 3%
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
