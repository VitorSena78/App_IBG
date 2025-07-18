package com.example.projeto_ibg3.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.projeto_ibg3.domain.model.SyncAction
import java.util.UUID

@Entity(tableName = "sync_metadata")
data class SyncMetadataEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "entity_type")
    val entityType: String, // "PACIENTE", "ESPECIALIDADE", etc.

    @ColumnInfo(name = "entity_id")
    val entityId: String, // local_id da entidade

    @ColumnInfo(name = "action")
    val action: SyncAction,

    @ColumnInfo(name = "json_data")
    val jsonData: String, // Dados serializados da entidade

    @ColumnInfo(name = "priority")
    val priority: Int = 1, // 1 = baixa, 2 = média, 3 = alta

    @ColumnInfo(name = "attempts")
    val attempts: Int = 0,

    @ColumnInfo(name = "max_attempts")
    val maxAttempts: Int = 3,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "last_attempt_at")
    val lastAttemptAt: Long = 0,

    @ColumnInfo(name = "error")
    val error: String? = null,

    @ColumnInfo(name = "device_id")
    val deviceId: String
) {
    // Métodos auxiliares
    fun canRetry(): Boolean = attempts < maxAttempts

    fun isExpired(maxAgeMs: Long = 86400000): Boolean { // 24 horas
        return System.currentTimeMillis() - createdAt > maxAgeMs
    }

    fun incrementAttempts(errorMessage: String? = null): SyncMetadataEntity {
        return copy(
            attempts = attempts + 1,
            lastAttemptAt = System.currentTimeMillis(),
            error = errorMessage
        )
    }
}