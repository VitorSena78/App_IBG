package com.example.projeto_ibg3.sync.model

sealed class SyncResult {
    data class SUCCESS(
        val syncedCount: Int,
        val failedCount: Int = 0,
        val conflictCount: Int = 0,
        val syncTimestamp: Long = System.currentTimeMillis(),
        val message: String? = null
    ) : SyncResult()

    data class ERROR(
        val error: SyncError,
        val partialSuccess: Boolean = false,
        val syncedCount: Int = 0,
        val failedCount: Int = 0
    ) : SyncResult()


    object NO_NETWORK : SyncResult()
    object InProgress : SyncResult()
}