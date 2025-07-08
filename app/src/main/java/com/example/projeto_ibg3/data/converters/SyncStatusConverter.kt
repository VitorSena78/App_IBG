package com.example.projeto_ibg3.data.converters

import androidx.room.TypeConverter
import com.example.projeto_ibg3.model.SyncStatus

class SyncStatusConverter {
    @TypeConverter
    fun fromSyncStatus(status: SyncStatus): String {
        return status.name
    }

    @TypeConverter
    fun toSyncStatus(status: String): SyncStatus {
        return SyncStatus.valueOf(status)
    }
}