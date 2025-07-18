package com.example.projeto_ibg3.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "sync_queue")
data class SyncQueue(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val entityType: String, // "PACIENTE", "ESPECIALIDADE", etc.
    val entityId: String, // ID local da entidade
    val operation: String, // "INSERT", "UPDATE", "DELETE"
    val jsonData: String, // Dados serializados da entidade
    val timestamp: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val syncStatus: String = "PENDING", // "PENDING", "SYNCED", "ERROR"
    val errorMessage: String? = null
)