package com.example.projeto_ibg3.domain.repository

import com.example.projeto_ibg3.data.local.database.entities.EspecialidadeEntity
import com.example.projeto_ibg3.domain.model.Especialidade
import com.example.projeto_ibg3.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

interface EspecialidadeRepository {
    // Operações básicas
    fun getAllEspecialidades(): Flow<List<EspecialidadeEntity>>
    suspend fun getEspecialidadeById(localId: String): EspecialidadeEntity?
    suspend fun getEspecialidadeByServerId(serverId: Long?): EspecialidadeEntity?
    suspend fun getEspecialidadeByName(nome: String): EspecialidadeEntity?
    suspend fun insertEspecialidade(especialidade: Especialidade): String
    suspend fun updateEspecialidade(especialidade: Especialidade)
    suspend fun deleteEspecialidade(localId: String)

    // Busca
    suspend fun searchEspecialidades(query: String): List<Especialidade>
    suspend fun getEspecialidadeCount(): Int

    // Sincronização
    suspend fun getPendingSyncItems(): List<EspecialidadeEntity>
    suspend fun getPendingUploads(): List<EspecialidadeEntity>
    suspend fun getPendingDeletions(): List<EspecialidadeEntity>
    suspend fun markAsSynced(localId: String, serverId: Long)
    suspend fun markAsSyncFailed(localId: String, error: String)
    suspend fun updateSyncStatus(localId: String, status: SyncStatus)
    suspend fun getModifiedSince(timestamp: Long): List<EspecialidadeEntity>
    suspend fun hasPendingChanges(): Boolean

    // Validação
    suspend fun especialidadeExists(nome: String, excludeId: String = ""): Boolean

    // Limpeza
    suspend fun cleanupOldDeletedRecords(cutoffTimestamp: Long)
    suspend fun retryFailedSync()
}
