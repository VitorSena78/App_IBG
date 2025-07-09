
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
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PacienteRepository @Inject constructor(
    private val localDao: PacienteDao,
    private val syncMetadataDao: SyncMetadataDao,
    private val apiService: PacienteApiService,
    private val networkUtils: NetworkUtils
) {

    // Mutex para evitar sincronizações concorrentes
    private val syncMutex = Mutex()

    // Cache para controle de retry
    private val retryCache = mutableMapOf<Long, Int>()
    private val maxRetries = 3

    // FONTE ÚNICA DA VERDADE: Sempre retorna dados do banco local
    fun getAllPacientes(): Flow<List<Paciente>> {
        return localDao.getAllPacientes().map { entities ->
            entities.filter { !it.isDeleted }.map { it.toPaciente() }
        }
    }

    // Busca por ID - sempre do local primeiro
    suspend fun getPacienteById(id: Long): Paciente? {
        return localDao.getPacienteById(id)?.takeIf { !it.isDeleted }?.toPaciente()
    }

    // Busca por nome (local primeiro, depois remoto se necessário)
    fun searchPacientes(nome: String): Flow<List<Paciente>> {
        return localDao.searchPacientes("%$nome%").map { entities ->
            entities.filter { !it.isDeleted }.map { it.toPaciente() }
        }
    }

    // OPERAÇÕES OFFLINE-FIRST

    // INSERIR: Salva local primeiro, depois sincroniza
    suspend fun insertPaciente(paciente: Paciente): Result<Long> {
        return try {
            val entity = paciente.toEntity().copy(
                syncStatus = SyncStatus.PENDING_UPLOAD,
                lastModified = System.currentTimeMillis()
            )
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
            if (existingEntity != null && !existingEntity.isDeleted) {
                val updatedEntity = existingEntity.updateFrom(paciente).copy(
                    syncStatus = SyncStatus.PENDING_UPLOAD,
                    lastModified = System.currentTimeMillis(),
                    version = existingEntity.version + 1
                )
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
                status = SyncStatus.PENDING_DELETE,
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
    suspend fun syncFromServer(): Result<Unit> = syncMutex.withLock {
        if (!networkUtils.isNetworkAvailable()) {
            return Result.failure(Exception("Sem conexão com a internet"))
        }

        return try {
            // Marca sincronização como em progresso
            syncMetadataDao.setSyncInProgress(true)

            val lastSync = syncMetadataDao.getLastPatientSyncTimestamp()
            val response = apiService.getUpdatedPacientes(lastSync)

            if (response.isSuccessful) {
                val serverPacientes = response.body() ?: emptyList()

                // Processa dados do servidor com resolução de conflitos
                serverPacientes.forEach { serverPaciente ->
                    processServerPaciente(serverPaciente)
                }

                syncMetadataDao.updateLastPatientSyncTimestamp(System.currentTimeMillis())
                syncMetadataDao.setSyncInProgress(false)
                Result.success(Unit)
            } else {
                syncMetadataDao.setSyncInProgress(false)
                Result.failure(Exception("Erro ao sincronizar: ${response.message()}"))
            }
        } catch (e: Exception) {
            syncMetadataDao.setSyncInProgress(false)
            Result.failure(e)
        }
    }

    // Processa dados do servidor com resolução de conflitos
    private suspend fun processServerPaciente(serverPaciente: Paciente) {
        val localEntity = localDao.getPacienteByServerId(serverPaciente.id)

        if (localEntity == null) {
            // Novo paciente do servidor
            val entity = serverPaciente.toEntity().copy(
                syncStatus = SyncStatus.SYNCED,
                serverId = serverPaciente.id
            )
            localDao.insertPaciente(entity)
        } else {
            // Verifica conflitos
            when {
                localEntity.syncStatus == SyncStatus.SYNCED -> {
                    // Sem conflito, atualiza normalmente
                    val updatedEntity = localEntity.updateFrom(serverPaciente).copy(
                        syncStatus = SyncStatus.SYNCED
                    )
                    localDao.updatePaciente(updatedEntity)
                }

                localEntity.lastModified > System.currentTimeMillis() -> {
                    // Dados locais são mais recentes, mantém local
                    // Mas marca para sincronização
                    localDao.updatePaciente(
                        localEntity.copy(syncStatus = SyncStatus.PENDING_UPLOAD)
                    )
                }

                else -> {
                    // Conflito detectado - salva dados do servidor e marca conflito
                    val conflictEntity = localEntity.copy(
                        syncStatus = SyncStatus.CONFLICT,
                        conflictData = Gson().toJson(serverPaciente) // Serializa dados do servidor
                    )
                    localDao.updatePaciente(conflictEntity)
                }
            }
        }
    }

    // Sincroniza dados locais para o servidor (Upload) - OTIMIZADO
    suspend fun syncToServer(): Result<Unit> = syncMutex.withLock {
        if (!networkUtils.isNetworkAvailable()) {
            return Result.failure(Exception("Sem conexão com a internet"))
        }

        return try {
            val pendingPacientes = localDao.getPendingSync() // Busca PENDING_UPLOAD
            val pendingDeletions = localDao.getPendingDeletions() // Busca PENDING_DELETE
            val failedItems = localDao.getFailedItems() // Busca UPLOAD_FAILED e DELETE_FAILED

            // Sincronização em lote para uploads
            if (pendingPacientes.isNotEmpty()) {
                syncPacientesBatch(pendingPacientes)
            }

            // Sincronização em lote para deleções
            if (pendingDeletions.isNotEmpty()) {
                syncDeletionsBatch(pendingDeletions)
            }

            // Retry para itens com falha
            if (failedItems.isNotEmpty()) {
                retryFailedItems(failedItems)
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
                        // Remove do cache de retry
                        retryCache.remove(localEntity.id)
                    }
                } else {
                    // Marca falha e incrementa retry
                    handleSyncFailure(pacientesToCreate)
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
                        // Remove do cache de retry
                        retryCache.remove(entity.id)
                    }
                } else {
                    // Marca falha e incrementa retry
                    handleSyncFailure(pacientesToUpdate)
                }
            }
        } catch (e: Exception) {
            println("Erro ao sincronizar lote: ${e.message}")
            handleSyncFailure(entities)
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
                        retryCache.remove(entity.id)
                    }
                } else {
                    handleSyncFailure(entities)
                }
            } else {
                // Deleta localmente se não tem serverId
                entities.forEach { entity ->
                    localDao.deletePacientePermanently(entity.id)
                }
            }
        } catch (e: Exception) {
            println("Erro ao sincronizar deleções: ${e.message}")
            handleSyncFailure(entities)
        }
    }

    // Gerencia falhas de sincronização com retry
    private suspend fun handleSyncFailure(entities: List<PacienteEntity>) {
        entities.forEach { entity ->
            val retryCount = retryCache.getOrDefault(entity.id, 0) + 1

            if (retryCount >= maxRetries) {
                // Marca como erro após máximo de tentativas
                val failedStatus = when (entity.syncStatus) {
                    SyncStatus.PENDING_UPLOAD, SyncStatus.SYNCING -> SyncStatus.UPLOAD_FAILED
                    SyncStatus.PENDING_DELETE -> SyncStatus.DELETE_FAILED
                    else -> SyncStatus.UPLOAD_FAILED
                }

                localDao.updatePaciente(
                    entity.copy(syncStatus = failedStatus)
                )
                retryCache.remove(entity.id)
            } else {
                // Mantém status atual para retry
                retryCache[entity.id] = retryCount
            }
        }
    }

    // Novo método para retry de itens com falha
    private suspend fun retryFailedItems(failedItems: List<PacienteEntity>) {
        val uploadFailed = failedItems.filter { it.syncStatus == SyncStatus.UPLOAD_FAILED }
        val deleteFailed = failedItems.filter { it.syncStatus == SyncStatus.DELETE_FAILED }

        // Muda status para retry
        uploadFailed.forEach { entity ->
            localDao.updatePaciente(
                entity.copy(syncStatus = SyncStatus.PENDING_UPLOAD)
            )
        }

        deleteFailed.forEach { entity ->
            localDao.updatePaciente(
                entity.copy(syncStatus = SyncStatus.PENDING_DELETE)
            )
        }

        // Executa sincronização
        if (uploadFailed.isNotEmpty()) {
            syncPacientesBatch(uploadFailed)
        }
        if (deleteFailed.isNotEmpty()) {
            syncDeletionsBatch(deleteFailed)
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
                        retryCache.remove(localId)
                    }
                } else {
                    handleSyncFailure(listOf(localEntity))
                }
            }
        } catch (e: Exception) {
            println("Erro ao sincronizar paciente $localId: ${e.message}")
            val localEntity = localDao.getPacienteById(localId)
            if (localEntity != null) {
                handleSyncFailure(listOf(localEntity))
            }
        }
    }

    private suspend fun syncDeletionToServer(localId: Long) {
        try {
            val localEntity = localDao.getPacienteById(localId)
            if (localEntity?.serverId != null) {
                val response = apiService.deletePaciente(localEntity.serverId)
                if (response.isSuccessful) {
                    localDao.deletePacientePermanently(localId)
                    retryCache.remove(localId)
                } else {
                    handleSyncFailure(listOf(localEntity))
                }
            } else {
                // Se não tem serverId, deleta localmente
                localDao.deletePacientePermanently(localId)
            }
        } catch (e: Exception) {
            println("Erro ao sincronizar deleção $localId: ${e.message}")
            val localEntity = localDao.getPacienteById(localId)
            if (localEntity != null) {
                handleSyncFailure(listOf(localEntity))
            }
        }
    }

    // SINCRONIZAÇÃO COMPLETA
    suspend fun performFullSync(): Result<Unit> {
        return try {
            // Primeiro upload dos dados locais
            syncToServer().getOrThrow()

            // Depois download dos dados do servidor
            syncFromServer().getOrThrow()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // RESOLUÇÃO DE CONFLITOS
    suspend fun resolveConflict(
        pacienteId: Long,
        resolution: ConflictResolution
    ): Result<Unit> {
        return try {
            val entity = localDao.getPacienteById(pacienteId)
            if (entity?.syncStatus == SyncStatus.CONFLICT) {
                when (resolution) {
                    ConflictResolution.KEEP_LOCAL -> {
                        localDao.updatePaciente(
                            entity.copy(
                                syncStatus = SyncStatus.PENDING_UPLOAD,
                                conflictData = null
                            )
                        )
                    }
                    ConflictResolution.KEEP_SERVER -> {
                        val serverData = entity.conflictData?.let {
                            parseServerData(it)
                        }
                        if (serverData != null) {
                            val updatedEntity = entity.updateFrom(serverData).copy(
                                syncStatus = SyncStatus.SYNCED,
                                conflictData = null
                            )
                            localDao.updatePaciente(updatedEntity)
                        }
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // MÉTODOS UTILITÁRIOS

    suspend fun getPendingSyncCount(): Int {
        return localDao.getPendingSyncCount() // PENDING_UPLOAD + PENDING_DELETE
    }

    suspend fun getFailedSyncCount(): Int {
        return localDao.getFailedSyncCount() // UPLOAD_FAILED + DELETE_FAILED
    }

    suspend fun getConflictCount(): Int {
        return localDao.getConflictCount() // CONFLICT
    }

    suspend fun getSyncingCount(): Int {
        return localDao.getSyncingCount() // SYNCING
    }

    suspend fun hasPendingSync(): Boolean {
        return getPendingSyncCount() > 0
    }

    suspend fun hasFailedSync(): Boolean {
        return getFailedSyncCount() > 0
    }

    suspend fun hasConflicts(): Boolean {
        return getConflictCount() > 0
    }

    suspend fun isSyncing(): Boolean {
        return getSyncingCount() > 0
    }

    suspend fun getLastSyncDate(): Long? {
        val timestamp = syncMetadataDao.getLastPatientSyncTimestamp()
        return timestamp.takeIf { it > 0 }
    }

    suspend fun isSyncInProgress(): Boolean {
        return syncMetadataDao.isSyncInProgress()
    }

    suspend fun getSyncStats(): SyncStats {
        return SyncStats(
            pendingSync = getPendingSyncCount(),
            failedSync = getFailedSyncCount(),
            conflicts = getConflictCount(),
            syncing = getSyncingCount(),
            lastSyncDate = getLastSyncDate()
        )
    }

    // Retry manual para itens com falha
    suspend fun retryFailedSync(): Result<Unit> {
        return try {
            val failedItems = localDao.getFailedItems()
            if (failedItems.isNotEmpty()) {
                retryFailedItems(failedItems)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Métodos privados auxiliares
    private fun parseServerData(jsonData: String): Paciente? {
        // Implementar parsing do JSON para Paciente
        return try {
            Gson().fromJson(jsonData, Paciente::class.java)
        } catch (e: Exception) {
            null
        }
    }
}