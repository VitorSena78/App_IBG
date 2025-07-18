package com.example.projeto_ibg3.sync.model

import com.example.projeto_ibg3.domain.model.SyncStatus

data class SyncStats(
    val totalItems: Int = 0,
    val syncedItems: Int = 0,
    val pendingUpload: Int = 0,
    val pendingDelete: Int = 0,
    val failedItems: Int = 0,
    val conflictItems: Int = 0,
    val syncingItems: Int = 0,
    val lastSyncTimestamp: Long? = null,
    val lastSuccessfulSyncTimestamp: Long? = null,
    val nextScheduledSync: Long? = null,
    val isSyncInProgress: Boolean = false,
    val failedSyncAttempts: Int = 0,
    val avgSyncDuration: Long = 0, // em ms
    val lastSyncDuration: Long = 0, // em ms
    val totalSyncsSinceInstall: Int = 0
) {
    // Propriedades calculadas para compatibilidade
    val pendingSync: Int get() = pendingUpload + pendingDelete
    val failedSync: Int get() = failedItems
    val conflicts: Int get() = conflictItems
    val syncing: Int get() = syncingItems
    val lastSyncDate: Long? get() = lastSyncTimestamp

    // Métodos úteis
    fun getSyncProgress(): Float {
        return if (totalItems > 0) {
            syncedItems.toFloat() / totalItems.toFloat()
        } else {
            0f
        }
    }

    fun hasFailures(): Boolean = failedItems > 0

    fun hasConflicts(): Boolean = conflictItems > 0

    fun needsSync(): Boolean = pendingSync > 0

    fun isHealthy(): Boolean = !hasFailures() && !hasConflicts()

    fun getSyncStatusSummary(): String {
        return when {
            isSyncInProgress -> "Sincronizando..."
            hasConflicts() -> "Conflitos detectados ($conflictItems)"
            hasFailures() -> "Falhas na sincronização ($failedItems)"
            needsSync() -> "Pendente ($pendingSync itens)"
            else -> "Sincronizado"
        }
    }

    fun getDetailedSummary(): String {
        return buildString {
            append("Total: $totalItems")
            if (syncedItems > 0) append(", Sincronizados: $syncedItems")
            if (pendingUpload > 0) append(", Pendente upload: $pendingUpload")
            if (pendingDelete > 0) append(", Pendente delete: $pendingDelete")
            if (failedItems > 0) append(", Falhas: $failedItems")
            if (conflictItems > 0) append(", Conflitos: $conflictItems")
            if (syncingItems > 0) append(", Sincronizando: $syncingItems")
        }
    }
}

// Extensões para criar SyncStats a partir de diferentes fontes
fun Map<SyncStatus, Int>.toSyncStats(
    lastSyncTimestamp: Long? = null,
    isSyncInProgress: Boolean = false
): SyncStats {
    return SyncStats(
        totalItems = values.sum(),
        syncedItems = get(SyncStatus.SYNCED) ?: 0,
        pendingUpload = get(SyncStatus.PENDING_UPLOAD) ?: 0,
        pendingDelete = get(SyncStatus.PENDING_DELETE) ?: 0,
        failedItems = (get(SyncStatus.UPLOAD_FAILED) ?: 0) + (get(SyncStatus.DELETE_FAILED) ?: 0),
        conflictItems = get(SyncStatus.CONFLICT) ?: 0,
        syncingItems = get(SyncStatus.SYNCING) ?: 0,
        lastSyncTimestamp = lastSyncTimestamp,
        isSyncInProgress = isSyncInProgress
    )
}