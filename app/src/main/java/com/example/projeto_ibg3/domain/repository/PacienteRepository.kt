package com.example.projeto_ibg3.domain.repository

import com.example.projeto_ibg3.domain.model.Paciente
import com.example.projeto_ibg3.domain.model.SyncStatus
import com.example.projeto_ibg3.sync.model.SyncStatistics
import kotlinx.coroutines.flow.Flow

interface PacienteRepository {

    // ==================== OPERÇÕES CRUD ====================

    fun getAllPacientes(): Flow<List<Paciente>>
    suspend fun getPacienteById(localId: String): Paciente?
    suspend fun insertPaciente(paciente: Paciente): String
    suspend fun updatePaciente(paciente: Paciente)
    suspend fun deletePaciente(localId: String)
    suspend fun restorePaciente(pacienteLocalId: String)

    // ==================== SINCRONIZAÇÃO ====================

    suspend fun syncPacientes(): Result<Unit>
    suspend fun getPacientesNaoSincronizados(): List<Paciente>
    suspend fun markAsSynced(localId: String, serverId: Long)
    suspend fun hasPendingChanges(): Boolean
    suspend fun retryFailedSync():  Result<Unit>
    suspend fun getFailedSyncPacientes(): List<Paciente>

    // --- Resolução de Conflitos ---
    suspend fun resolveConflictKeepLocal(pacienteLocalId: String)
    suspend fun resolveConflictKeepServer(pacienteLocalId: String)

    // --- Estatísticas de Sincronização ---
    fun getPendingSyncCount(): Flow<Int>
    fun getConflictCount(): Flow<Int>
    fun getPacientesByStatus(status: SyncStatus): Flow<List<Paciente>>
    fun getSyncStatistics(): Flow<SyncStatistics>

    // ==================== BUSCA E CONTAGEM ====================

    suspend fun searchPacientes(query: String): Flow<List<Paciente>>
    suspend fun getPacienteCount(): Int

    // ==================== VALIDAÇÃO ====================

    suspend fun pacienteExists(cpf: String, excludeId: String = ""): Boolean
}
