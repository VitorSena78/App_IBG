package com.example.projeto_ibg3.domain.repository

import com.example.projeto_ibg3.domain.model.SyncProgress
import com.example.projeto_ibg3.domain.model.SyncState
import kotlinx.coroutines.flow.Flow

interface SyncRepository {
    // Métodos existentes
    suspend fun syncAll(): Result<Unit>
    suspend fun syncPacientes(): Result<Unit>
    suspend fun syncEspecialidades(): Result<Unit> //mudar para Response<ApiResponse<List<EspecialidadeDto>>>
    suspend fun hasPendingChanges(): Boolean
    suspend fun getLastSyncTimestamp(): Long
    suspend fun updateLastSyncTimestamp(timestamp: Long)

    // Novos métodos para observar estado
    val syncState: Flow<SyncState>
    val syncProgress: Flow<SyncProgress>

    // Método para iniciar sincronização com observabilidade
    suspend fun startSync(): Flow<SyncState>
    suspend fun startSyncPacientes(): Flow<SyncState>
    suspend fun startSyncEspecialidades(): Flow<SyncState>

    // Limpar erros
    fun clearError()

    suspend fun syncPacienteEspecialidadesOnly(): Result<Unit>
    suspend fun uploadPacienteEspecialidadesPending(): Result<Unit>
    suspend fun downloadPacienteEspecialidadesUpdated(): Result<Unit>
}