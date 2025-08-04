package com.example.projeto_ibg3.data.local.database.dao

import androidx.room.*
import com.example.projeto_ibg3.data.local.database.entities.SyncQueue

@Dao
interface SyncQueueDao {

    @Query("SELECT * FROM sync_queue WHERE syncStatus = 'PENDING' ORDER BY timestamp ASC")
    suspend fun getAllPendingItems(): List<SyncQueue>

    @Query("SELECT * FROM sync_queue WHERE entityType = :entityType AND syncStatus = 'PENDING'")
    suspend fun getPendingItemsByType(entityType: String): List<SyncQueue>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: SyncQueue)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<SyncQueue>)

    @Update
    suspend fun updateItem(item: SyncQueue)

    @Delete
    suspend fun deleteItem(item: SyncQueue)

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun deleteItemById(id: String)

    @Query("DELETE FROM sync_queue WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun deleteItemByEntity(entityType: String, entityId: String)

    @Query("DELETE FROM sync_queue WHERE syncStatus = 'SYNCED'")
    suspend fun deleteSyncedItems()

    @Query("SELECT COUNT(*) FROM sync_queue WHERE syncStatus = 'PENDING'")
    suspend fun getPendingItemsCount(): Int

    @Query("SELECT * FROM sync_queue WHERE entityId = :entityId AND entityType = :entityType")
    suspend fun getItemByEntity(entityId: String, entityType: String): SyncQueue?

    @Query("UPDATE sync_queue SET syncStatus = 'SYNCED' WHERE id = :id")
    suspend fun markAsSynced(id: String)

    @Query("UPDATE sync_queue SET syncStatus = 'ERROR', errorMessage = :errorMessage, retryCount = retryCount + 1 WHERE id = :id")
    suspend fun markAsError(id: String, errorMessage: String)

    @Query("DELETE FROM sync_queue")
    suspend fun deleteAllItems()
}