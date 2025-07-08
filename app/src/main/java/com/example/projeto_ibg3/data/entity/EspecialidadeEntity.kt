package com.example.projeto_ibg3.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.projeto_ibg3.model.SyncStatus

@Entity(tableName = "especialidades")
data class EspecialidadeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "server_id")
    val serverId: Long? = null,

    @ColumnInfo(name = "nome")
    val nome: String,

    // Campos para sincronização
    @ColumnInfo(name = "sync_status")
    val syncStatus: SyncStatus = SyncStatus.SYNCED,

    @ColumnInfo(name = "last_modified")
    val lastModified: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false
)
