package com.example.projeto_ibg3.domain.repository

import com.example.projeto_ibg3.domain.model.Especialidade
import com.example.projeto_ibg3.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

interface EspecialidadeRepository {
    // Operações básicas
    fun getAllEspecialidades(): Flow<List<Especialidade>>
    suspend fun getEspecialidadeById(localId: String): Especialidade?
    suspend fun getEspecialidadeByServerId(serverId: Long): Especialidade?
    suspend fun getEspecialidadeByName(nome: String): Especialidade?
    suspend fun insertEspecialidade(especialidade: Especialidade): String
    suspend fun updateEspecialidade(especialidade: Especialidade)
    suspend fun deleteEspecialidade(localId: String)

    // Busca
    suspend fun searchEspecialidades(query: String): List<Especialidade>
    suspend fun getEspecialidadeCount(): Int

    // Sincronização
    suspend fun getPendingSyncItems(): List<Especialidade>
    suspend fun getPendingUploads(): List<Especialidade>
    suspend fun getPendingDeletions(): List<Especialidade>
    suspend fun markAsSynced(localId: String, serverId: Long)
    suspend fun markAsSyncFailed(localId: String, error: String)
    suspend fun updateSyncStatus(localId: String, status: SyncStatus)
    suspend fun getModifiedSince(timestamp: Long): List<Especialidade>
    suspend fun hasPendingChanges(): Boolean

    // Validação
    suspend fun especialidadeExists(nome: String, excludeId: String = ""): Boolean

    // Limpeza
    suspend fun cleanupOldDeletedRecords(cutoffTimestamp: Long)
    suspend fun retryFailedSync()
}
