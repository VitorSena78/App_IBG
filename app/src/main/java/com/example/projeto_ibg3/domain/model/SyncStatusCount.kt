package com.example.projeto_ibg3.domain.model

import androidx.room.ColumnInfo

data class SyncStatusCount(
    @ColumnInfo(name = "sync_status") val syncStatus: SyncStatus,
    val count: Int
)
