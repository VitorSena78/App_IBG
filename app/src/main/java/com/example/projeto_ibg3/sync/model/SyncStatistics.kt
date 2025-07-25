package com.example.projeto_ibg3.sync.model

data class SyncStatistics(
    val totalSynced: Int,
    val uploaded: Int,
    val downloaded: Int,
    val conflicts: Int,
    val failed: Int = 0,
    val duration: Long,

    val total: Int = 0,
    val synced: Int = 0,
    val pending: Int = 0
)
