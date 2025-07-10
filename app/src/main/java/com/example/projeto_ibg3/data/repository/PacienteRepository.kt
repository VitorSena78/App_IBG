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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PacienteRepository @Inject constructor(
    private val localDao: PacienteDao,
    private val syncMetadataDao: SyncMetadataDao,
    private val apiService: PacienteApiService,
    private val networkUtils: NetworkUtils
) {
    companion object {
        private const val TAG = "PacienteRepository"
    }

    // Mutex para evitar sincronizações concorrentes
    private val syncMutex = Mutex()

    // Cache para controle de retry
    private val retryCache = mutableMapOf<Long, Int>()
    private val maxRetries = 3

    // ========== OPERAÇÕES BÁSICAS (OFFLINE-FIRST) ==========

    /**
     * FONTE ÚNICA DA VERDADE: Sempre retorna dados do banco local
     * Com tratamento de erro robusto
     */
    fun getAllPacientes(): Flow<List<Paciente>> {
        return localDao.getAllPacientes()
            .map { entities ->
                Log.d(TAG, "Carregando ${entities.size} pacientes do banco local")
                entities.filter { !it.isDeleted }.map { it.toPaciente() }
            }
            .catch { exception ->
                Log.e(TAG, "Erro ao carregar pacientes do banco local", exception)
                // Emite lista vazia em caso de erro ao invés de quebrar o Flow
                emit(emptyList())
            }
            .onStart {
                Log.d(TAG, "Iniciando carregamento de pacientes")
            }
    }

    //Busca por ID - sempre do local primeiro

    suspend fun getPacienteById(id: Long): Paciente? {
        return try {
            Log.d(TAG, "Buscando paciente com ID: $id")
            localDao.getPacienteById(id)?.takeIf { !it.isDeleted }?.toPaciente()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar paciente por ID: $id", e)
            null
        }
    }

    //Busca por nome (local primeiro)

    fun searchPacientes(nome: String): Flow<List<Paciente>> {
        return localDao.searchPacientes("%$nome%")
            .map { entities ->
                entities.filter { !it.isDeleted }.map { it.toPaciente() }
            }
            .catch { exception ->
                Log.e(TAG, "Erro ao buscar pacientes por nome: $nome", exception)
                emit(emptyList())
            }
    }

    // ========== OPERAÇÕES CRUD SIMPLIFICADAS ==========

    // INSERIR: Salva local primeiro, sincronização em background

    suspend fun insertPaciente(paciente: Paciente) {
        try {
            Log.d(TAG, "Inserindo paciente: ${paciente.nome}")

            val entity = paciente.toEntity().copy(
                syncStatus = SyncStatus.PENDING_UPLOAD,
                lastModified = System.currentTimeMillis()
            )

            val localId = localDao.insertPaciente(entity)
            Log.d(TAG, "Paciente inserido com ID local: $localId")

            // Sincronização em background (não bloqueia a operação)
            tryBackgroundSync(localId)

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inserir paciente", e)
            throw e
        }
    }

    //ATUALIZAR: Atualiza local primeiro, sincronização em background

    suspend fun updatePaciente(paciente: Paciente) {
        try {
            Log.d(TAG, "Atualizando paciente: ${paciente.nome}")

            val existingEntity = localDao.getPacienteById(paciente.id)
            if (existingEntity != null && !existingEntity.isDeleted) {
                val updatedEntity = existingEntity.updateFrom(paciente).copy(
                    syncStatus = SyncStatus.PENDING_UPLOAD,
                    lastModified = System.currentTimeMillis(),
                    version = existingEntity.version + 1
                )
                localDao.updatePaciente(updatedEntity)
                Log.d(TAG, "Paciente atualizado localmente")

                // Sincronização em background
                tryBackgroundSync(updatedEntity.id)
            } else {
                Log.w(TAG, "Paciente não encontrado ou foi deletado: ${paciente.id}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao atualizar paciente", e)
            throw e
        }
    }

    //DELETAR: Marca como deletado localmente, sincronização em background

    suspend fun deletePaciente(id: Long) {
        try {
            Log.d(TAG, "Deletando paciente com ID: $id")

            localDao.markAsDeleted(
                id = id,
                status = SyncStatus.PENDING_DELETE,
                timestamp = System.currentTimeMillis()
            )

            Log.d(TAG, "Paciente marcado como deletado localmente")

            // Sincronização em background
            tryBackgroundSync(id)

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao deletar paciente", e)
            throw e
        }
    }

    // ========== SINCRONIZAÇÃO SIMPLIFICADA ==========

    /**
     * Tenta sincronizar em background (não falha se não conseguir)
     */
    private suspend fun tryBackgroundSync(localId: Long) {
        try {
            if (networkUtils.isNetworkAvailable()) {
                Log.d(TAG, "Tentando sincronizar paciente $localId em background")
                syncPacienteToServer(localId)
            } else {
                Log.d(TAG, "Sem conexão, sincronização será feita posteriormente")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Erro na sincronização em background (não crítico)", e)
            // Não propaga o erro - sincronização é em background
        }
    }

    //Sincroniza dados do servidor para o local (Download)

    suspend fun syncFromServer(): Result<Unit> = syncMutex.withLock {
        if (!networkUtils.isNetworkAvailable()) {
            return Result.failure(Exception("Sem conexão com a internet"))
        }

        return try {
            Log.d(TAG, "Iniciando sincronização do servidor")

            // Marca sincronização como em progresso
            syncMetadataDao.setSyncInProgress(true)

            val lastSync = syncMetadataDao.getLastPatientSyncTimestamp()
            val response = apiService.getUpdatedPacientes(lastSync)

            if (response.isSuccessful) {
                val serverPacientes = response.body() ?: emptyList()
                Log.d(TAG, "Recebidos ${serverPacientes.size} pacientes do servidor")

                // Processa dados do servidor com resolução de conflitos
                serverPacientes.forEach { serverPaciente ->
                    processServerPaciente(serverPaciente)
                }

                syncMetadataDao.updateLastPatientSyncTimestamp(System.currentTimeMillis())
                syncMetadataDao.setSyncInProgress(false)

                Log.d(TAG, "Sincronização do servidor concluída com sucesso")
                Result.success(Unit)
            } else {
                syncMetadataDao.setSyncInProgress(false)
                val error = "Erro ao sincronizar: ${response.message()}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            syncMetadataDao.setSyncInProgress(false)
            Log.e(TAG, "Erro na sincronização do servidor", e)
            Result.failure(e)
        }
    }

    //Sincroniza dados locais para o servidor (Upload)

    suspend fun syncToServer(): Result<Unit> = syncMutex.withLock {
        if (!networkUtils.isNetworkAvailable()) {
            return Result.failure(Exception("Sem conexão com a internet"))
        }

        return try {
            Log.d(TAG, "Iniciando sincronização para o servidor")

            val pendingPacientes = localDao.getPendingSync()
            val pendingDeletions = localDao.getPendingDeletions()
            val failedItems = localDao.getFailedItems()

            Log.d(TAG, "Pending uploads: ${pendingPacientes.size}, deletions: ${pendingDeletions.size}, failed: ${failedItems.size}")

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

            Log.d(TAG, "Sincronização para o servidor concluída")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro na sincronização para o servidor", e)
            Result.failure(e)
        }
    }

    // ========== MÉTODOS AUXILIARES ==========

    //Processa dados do servidor com resolução de conflitos

    private suspend fun processServerPaciente(serverPaciente: Paciente) {
        try {
            val localEntity = localDao.getPacienteByServerId(serverPaciente.id)

            if (localEntity == null) {
                // Novo paciente do servidor
                val entity = serverPaciente.toEntity().copy(
                    syncStatus = SyncStatus.SYNCED,
                    serverId = serverPaciente.id
                )
                localDao.insertPaciente(entity)
                Log.d(TAG, "Novo paciente inserido do servidor: ${serverPaciente.nome}")
            } else {
                // Verifica conflitos
                when {
                    localEntity.syncStatus == SyncStatus.SYNCED -> {
                        // Sem conflito, atualiza normalmente
                        val updatedEntity = localEntity.updateFrom(serverPaciente).copy(
                            syncStatus = SyncStatus.SYNCED
                        )
                        localDao.updatePaciente(updatedEntity)
                        Log.d(TAG, "Paciente atualizado do servidor: ${serverPaciente.nome}")
                    }

                    localEntity.lastModified > System.currentTimeMillis() -> {
                        // Dados locais são mais recentes, mantém local
                        localDao.updatePaciente(
                            localEntity.copy(syncStatus = SyncStatus.PENDING_UPLOAD)
                        )
                        Log.d(TAG, "Mantendo dados locais mais recentes: ${serverPaciente.nome}")
                    }

                    else -> {
                        // Conflito detectado
                        val conflictEntity = localEntity.copy(
                            syncStatus = SyncStatus.CONFLICT,
                            conflictData = Gson().toJson(serverPaciente)
                        )
                        localDao.updatePaciente(conflictEntity)
                        Log.w(TAG, "Conflito detectado para paciente: ${serverPaciente.nome}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao processar paciente do servidor: ${serverPaciente.nome}", e)
        }
    }

    // Sincronização individual (fallback ou para casos específicos)

    private suspend fun syncPacienteToServer(localId: Long) {
        try {
            val localEntity = localDao.getPacienteById(localId)
            if (localEntity != null && !localEntity.isDeleted) {
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
                        Log.d(TAG, "Paciente sincronizado com sucesso: ${paciente.nome}")
                    }
                } else {
                    handleSyncFailure(listOf(localEntity))
                    Log.w(TAG, "Falha na sincronização: ${response.message()}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao sincronizar paciente $localId", e)
            val localEntity = localDao.getPacienteById(localId)
            if (localEntity != null) {
                handleSyncFailure(listOf(localEntity))
            }
        }
    }

    // Sincronização em lote para uploads

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
                    serverPacientes.forEachIndexed { index, serverPaciente ->
                        val localEntity = pacientesToCreate[index]
                        localDao.updatePaciente(
                            localEntity.copy(
                                serverId = serverPaciente.id,
                                syncStatus = SyncStatus.SYNCED
                            )
                        )
                        retryCache.remove(localEntity.id)
                    }
                    Log.d(TAG, "Batch create sincronizado: ${pacientesToCreate.size} pacientes")
                } else {
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
                        retryCache.remove(entity.id)
                    }
                    Log.d(TAG, "Batch update sincronizado: ${pacientesToUpdate.size} pacientes")
                } else {
                    handleSyncFailure(pacientesToUpdate)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao sincronizar lote", e)
            handleSyncFailure(entities)
        }
    }

    //Sincronização em lote para deleções

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
                    Log.d(TAG, "Batch delete sincronizado: ${entities.size} pacientes")
                } else {
                    handleSyncFailure(entities)
                }
            } else {
                // Deleta localmente se não tem serverId
                entities.forEach { entity ->
                    localDao.deletePacientePermanently(entity.id)
                }
                Log.d(TAG, "Deleções locais processadas: ${entities.size} pacientes")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao sincronizar deleções", e)
            handleSyncFailure(entities)
        }
    }

    //Gerencia falhas de sincronização com retry

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
                Log.w(TAG, "Falha definitiva na sincronização após $maxRetries tentativas: ${entity.id}")
            } else {
                // Mantém status atual para retry
                retryCache[entity.id] = retryCount
                Log.d(TAG, "Retry $retryCount/$maxRetries para paciente: ${entity.id}")
            }
        }
    }

    //Retry de itens com falha

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

    // ========== MÉTODOS UTILITÁRIOS ==========

    suspend fun performFullSync(): Result<Unit> {
        return try {
            Log.d(TAG, "Iniciando sincronização completa")

            // Primeiro upload dos dados locais
            syncToServer().getOrThrow()

            // Depois download dos dados do servidor
            syncFromServer().getOrThrow()

            Log.d(TAG, "Sincronização completa concluída")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro na sincronização completa", e)
            Result.failure(e)
        }
    }

    suspend fun getPendingSyncCount(): Int {
        return try {
            localDao.getPendingSyncCount()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter contagem de sincronização pendente", e)
            0
        }
    }

    suspend fun getFailedSyncCount(): Int {
        return try {
            localDao.getFailedSyncCount()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter contagem de sincronização falhada", e)
            0
        }
    }

    suspend fun hasPendingSync(): Boolean {
        return getPendingSyncCount() > 0
    }

    suspend fun hasFailedSync(): Boolean {
        return getFailedSyncCount() > 0
    }

    suspend fun getLastSyncDate(): Long? {
        return try {
            val timestamp = syncMetadataDao.getLastPatientSyncTimestamp()
            timestamp.takeIf { it > 0 }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter data da última sincronização", e)
            null
        }
    }

    suspend fun retryFailedSync(): Result<Unit> {
        return try {
            Log.d(TAG, "Tentando retry de sincronizações falhadas")
            val failedItems = localDao.getFailedItems()
            if (failedItems.isNotEmpty()) {
                retryFailedItems(failedItems)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro no retry de sincronizações falhadas", e)
            Result.failure(e)
        }
    }

    // ========== RESOLUÇÃO DE CONFLITOS ==========

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
                        Log.d(TAG, "Conflito resolvido mantendo dados locais: $pacienteId")
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
                            Log.d(TAG, "Conflito resolvido mantendo dados do servidor: $pacienteId")
                        }
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao resolver conflito: $pacienteId", e)
            Result.failure(e)
        }
    }

    private fun parseServerData(jsonData: String): Paciente? {
        return try {
            Gson().fromJson(jsonData, Paciente::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao fazer parse dos dados do servidor", e)
            null
        }
    }
}