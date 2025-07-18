package com.example.projeto_ibg3.core.base

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.projeto_ibg3.domain.model.SyncStatus

@Entity
abstract class BaseEntity {
    @PrimaryKey(autoGenerate = true)
    open val id: Long = 0

    @ColumnInfo(name = "server_id")
    open val serverId: Long? = null

    @ColumnInfo(name = "created_at")
    open val createdAt: Long = System.currentTimeMillis()

    @ColumnInfo(name = "updated_at")
    open val updatedAt: Long = System.currentTimeMillis()

    @ColumnInfo(name = "is_deleted")
    open val isDeleted: Boolean = false

    @ColumnInfo(name = "deleted_at")
    open val deletedAt: Long? = null

    @ColumnInfo(name = "sync_status")
    open val syncStatus: SyncStatus = SyncStatus.SYNCED
}