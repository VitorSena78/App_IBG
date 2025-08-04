package com.example.projeto_ibg3.core.extensions

import com.example.projeto_ibg3.data.local.database.dao.SyncMetadataDao
import com.example.projeto_ibg3.data.local.database.entities.SyncMetadataEntity
import com.example.projeto_ibg3.domain.model.SyncAction

// Extensões para operações comuns
suspend fun SyncMetadataDao.addOrUpdateSyncItem(
    entityType: String,
    entityId: String,
    action: SyncAction,
    jsonData: String,
    priority: Int = 1,
    deviceId: String
) {
    // Remove item existente se houver
    deleteItemsByEntityId(entityId, entityType)

    // Adiciona novo item
    val newItem = SyncMetadataEntity(
        entityType = entityType,
        entityId = entityId,
        action = action,
        jsonData = jsonData,
        priority = priority,
        deviceId = deviceId
    )
    insertItem(newItem)
}

suspend fun SyncMetadataDao.incrementAttempts(item: SyncMetadataEntity, errorMessage: String? = null) {
    val updatedItem = item.incrementAttempts(errorMessage)
    updateItem(updatedItem)
}

suspend fun SyncMetadataDao.cleanupDatabase() {
    deleteFailedItems()
    cleanupOldFailedItems()
}