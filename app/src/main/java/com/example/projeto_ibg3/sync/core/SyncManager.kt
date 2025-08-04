package com.example.projeto_ibg3.sync.core

import com.example.projeto_ibg3.sync.model.*
import com.example.projeto_ibg3.sync.strategy.*
import com.example.projeto_ibg3.sync.utils.ExponentialBackoffRetry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Singleton
class SyncManager @Inject constructor(
    private val networkChecker: NetworkChecker,
    private val syncStateManager: SyncStateManager,
    private val conflictResolver: ConflictResolver,
    private val especialidadeSyncStrategy: EspecialidadeSyncStrategy,
    //private val pacienteSyncStrategy: PacienteSyncStrategy,
    //private val pacienteEspecialidadeSyncStrategy: PacienteEspecialidadeSyncStrategy,
    private val retryHandler: ExponentialBackoffRetry
) {

    companion object {
        private const val TAG = "SyncManager"
        private const val SYNC_TIMEOUT_SECONDS = 30
        private const val AUTO_SYNC_INTERVAL_MS = 60_000L // 1 minuto
    }

    // Estado da sincronização
    private val _syncPhase = MutableStateFlow(SyncPhase.IDLE)
    val syncPhase: StateFlow<SyncPhase> = _syncPhase.asStateFlow()

    private val _syncStats = MutableStateFlow(SyncStats())
    val syncStats: StateFlow<SyncStats> = _syncStats.asStateFlow()

    // Escopo para operações de sincronização
    private val syncScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    /**
     * Sincronização completa - executa todas as entidades
     */
    suspend fun performFullSync(): SyncResult {
        if (syncStateManager.isSyncInProgress()) {
            return SyncResult.ERROR(SyncError.UnknownError("Sincronização já em andamento"))
        }

        return withTimeout(SYNC_TIMEOUT_SECONDS.seconds) {
            retryHandler.execute {
                executeFullSyncInternal()
            }
        }
    }

    /**
     * Sincronização rápida - apenas itens modificados
     */
    suspend fun performQuickSync(): SyncResult {
        if (!networkChecker.isNetworkAvailable()) {
            return SyncResult.NO_NETWORK
        }

        return try {
            _syncPhase.value = SyncPhase.SYNCING

            val strategies = getSyncStrategies()
            var totalSynced = 0
            var totalFailed = 0

            strategies.forEach { strategy ->
                when (val result = strategy.syncQuick()) {
                    is SyncResult.SUCCESS -> {
                        totalSynced += result.syncedCount
                        totalFailed += result.failedCount
                    }
                    is SyncResult.ERROR -> {
                        totalFailed += result.failedCount
                    }
                    else -> { /* NO_NETWORK já tratado */ }
                }
            }

            if (totalFailed == 0) {
                syncStateManager.updateLastSyncTimestamp()
                SyncResult.SUCCESS(totalSynced, totalFailed, message = "Sincronização rápida concluída")
            } else {
                SyncResult.SUCCESS(totalSynced, totalFailed, message = "Sincronização parcial")
            }

        } catch (e: Exception) {
            SyncResult.ERROR(SyncError.UnknownError("Erro na sincronização rápida", e))
        } finally {
            _syncPhase.value = SyncPhase.IDLE
        }
    }

    /**
     * Sincronização automática em background
     */
    fun startAutoSync(): Flow<SyncResult> = flow {
        while (currentCoroutineContext().isActive) {
            if (networkChecker.isNetworkAvailable() && hasPendingChanges()) {
                emit(performQuickSync())
            }
            delay(AUTO_SYNC_INTERVAL_MS)
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Obter estatísticas de sincronização
     */
    suspend fun getSyncStatistics(): SyncStats {
        return try {
            val stats = mutableMapOf<String, Int>()

            // Coletar estatísticas de cada estratégia
            getSyncStrategies().forEach { strategy ->
                val entityStats = strategy.getStats()
                stats.putAll(entityStats)
            }

            SyncStats(
                totalItems = stats.values.sum(),
                syncedItems = stats["synced"] ?: 0,
                pendingUpload = stats["pending_upload"] ?: 0,
                pendingDelete = stats["pending_delete"] ?: 0,
                failedItems = stats["failed"] ?: 0,
                conflictItems = stats["conflicts"] ?: 0,
                lastSyncTimestamp = syncStateManager.getLastSyncTimestamp(),
                isSyncInProgress = syncStateManager.isSyncInProgress()
            )
        } catch (e: Exception) {
            SyncStats() // Retorna stats vazio em caso de erro
        }
    }

    /**
     * Forçar resolução de conflitos
     */
    suspend fun resolveAllConflicts(): SyncResult {
        return try {
            _syncPhase.value = SyncPhase.RESOLVING_CONFLICTS

            val strategies = getSyncStrategies()
            var totalResolved = 0

            strategies.forEach { strategy ->
                val result = strategy.resolveConflicts()
                if (result is SyncResult.SUCCESS) {
                    totalResolved += result.syncedCount
                }
            }

            SyncResult.SUCCESS(totalResolved, message = "Conflitos resolvidos")

        } catch (e: Exception) {
            SyncResult.ERROR(SyncError.UnknownError("Erro ao resolver conflitos", e))
        } finally {
            _syncPhase.value = SyncPhase.IDLE
        }
    }

    /**
     * Cancelar sincronização em andamento
     */
    fun cancelSync() {
        syncScope.coroutineContext.cancelChildren()
        syncStateManager.setSyncInProgress(false)
        _syncPhase.value = SyncPhase.IDLE
    }

    // Métodos privados

    private suspend fun executeFullSyncInternal(): SyncResult {
        if (!networkChecker.isNetworkAvailable()) {
            return SyncResult.NO_NETWORK
        }

        return try {
            syncStateManager.setSyncInProgress(true)
            _syncPhase.value = SyncPhase.SYNCING

            val startTime = System.currentTimeMillis()
            val strategies = getSyncStrategies()
            val results = mutableListOf<SyncResult>()

            // Executar estratégias em ordem de dependência
            strategies.forEach { strategy ->
                val result = strategy.sync()
                results.add(result)

                // Parar se houver erro crítico
                if (result is SyncResult.ERROR && !result.partialSuccess) {
                    return result
                }
            }

            // Consolidar resultados
            val totalSynced = results.sumOf {
                when (it) {
                    is SyncResult.SUCCESS -> it.syncedCount
                    is SyncResult.ERROR -> it.syncedCount
                    else -> 0
                }
            }

            val totalFailed = results.sumOf {
                when (it) {
                    is SyncResult.SUCCESS -> it.failedCount
                    is SyncResult.ERROR -> it.failedCount
                    else -> 0
                }
            }

            val totalConflicts = results.sumOf {
                when (it) {
                    is SyncResult.SUCCESS -> it.conflictCount
                    else -> 0
                }
            }

            val duration = System.currentTimeMillis() - startTime

            if (totalFailed == 0) {
                syncStateManager.updateLastSyncTimestamp()
                _syncPhase.value = SyncPhase.COMPLETED
                SyncResult.SUCCESS(
                    syncedCount = totalSynced,
                    failedCount = totalFailed,
                    conflictCount = totalConflicts,
                    message = "Sincronização completa concluída em ${duration}ms"
                )
            } else {
                SyncResult.SUCCESS(
                    syncedCount = totalSynced,
                    failedCount = totalFailed,
                    conflictCount = totalConflicts,
                    message = "Sincronização parcial - $totalFailed falhas"
                )
            }

        } catch (e: Exception) {
            _syncPhase.value = SyncPhase.ERROR
            SyncResult.ERROR(SyncError.UnknownError("Erro na sincronização completa", e))
        } finally {
            syncStateManager.setSyncInProgress(false)
            _syncPhase.value = SyncPhase.IDLE
        }
    }

    private fun getSyncStrategies(): List<SyncStrategy> {
        // Ordem de dependência: Especialidades -> Pacientes -> PacienteEspecialidade
        return listOf(
            especialidadeSyncStrategy,
            //pacienteSyncStrategy,
            //pacienteEspecialidadeSyncStrategy
        )
    }

    private suspend fun hasPendingChanges(): Boolean {
        return try {
            getSyncStrategies().any { it.hasPendingChanges() }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Limpar dados de sincronização (usar com cuidado)
     */
    suspend fun resetSyncData() {
        syncStateManager.setSyncInProgress(false)
        _syncPhase.value = SyncPhase.IDLE
        _syncStats.value = SyncStats()
    }

    /**
     * Obter informações de debug
     */
    fun getDebugInfo(): Map<String, Any> {
        return mapOf(
            "syncPhase" to _syncPhase.value,
            "lastSyncTimestamp" to syncStateManager.getLastSyncTimestamp(),
            "isSyncInProgress" to syncStateManager.isSyncInProgress(),
            "networkAvailable" to networkChecker.isNetworkAvailable(),
            "deviceId" to syncStateManager.getDeviceId()
        )
    }
}