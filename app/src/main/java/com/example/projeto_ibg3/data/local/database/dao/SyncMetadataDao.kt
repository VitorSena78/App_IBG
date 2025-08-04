package com.example.projeto_ibg3.data.local.database.dao

import androidx.room.*
import com.example.projeto_ibg3.data.local.database.entities.SyncMetadataEntity
import com.example.projeto_ibg3.domain.model.SyncAction

@Dao
interface SyncMetadataDao {

    @Query("SELECT * FROM sync_metadata ORDER BY priority DESC, created_at ASC")
    suspend fun getAllPendingItems(): List<SyncMetadataEntity>

    @Query("SELECT * FROM sync_metadata WHERE entity_type = :entityType ORDER BY priority DESC, created_at ASC")
    suspend fun getPendingItemsByType(entityType: String): List<SyncMetadataEntity>

    @Query("SELECT * FROM sync_metadata WHERE action = :action ORDER BY priority DESC, created_at ASC")
    suspend fun getItemsByAction(action: SyncAction): List<SyncMetadataEntity>

    @Query("SELECT * FROM sync_metadata WHERE entity_id = :entityId AND entity_type = :entityType")
    suspend fun getItemByEntityId(entityId: String, entityType: String): SyncMetadataEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: SyncMetadataEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<SyncMetadataEntity>)

    @Update
    suspend fun updateItem(item: SyncMetadataEntity)

    @Delete
    suspend fun deleteItem(item: SyncMetadataEntity)

    @Query("DELETE FROM sync_metadata WHERE entity_id = :entityId AND entity_type = :entityType")
    suspend fun deleteItemsByEntityId(entityId: String, entityType: String)

    @Query("DELETE FROM sync_metadata WHERE attempts >= max_attempts")
    suspend fun deleteFailedItems()

    @Query("DELETE FROM sync_metadata WHERE created_at < :timestamp")
    suspend fun deleteOldItems(timestamp: Long)

    @Query("DELETE FROM sync_metadata")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM sync_metadata")
    suspend fun getPendingItemsCount(): Int

    @Query("SELECT COUNT(*) FROM sync_metadata WHERE entity_type = :entityType")
    suspend fun getPendingItemsCountByType(entityType: String): Int

    @Query("SELECT COUNT(*) FROM sync_metadata WHERE action = :action")
    suspend fun getItemsCountByAction(action: SyncAction): Int

    // Métodos para limpeza automática
    @Query("DELETE FROM sync_metadata WHERE created_at < :maxAge AND attempts >= max_attempts")
    suspend fun cleanupOldFailedItems(maxAge: Long = System.currentTimeMillis() - 86400000) // 24h

    // Método para controlar se sync está em progresso
    @Query("INSERT OR REPLACE INTO sync_metadata_config (key, value) VALUES ('sync_in_progress', CASE WHEN :inProgress THEN '1' ELSE '0' END)")
    suspend fun setSyncInProgress(inProgress: Boolean)

    // Corrigir o tipo de retorno (era Long, mas o valor é String)
    @Query("SELECT CAST(COALESCE(value, '0') AS INTEGER) FROM sync_metadata_config WHERE key = 'last_patient_sync' LIMIT 1")
    suspend fun getLastPatientSyncTimestamp(): Long

    // Converter o timestamp para String
    @Query("INSERT OR REPLACE INTO sync_metadata_config (key, value) VALUES ('last_patient_sync', CAST(:timestamp AS TEXT))")
    suspend fun updateLastPatientSyncTimestamp(timestamp: Long)
}

