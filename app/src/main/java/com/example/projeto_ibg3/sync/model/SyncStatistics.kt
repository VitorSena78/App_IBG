package com.example.projeto_ibg3.sync.model

data class SyncStatistics(
    val totalSynced: Int,
    val uploaded: Int,
    val downloaded: Int,
    val conflicts: Int,
    val failed: Int,
    val duration: Long
)
