package com.example.projeto_ibg3.data.dao

import androidx.room.*
import com.example.projeto_ibg3.data.entity.SyncMetadataEntity


@Dao
interface SyncMetadataDao {

    @Query("SELECT * FROM sync_metadata WHERE key = :key")
    suspend fun getMetadata(key: String): SyncMetadataEntity?

    @Query("SELECT value FROM sync_metadata WHERE key = :key")
    suspend fun getValue(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(metadata: SyncMetadataEntity)

    @Query("DELETE FROM sync_metadata WHERE key = :key")
    suspend fun deleteByKey(key: String)

    @Query("SELECT * FROM sync_metadata")
    suspend fun getAllMetadata(): List<SyncMetadataEntity>



    // Métodos auxiliares para sincronização
    suspend fun getLastSyncTimestamp(): Long {
        return getMetadata("last_sync")?.value?.toLongOrNull() ?: 0L
    }

    suspend fun updateLastSyncTimestamp(timestamp: Long) {
        insertOrUpdate(SyncMetadataEntity("last_sync", timestamp.toString()))
    }

    suspend fun getLastPatientSyncTimestamp(): Long {
        return getMetadata("last_patient_sync")?.value?.toLongOrNull() ?: 0L
    }

    suspend fun updateLastPatientSyncTimestamp(timestamp: Long) {
        insertOrUpdate(SyncMetadataEntity("last_patient_sync", timestamp.toString()))
    }

    suspend fun getSyncBatchSize(): Int {
        return getMetadata("sync_batch_size")?.value?.toIntOrNull() ?: 50
    }

    suspend fun updateSyncBatchSize(size: Int) {
        insertOrUpdate(SyncMetadataEntity("sync_batch_size", size.toString()))
    }

    suspend fun isSyncInProgress(): Boolean {
        return getMetadata("sync_in_progress")?.value?.toBoolean() ?: false
    }

    suspend fun setSyncInProgress(inProgress: Boolean) {
        insertOrUpdate(SyncMetadataEntity("sync_in_progress", inProgress.toString()))
    }
}