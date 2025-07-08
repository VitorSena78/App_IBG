package com.example.projeto_ibg3.data.repository

import com.example.projeto_ibg3.data.dao.PacienteDao
import com.example.projeto_ibg3.data.dao.SyncMetadataDao
import com.example.projeto_ibg3.data.entity.PacienteEntity
import com.example.projeto_ibg3.data.entity.toEntity
import com.example.projeto_ibg3.data.entity.toPaciente
import com.example.projeto_ibg3.data.entity.updateFrom
import com.example.projeto_ibg3.data.remote.PacienteApiService
import com.example.projeto_ibg3.model.Paciente
import com.example.projeto_ibg3.model.SyncStatus
import com.example.projeto_ibg3.utils.NetworkUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PacienteRepository @Inject constructor(
    private val localDao: PacienteDao,
    private val syncMetadataDao: SyncMetadataDao,
    private val apiService: PacienteApiService,
    private val networkUtils: NetworkUtils
) {

    // FONTE ÚNICA DA VERDADE: Sempre retorna dados do banco local
    fun getAllPacientes(): Flow<List<Paciente>> {
        return localDao.getAllPacientes().map { entities ->
            entities.map { it.toPaciente() }
        }
    }

    // Busca por ID - sempre do local primeiro
    suspend fun getPacienteById(id: Long): Paciente? {
        return localDao.getPacienteById(id)?.toPaciente()
    }

    // Busca por nome (local primeiro, depois remoto se necessário)
    fun searchPacientes(nome: String): Flow<List<Paciente>> {
        return localDao.searchPacientes("%$nome%").map { entities ->
            entities.map { it.toPaciente() }
        }
    }

    // OPERAÇÕES OFFLINE-FIRST

    // INSERIR: Salva local primeiro, depois sincroniza
    suspend fun insertPaciente(paciente: Paciente): Result<Long> {
        return try {
            val entity = paciente.toEntity()
            val localId = localDao.insertPaciente(entity)

            // Tenta sincronizar imediatamente se houver internet
            if (networkUtils.isNetworkAvailable()) {
                syncPacienteToServer(localId)
            }

            Result.success(localId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ATUALIZAR: Atualiza local primeiro, depois sincroniza
    suspend fun updatePaciente(paciente: Paciente): Result<Unit> {
        return try {
            val existingEntity = localDao.getPacienteById(paciente.id)
            if (existingEntity != null) {
                val updatedEntity = existingEntity.updateFrom(paciente)
                localDao.updatePaciente(updatedEntity)

                // Tenta sincronizar se houver internet
                if (networkUtils.isNetworkAvailable()) {
                    syncPacienteToServer(updatedEntity.id)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // DELETAR: Marca como deletado localmente, depois sincroniza
    suspend fun deletePaciente(id: Long): Result<Unit> {
        return try {
            localDao.markAsDeleted(
                id = id,
                status = SyncStatus.PENDING_DELETION,
                timestamp = System.currentTimeMillis()
            )

            // Tenta sincronizar se houver internet
            if (networkUtils.isNetworkAvailable()) {
                syncDeletionToServer(id)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // SINCRONIZAÇÃO OTIMIZADA

    // Sincroniza dados do servidor para o local (Download)
    suspend fun syncFromServer(): Result<Unit> {
        if (!networkUtils.isNetworkAvailable()) {
            return Result.failure(Exception("Sem conexão com a internet"))
        }

        return try {
            val lastSync = syncMetadataDao.getLastPatientSyncTimestamp()
            val response = apiService.getUpdatedPacientes(lastSync)

            if (response.isSuccessful) {
                val serverPacientes = response.body() ?: emptyList()

                // Processa dados do servidor
                serverPacientes.forEach { serverPaciente ->
                    val entity = serverPaciente.toEntity().copy(
                        syncStatus = SyncStatus.SYNCED
                    )
                    localDao.insertOrUpdatePaciente(entity)
                }

                syncMetadataDao.updateLastPatientSyncTimestamp(System.currentTimeMillis())
                Result.success(Unit)
            } else {
                Result.failure(Exception("Erro ao sincronizar: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Sincroniza dados locais para o servidor (Upload) - OTIMIZADO
    suspend fun syncToServer(): Result<Unit> {
        if (!networkUtils.isNetworkAvailable()) {
            return Result.failure(Exception("Sem conexão com a internet"))
        }

        return try {
            val pendingPacientes = localDao.getPendingSync()
            val pendingDeletions = localDao.getPendingDeletions()

            // Sincronização em lote (mais eficiente)
            if (pendingPacientes.isNotEmpty()) {
                syncPacientesBatch(pendingPacientes)
            }

            if (pendingDeletions.isNotEmpty()) {
                syncDeletionsBatch(pendingDeletions)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // MÉTODOS AUXILIARES OTIMIZADOS

    private suspend fun syncPacientesBatch(entities: List<PacienteEntity>) {
        try {
            val pacientesToCreate = entities.filter { it.serverId == null }
            val pacientesToUpdate = entities.filter { it.serverId != null }

            // Cria novos pacientes em lote
            if (pacientesToCreate.isNotEmpty()) {
                val pacientes = pacientesToCreate.map { it.toPaciente() }
                val response = apiService.createPacientesBatch(pacientes)

                if (response.isSuccessful) {
                    val serverPacientes = response.body() ?: emptyList()

                    // Atualiza com IDs do servidor
                    serverPacientes.forEachIndexed { index, serverPaciente ->
                        val localEntity = pacientesToCreate[index]
                        localDao.updatePaciente(
                            localEntity.copy(
                                serverId = serverPaciente.id,
                                syncStatus = SyncStatus.SYNCED
                            )
                        )
                    }
                }
            }

            // Atualiza pacientes existentes em lote
            if (pacientesToUpdate.isNotEmpty()) {
                val pacientes = pacientesToUpdate.map { it.toPaciente() }
                val response = apiService.updatePacientesBatch(pacientes)

                if (response.isSuccessful) {
                    pacientesToUpdate.forEach { entity ->
                        localDao.updatePaciente(
                            entity.copy(syncStatus = SyncStatus.SYNCED)
                        )
                    }
                }
            }
        } catch (e: Exception) {
            println("Erro ao sincronizar lote: ${e.message}")
        }
    }

    private suspend fun syncDeletionsBatch(entities: List<PacienteEntity>) {
        try {
            val serverIds = entities.mapNotNull { it.serverId }
            if (serverIds.isNotEmpty()) {
                val response = apiService.deletePacientesBatch(serverIds)

                if (response.isSuccessful) {
                    entities.forEach { entity ->
                        localDao.deletePacientePermanently(entity.id)
                    }
                }
            }
        } catch (e: Exception) {
            println("Erro ao sincronizar deleções: ${e.message}")
        }
    }

    // Fallback para sincronização individual (caso o servidor não suporte batch)
    private suspend fun syncPacienteToServer(localId: Long) {
        try {
            val localEntity = localDao.getPacienteById(localId)
            if (localEntity != null) {
                val paciente = localEntity.toPaciente()

                val response = if (localEntity.serverId != null) {
                    apiService.updatePaciente(localEntity.serverId, paciente)
                } else {
                    apiService.createPaciente(paciente)
                }

                if (response.isSuccessful) {
                    val serverPaciente = response.body()
                    if (serverPaciente != null) {
                        localDao.updatePaciente(
                            localEntity.copy(
                                serverId = serverPaciente.id,
                                syncStatus = SyncStatus.SYNCED
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            println("Erro ao sincronizar paciente $localId: ${e.message}")
        }
    }

    private suspend fun syncDeletionToServer(localId: Long) {
        try {
            val localEntity = localDao.getPacienteById(localId)
            if (localEntity?.serverId != null) {
                val response = apiService.deletePaciente(localEntity.serverId)
                if (response.isSuccessful) {
                    localDao.deletePacientePermanently(localId)
                }
            }
        } catch (e: Exception) {
            println("Erro ao sincronizar deleção $localId: ${e.message}")
        }
    }

    // SINCRONIZAÇÃO COMPLETA
    suspend fun performFullSync(): Result<Unit> {
        return try {
            // Marca sincronização como em progresso
            syncMetadataDao.setSyncInProgress(true)

            // Primeiro upload dos dados locais
            syncToServer().getOrThrow()

            // Depois download dos dados do servidor
            syncFromServer().getOrThrow()

            // Marca sincronização como concluída
            syncMetadataDao.setSyncInProgress(false)

            Result.success(Unit)
        } catch (e: Exception) {
            syncMetadataDao.setSyncInProgress(false)
            Result.failure(e)
        }
    }

    // MÉTODOS UTILITÁRIOS

    suspend fun getPendingSyncCount(): Int {
        return localDao.getPendingSyncCount()
    }

    suspend fun hasPendingSync(): Boolean {
        return getPendingSyncCount() > 0
    }

    suspend fun getLastSyncDate(): Long? {
        val timestamp = syncMetadataDao.getLastPatientSyncTimestamp()
        return timestamp.takeIf { it > 0 }
    }

    suspend fun isSyncInProgress(): Boolean {
        return syncMetadataDao.isSyncInProgress()
    }
}