package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestampEpoch: Long = System.currentTimeMillis(),
    val userId: Long,
    val userName: String,
    val action: String,
    val details: String
)
