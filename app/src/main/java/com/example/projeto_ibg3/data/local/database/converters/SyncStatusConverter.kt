package com.example.projeto_ibg3.data.local.database.converters

import androidx.room.TypeConverter
import com.example.projeto_ibg3.domain.model.SyncStatus

class SyncStatusConverter {
    @TypeConverter
    fun fromSyncStatus(status: SyncStatus): String {
        return status.name
    }

    @TypeConverter
    fun toSyncStatus(statusString: String): SyncStatus {
        return try {
            SyncStatus.valueOf(statusString)
        } catch (e: IllegalArgumentException) {
            SyncStatus.PENDING_UPLOAD
        }
    }
}