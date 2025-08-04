package com.example.projeto_ibg3.sync.worker

import com.example.projeto_ibg3.data.remote.conflict.ConflictResolution

data class SyncConfig(
    // Configurações de retry
    val maxRetries: Int = 3,
    val retryDelayMs: Long = 5000,
    val retryBackoffMultiplier: Float = 2.0f,

    // Configurações de batch
    val batchSize: Int = 50,
    val maxBatchSize: Int = 100,

    // Configurações de timeout
    val timeoutMs: Long = 30000,
    val connectionTimeoutMs: Long = 10000,
    val readTimeoutMs: Long = 20000,

    // Configurações de sincronização automática
    val autoSyncEnabled: Boolean = true,
    val syncIntervalMs: Long = 300000, // 5 minutos
    val syncOnAppStart: Boolean = true,
    val syncOnNetworkReconnect: Boolean = true,

    // Configurações de resolução de conflitos
    val conflictResolution: ConflictResolution = ConflictResolution.KEEP_LOCAL,
    val autoResolveConflicts: Boolean = false,

    // Configurações de limpeza
    val cleanupFailedItemsAfterMs: Long = 86400000, // 24 horas
    val maxFailedItemsToKeep: Int = 100,

    // Configurações de rede
    val requireWifiForSync: Boolean = false,
    val requireChargingForSync: Boolean = false,
    val syncOnlyWhenIdle: Boolean = false,

    // Configurações de debug
    val enableDebugLogs: Boolean = false,
    val logSyncDuration: Boolean = true,

    // Configurações de cache
    val cacheResponsesForOfflineMs: Long = 3600000, // 1 hora
    val enableOfflineMode: Boolean = true
) {
    fun getRetryDelay(attemptNumber: Int): Long {
        return (retryDelayMs * Math.pow(retryBackoffMultiplier.toDouble(), attemptNumber.toDouble())).toLong()
    }

    fun validate(): SyncConfig {
        require(maxRetries >= 0) { "maxRetries deve ser >= 0" }
        require(retryDelayMs > 0) { "retryDelayMs deve ser > 0" }
        require(batchSize > 0) { "batchSize deve ser > 0" }
        require(batchSize <= maxBatchSize) { "batchSize deve ser <= maxBatchSize" }
        require(timeoutMs > 0) { "timeoutMs deve ser > 0" }
        require(syncIntervalMs > 0) { "syncIntervalMs deve ser > 0" }

        return this
    }

    companion object {
        fun createDefault() = SyncConfig()

        fun createAggressive() = SyncConfig(
            maxRetries = 5,
            retryDelayMs = 2000,
            syncIntervalMs = 60000, // 1 minuto
            autoSyncEnabled = true,
            syncOnAppStart = true,
            syncOnNetworkReconnect = true
        )

        fun createConservative() = SyncConfig(
            maxRetries = 2,
            retryDelayMs = 10000,
            syncIntervalMs = 900000, // 15 minutos
            requireWifiForSync = true,
            requireChargingForSync = true,
            syncOnlyWhenIdle = true
        )
    }
}