package com.example.projeto_ibg3.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.projeto_ibg3.domain.model.SyncStatus
import java.util.UUID

@Entity(tableName = "especialidades")
data class EspecialidadeEntity(
    // ID local sempre presente
    @PrimaryKey
    @ColumnInfo(name = "local_id")
    val localId: String = UUID.randomUUID().toString(),

    // ID do servidor (pode ser nulo até sincronizar)
    @ColumnInfo(name = "server_id")
    val serverId: Long? = null,

    @ColumnInfo(name = "nome")
    val nome: String,

    // Campos para sincronização
    @ColumnInfo(name = "sync_status")
    val syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD,

    @ColumnInfo(name = "device_id")
    val deviceId: String, // Identifica qual device criou o registro

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "last_sync_timestamp")
    val lastSyncTimestamp: Long = 0,

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false
)