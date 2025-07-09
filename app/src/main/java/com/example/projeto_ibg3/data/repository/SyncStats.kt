package com.example.projeto_ibg3.data.repository

data class SyncStats(
    val pendingSync: Int,
    val failedSync: Int,
    val conflicts: Int,
    val syncing: Int,
    val lastSyncDate: Long?
)