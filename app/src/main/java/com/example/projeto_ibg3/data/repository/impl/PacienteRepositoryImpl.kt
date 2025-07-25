package com.example.projeto_ibg3.data.repository.impl

import com.example.projeto_ibg3.data.local.database.dao.PacienteDao
import com.example.projeto_ibg3.data.local.database.dao.SyncMetadataDao
import com.example.projeto_ibg3.data.local.database.entities.PacienteEntity
import com.example.projeto_ibg3.data.mappers.toPaciente
import com.example.projeto_ibg3.data.mappers.updateFrom
import com.example.projeto_ibg3.data.remote.api.ApiService
import com.example.projeto_ibg3.data.remote.dto.PacienteDto
import com.example.projeto_ibg3.domain.model.Paciente
import com.example.projeto_ibg3.domain.model.SyncStatus
import com.example.projeto_ibg3.core.utils.NetworkUtils
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import android.util.Log
import com.example.projeto_ibg3.data.mappers.toDto
import com.example.projeto_ibg3.data.mappers.toEntity
import com.example.projeto_ibg3.data.remote.conflict.ConflictResolution
import com.example.projeto_ibg3.domain.repository.PacienteRepository
import com.example.projeto_ibg3.sync.model.SyncStatistics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import java.util.Date
import java.util.Locale


@Singleton
class PacienteRepositoryImpl @Inject constructor(
    private val localDao: PacienteDao,
    private val syncMetadataDao: SyncMetadataDao,
    private val apiService: ApiService,
    private val networkUtils: NetworkUtils,
): PacienteRepository {
    companion object {
        private const val TAG = "PacienteRepositoryImpl"
    }

    // Mutex para evitar sincronizações concorrentes
    private val syncMutex = Mutex()

    // Cache para controle de retry
    private val retryCache = mutableMapOf<String, Int>() // Mudança: usar String para localId
    private val maxRetries = 3

    // Device ID para controle de origem
    private val deviceId = UUID.randomUUID().toString()

    // ========== OPERAÇÕES BÁSICAS (OFFLINE-FIRST) ==========

    /**
     * FONTE ÚNICA DA VERDADE: Sempre retorna dados do banco local
     * Com tratamento de erro robusto
     */
    override fun getAllPacientes(): Flow<List<Paciente>> {
        return localDao.getAllPacientes()
            .map { entities ->
                Log.d(TAG, "Carregando ${entities.size} pacientes do banco local")
                entities.filter { !it.isDeleted }.map { it.toPaciente() }
            }
            .catch { exception ->
                Log.e(TAG, "Erro ao carregar pacientes do banco local", exception)
                emit(emptyList())
            }
            .onStart {
                Log.d(TAG, "Iniciando carregamento de pacientes")
            }
    }

    // Busca por localId - sempre do local primeiro
    override suspend fun getPacienteById(localId: String): Paciente? {
        return try {
            Log.d(TAG, "Buscando paciente com localId: $localId")
            localDao.getPacienteByLocalId(localId)?.takeIf { !it.isDeleted }?.toPaciente()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar paciente por localId: $localId", e)
            null
        }
    }

    // Busca por CPF
    suspend fun getPacienteByCpf(cpf: String): Paciente? {
        return try {
            Log.d(TAG, "Buscando paciente com CPF: $cpf")
            localDao.getPacienteByCpf(cpf)?.takeIf { !it.isDeleted }?.toPaciente()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar paciente por CPF: $cpf", e)
            null
        }
    }

    // Busca por SUS
    suspend fun getPacienteBySus(sus: String): Paciente? {
        return try {
            Log.d(TAG, "Buscando paciente com SUS: $sus")
            localDao.getPacienteBySus(sus)?.takeIf { !it.isDeleted }?.toPaciente()
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao buscar paciente por SUS: $sus", e)
            null
        }
    }

    // Busca por nome (local primeiro)
    override suspend fun searchPacientes(query: String): Flow<List<Paciente>> {
        return localDao.searchPacientes(query)
            .map { entities ->
                entities.filter { !it.isDeleted }.map { it.toPaciente() }
            }
            .catch { exception ->
                Log.e(TAG, "Erro ao buscar pacientes por query: $query", exception)
                emit(emptyList())
            }
    }

    // ========== OPERAÇÕES CRUD SIMPLIFICADAS ==========

    // INSERIR: Salva local primeiro, sincronização em background
    override suspend fun insertPaciente(paciente: Paciente): String { // MUDANÇA: Long -> String
        return try {
            Log.d(TAG, "Inserindo paciente: ${paciente.nome}")

            // Verificar duplicatas por CPF
            if (paciente.cpf.isNotBlank()) {
                val existingByCpf = localDao.getPacienteByCpf(paciente.cpf)
                if (existingByCpf != null && !existingByCpf.isDeleted) {
                    throw IllegalStateException("Já existe um paciente com este CPF")
                }
            }

            // Verificar duplicatas por SUS
            if ((paciente.sus ?: "").isNotBlank()) {
                val existingBySus = localDao.getPacienteBySus(paciente.sus)
                if (existingBySus != null && !existingBySus.isDeleted) {
                    throw IllegalStateException("Já existe um paciente com este SUS")
                }
            }

            val entity = paciente.toEntity().copy(
                syncStatus = SyncStatus.PENDING_UPLOAD,
                updatedAt = System.currentTimeMillis(),
                deviceId = deviceId
            )

            // A inserção retorna o localId (String) da entidade
            localDao.insertPaciente(entity)
            val insertedId = entity.localId // MUDANÇA: usar localId da entidade
            Log.d(TAG, "Paciente inserido com localId: $insertedId")

            // Sincronização em background (não bloqueia a operação)
            tryBackgroundSync(insertedId)

            insertedId // MUDANÇA: retorna String
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao inserir paciente", e)
            throw e
        }
    }

    // ATUALIZAR: Atualiza local primeiro, sincronização em background
    override suspend fun updatePaciente(paciente: Paciente) {
        try {
            Log.d(TAG, "Atualizando paciente: ${paciente.nome}")

            val existingEntity = localDao.getPacienteByLocalId(paciente.localId)
            if (existingEntity != null && !existingEntity.isDeleted) {
                // Verificar duplicatas por CPF (excluindo o próprio)
                if (paciente.cpf.isNotBlank()) {
                    val countByCpf = localDao.countPacientesByCpfExcluding(paciente.cpf, paciente.localId)
                    if (countByCpf > 0) {
                        throw IllegalStateException("Já existe outro paciente com este CPF")
                    }
                }

                // Verificar duplicatas por SUS (excluindo o próprio)
                if ((paciente.sus ?: "").isNotBlank()) {
                    val countBySus = localDao.countPacientesBySusExcluding(paciente.sus, paciente.localId)
                    if (countBySus > 0) {
                        throw IllegalStateException("Já existe outro paciente com este SUS")
                    }
                }

                val updatedEntity = existingEntity.updateFrom(paciente).copy(
                    syncStatus = SyncStatus.PENDING_UPLOAD,
                    updatedAt = System.currentTimeMillis(),
                    version = existingEntity.version + 1
                )
                localDao.updatePaciente(updatedEntity)
                Log.d(TAG, "Paciente atualizado localmente")

                // Sincronização em background
                tryBackgroundSync(updatedEntity.localId)
            } else {
                Log.w(TAG, "Paciente não encontrado ou foi deletado: ${paciente.localId}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao atualizar paciente", e)
            throw e
        }
    }

    // DELETAR: Marca como deletado localmente, sincronização em background
    override suspend fun deletePaciente(localId: String) {
        try {
            Log.d(TAG, "Deletando paciente com localId: $localId")

            localDao.markAsDeleted(
                localId = localId,
                status = SyncStatus.PENDING_DELETE,
                timestamp = System.currentTimeMillis()
            )

            Log.d(TAG, "Paciente marcado como deletado localmente")

            // Sincronização em background
            tryBackgroundSync(localId)

        } catch (e: Exception) {
            Log.e(TAG, "Erro ao deletar paciente", e)
            throw e
        }
    }

    override suspend fun restorePaciente(pacienteLocalId: String) {
        withContext(Dispatchers.IO) {
            val paciente = localDao.getPacienteById(pacienteLocalId)
            if (paciente != null && paciente.isDeleted) {
                val restoredPaciente = paciente.copy(
                    isDeleted = false,
                    syncStatus = SyncStatus.PENDING_UPLOAD,
                    updatedAt = System.currentTimeMillis()
                )
                localDao.updatePaciente(restoredPaciente)
            }
        }
    }

    override suspend fun syncPacientes(): Result<Unit> {
        TODO("Not yet implemented")
    }

    override suspend fun getPacientesNaoSincronizados(): List<Paciente> {
        TODO("Not yet implemented")
    }

    override suspend fun markAsSynced(localId: String, serverId: Long) {
        TODO("Not yet implemented")
    }

    override suspend fun hasPendingChanges(): Boolean {
        return withContext(Dispatchers.IO) {
            localDao.getUnsyncedCount() > 0
        }
    }

    override suspend fun getPacienteCount(): Int {
        TODO("Not yet implemented")
    }

    override suspend fun pacienteExists(cpf: String, excludeId: String): Boolean {
        TODO("Not yet implemented")
    }

    // ========== SINCRONIZAÇÃO SIMPLIFICADA ==========

    /**
     * Tenta sincronizar em background (não falha se não conseguir)
     */
    private suspend fun tryBackgroundSync(localId: String) {
        try {
            if (networkUtils.isNetworkAvailable()) {
                Log.d(TAG, "Tentando sincronizar paciente $localId em background")
                syncPacienteToServer(localId)
            } else {
                Log.d(TAG, "Sem conexão, sincronização será feita posteriormente")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Erro na sincronização em background (não crítico)", e)
        }
    }

    // Sincroniza dados do servidor para o local (Download)
    suspend fun syncFromServer(): Result<Unit> = syncMutex.withLock {
        if (!networkUtils.isNetworkAvailable()) {
            return Result.failure(Exception("Sem conexão com a internet"))
        }

        return try {
            Log.d(TAG, "Iniciando sincronização do servidor")

            syncMetadataDao.setSyncInProgress(true)

            val lastSync = syncMetadataDao.getLastPatientSyncTimestamp()
            val response = apiService.getUpdatedPacientes(lastSync)

            if (response.isSuccessful) {
                val apiResponse = response.body()

                // CORREÇÃO: Extrair a lista do ApiResponse
                val serverPacientes: List<PacienteDto> = when {
                    apiResponse?.success == true -> apiResponse.data ?: emptyList()
                    else -> {
                        val errorMsg = apiResponse?.error ?: apiResponse?.message ?: "Erro desconhecido"
                        throw Exception("Erro da API: $errorMsg")
                    }
                }

                Log.d(TAG, "Recebidos ${serverPacientes.size} pacientes do servidor")

                // Processar cada paciente
                serverPacientes.forEach { serverPacienteDto: PacienteDto ->
                    processServerPaciente(serverPacienteDto)
                }

                syncMetadataDao.updateLastPatientSyncTimestamp(System.currentTimeMillis())
                syncMetadataDao.setSyncInProgress(false)

                Log.d(TAG, "Sincronização do servidor concluída com sucesso")
                Result.success(Unit)
            } else {
                syncMetadataDao.setSyncInProgress(false)
                val error = "Erro HTTP: ${response.code()} - ${response.message()}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }
        } catch (e: Exception) {
            syncMetadataDao.setSyncInProgress(false)
            Log.e(TAG, "Erro na sincronização do servidor", e)
            Result.failure(e)
        }
    }

    // Sincroniza dados locais para o servidor (Upload)
    suspend fun syncToServer(): Result<Unit> = syncMutex.withLock {
        if (!networkUtils.isNetworkAvailable()) {
            return Result.failure(Exception("Sem conexão com a internet"))
        }

        return try {
            Log.d(TAG, "Iniciando sincronização para o servidor")

            val pendingPacientes = localDao.getPendingUpload()
            val pendingDeletions = localDao.getPendingDeletion()
            val failedItems = localDao.getFailedSyncItems()

            Log.d(TAG, "Pending uploads: ${pendingPacientes.size}, deletions: ${pendingDeletions.size}, failed: ${failedItems.size}")

            // Sincronização para uploads
            if (pendingPacientes.isNotEmpty()) {
                syncPacientesBatch(pendingPacientes)
            }

            // Sincronização para deleções
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

    // Processa dados do servidor com resolução de conflitos
    private suspend fun processServerPaciente(serverPacienteDto: PacienteDto) {
        try {
            val serverId = serverPacienteDto.serverId ?: return
            val localEntity = localDao.getPacienteByServerId(serverId)

            if (localEntity == null) {
                // Novo paciente do servidor
                val entity = serverPacienteDto.toEntity().copy(
                    syncStatus = SyncStatus.SYNCED,
                    serverId = serverId,
                    deviceId = serverPacienteDto.deviceId ?: ""
                )
                localDao.insertPaciente(entity)
                Log.d(TAG, "Novo paciente inserido do servidor: ${serverPacienteDto.nome}")
            } else {
                // Verifica conflitos
                when {
                    localEntity.syncStatus == SyncStatus.SYNCED -> {
                        // Sem conflito, atualiza normalmente
                        val updatedEntity = localEntity.updateFrom(serverPacienteDto).copy(
                            syncStatus = SyncStatus.SYNCED
                        )
                        localDao.updatePaciente(updatedEntity)
                        Log.d(TAG, "Paciente atualizado do servidor: ${serverPacienteDto.nome}")
                    }

                    localEntity.updatedAt > (serverPacienteDto.updatedAt?.let { parseTimestamp(it) } ?: 0) -> {
                        // Dados locais são mais recentes, mantém local
                        localDao.updateSyncStatus(
                            localEntity.localId,
                            SyncStatus.PENDING_UPLOAD
                        )
                        Log.d(TAG, "Mantendo dados locais mais recentes: ${serverPacienteDto.nome}")
                    }

                    else -> {
                        // Conflito detectado
                        localDao.markAsConflict(
                            localEntity.localId,
                            Gson().toJson(serverPacienteDto)
                        )
                        Log.w(TAG, "Conflito detectado para paciente: ${serverPacienteDto.nome}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao processar paciente do servidor: ${serverPacienteDto.nome}", e)
        }
    }

    // Sincronização individual
    private suspend fun syncPacienteToServer(localId: String) {
        try {
            val localEntity = localDao.getPacienteByLocalId(localId)
            if (localEntity != null && !localEntity.isDeleted) {
                val pacienteDto = localEntity.toDto()

                val response = if (localEntity.serverId != null) {
                    apiService.updatePaciente(localEntity.serverId, pacienteDto)
                } else {
                    apiService.createPaciente(pacienteDto)
                }

                if (response.isSuccessful) {
                    val apiResponse = response.body()

                    // CORREÇÃO: Extrair o paciente do ApiResponse
                    val serverPacienteDto = when {
                        apiResponse?.success == true -> apiResponse.data
                        else -> {
                            val errorMsg = apiResponse?.error ?: apiResponse?.message ?: "Erro desconhecido"
                            throw Exception("Erro na sincronização individual: $errorMsg")
                        }
                    }

                    if (serverPacienteDto != null) {
                        localDao.updateSyncStatusAndServerId(
                            localEntity.localId,
                            SyncStatus.SYNCED,
                            serverPacienteDto.serverId ?: throw Exception("ServerId não retornado pelo servidor")
                        )
                        retryCache.remove(localId)
                        Log.d(TAG, "Paciente sincronizado com sucesso: ${localEntity.nome}")
                    }
                } else {
                    handleSyncFailure(listOf(localEntity))
                    Log.w(TAG, "Falha na sincronização: ${response.message()}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao sincronizar paciente $localId", e)
            val localEntity = localDao.getPacienteByLocalId(localId)
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
                val pacientesDto: List<PacienteDto> = pacientesToCreate.map { it.toDto() }
                val response = apiService.createPacientesBatch(pacientesDto)

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    val serverPacientes: List<PacienteDto> = when {
                        apiResponse?.success == true -> apiResponse.data ?: emptyList()
                        else -> {
                            val errorMsg = apiResponse?.error ?: apiResponse?.message ?: "Erro desconhecido"
                            throw Exception("Erro na criação em lote: $errorMsg")
                        }
                    }

                    // SOLUÇÃO SEGURA: Mapear por localId em vez de posição
                    val serverPacientesMap = serverPacientes.associateBy { it.localId }

                    pacientesToCreate.forEach { localEntity ->
                        val serverPacienteDto = serverPacientesMap[localEntity.localId]

                        if (serverPacienteDto?.serverId != null) {
                            localDao.updateSyncStatusAndServerId(
                                localEntity.localId,
                                SyncStatus.SYNCED,
                                serverPacienteDto.serverId
                            )
                            retryCache.remove(localEntity.localId)
                            Log.d(TAG, "Paciente sincronizado: ${localEntity.nome}")
                        } else {
                            Log.e(TAG, "ServerId não encontrado para paciente: ${localEntity.localId}")
                            handleSyncFailure(listOf(localEntity))
                        }
                    }

                    Log.d(TAG, "Batch create sincronizado: ${serverPacientes.size} pacientes")
                } else {
                    handleSyncFailure(pacientesToCreate)
                }
            }

            // Atualiza pacientes existentes (permanece igual)
            if (pacientesToUpdate.isNotEmpty()) {
                val pacientesDto: List<PacienteDto> = pacientesToUpdate.map { it.toDto() }
                val response = apiService.updatePacientesBatch(pacientesDto)

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    when {
                        apiResponse?.success == true -> {
                            pacientesToUpdate.forEach { entity ->
                                localDao.updateSyncStatus(entity.localId, SyncStatus.SYNCED)
                                retryCache.remove(entity.localId)
                            }
                            Log.d(TAG, "Batch update sincronizado: ${pacientesToUpdate.size} pacientes")
                        }
                        else -> {
                            val errorMsg = apiResponse?.error ?: apiResponse?.message ?: "Erro desconhecido"
                            throw Exception("Erro na atualização em lote: $errorMsg")
                        }
                    }
                } else {
                    handleSyncFailure(pacientesToUpdate)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao sincronizar lote", e)
            handleSyncFailure(entities)
        }
    }

    // Sincronização em lote para deleções
    private suspend fun syncDeletionsBatch(entities: List<PacienteEntity>) {
        try {
            val serverIds = entities.mapNotNull { it.serverId }
            if (serverIds.isNotEmpty()) {
                val response = apiService.deletePacientesBatch(serverIds)

                if (response.isSuccessful) {
                    val apiResponse = response.body()

                    // CORREÇÃO: Verificar se a operação foi bem-sucedida
                    when {
                        apiResponse?.success == true -> {
                            entities.forEach { entity ->
                                localDao.deletePacientePermanently(entity.localId)
                                retryCache.remove(entity.localId)
                            }
                            Log.d(TAG, "Batch delete sincronizado: ${entities.size} pacientes")
                        }
                        else -> {
                            val errorMsg = apiResponse?.error ?: apiResponse?.message ?: "Erro desconhecido"
                            throw Exception("Erro na deleção em lote: $errorMsg")
                        }
                    }
                } else {
                    handleSyncFailure(entities)
                }
            } else {
                // Deleta localmente se não tem serverId
                entities.forEach { entity ->
                    localDao.deletePacientePermanently(entity.localId)
                }
                Log.d(TAG, "Deleções locais processadas: ${entities.size} pacientes")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao sincronizar deleções", e)
            handleSyncFailure(entities)
        }
    }

    // Gerencia falhas de sincronização com retry
    private suspend fun handleSyncFailure(entities: List<PacienteEntity>) {
        entities.forEach { entity ->
            val entityLocalId = entity.localId // MUDANÇA: já é String
            val retryCount = retryCache.getOrDefault(entityLocalId, 0) + 1 // MUDANÇA: usar entityLocalId

            if (retryCount >= maxRetries) {
                // Marca como erro após máximo de tentativas
                val failedStatus = when (entity.syncStatus) {
                    SyncStatus.PENDING_UPLOAD -> SyncStatus.UPLOAD_FAILED
                    SyncStatus.PENDING_DELETE -> SyncStatus.DELETE_FAILED
                    else -> SyncStatus.UPLOAD_FAILED
                }

                localDao.updateSyncStatus(entityLocalId, failedStatus)
                retryCache.remove(entityLocalId) // MUDANÇA: usar entityLocalId
                Log.w(TAG, "Falha definitiva na sincronização após $maxRetries tentativas: $entityLocalId")
            } else {
                // Incrementa contador de tentativas
                localDao.incrementSyncAttempts(
                    entityLocalId,
                    System.currentTimeMillis(),
                    "Falha na sincronização"
                )
                retryCache[entityLocalId] = retryCount // MUDANÇA: usar entityLocalId
                Log.d(TAG, "Retry $retryCount/$maxRetries para paciente: $entityLocalId")
            }
        }
    }

    // Retry de itens com falha
    private suspend fun retryFailedItems(failedItems: List<PacienteEntity>) {
        val uploadFailed = failedItems.filter { it.syncStatus == SyncStatus.UPLOAD_FAILED }
        val deleteFailed = failedItems.filter { it.syncStatus == SyncStatus.DELETE_FAILED }

        // Muda status para retry
        uploadFailed.forEach { entity ->
            localDao.updateSyncStatus(entity.localId, SyncStatus.PENDING_UPLOAD)
        }

        deleteFailed.forEach { entity ->
            localDao.updateSyncStatus(entity.localId, SyncStatus.PENDING_DELETE)
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

    override fun getPendingSyncCount(): Flow<Int> {
        return flow {
            emit(localDao.getPendingSyncCount())
        }.flowOn(Dispatchers.IO)
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
        return getPendingSyncCount().first() > 0
    }

    suspend fun hasFailedSync(): Boolean {
        return getFailedSyncCount() > 0
    }

    suspend fun getLastSyncDate(): Long? {
        return try {
            val timestamp = syncMetadataDao.getLastPatientSyncTimestamp()
            if (timestamp > 0) timestamp else null
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao obter data da última sincronização", e)
            null
        }
    }

    override suspend fun retryFailedSync(): Result<Unit> {
        return try {
            Log.d(TAG, "Tentando retry de sincronizações falhadas")
            val failedItems = localDao.getFailedSyncItems()
            if (failedItems.isNotEmpty()) {
                retryFailedItems(failedItems)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro no retry de sincronizações falhadas", e)
            Result.failure(e)
        }
    }

    override suspend fun getFailedSyncPacientes(): List<Paciente> {
        return withContext(Dispatchers.IO) {
            localDao.getFailedSyncItems().map { it.toPaciente() }
        }
    }

    override suspend fun resolveConflictKeepLocal(pacienteLocalId: String) {
        withContext(Dispatchers.IO) {
            // Marca como pendente para reenviar ao servidor
            localDao.updateSyncStatus(pacienteLocalId, SyncStatus.PENDING_UPLOAD)
        }
    }

    override suspend fun resolveConflictKeepServer(pacienteLocalId: String) {
        withContext(Dispatchers.IO) {
            // Busca dados do servidor e sobrescreve local
            // Por enquanto apenas marca como sincronizado
            // Em uma implementação completa, buscaria do servidor
            localDao.updateSyncStatus(pacienteLocalId, SyncStatus.SYNCED)
        }
    }

    override fun getConflictCount(): Flow<Int> {
        return flow {
            emit(localDao.getConflictCount())
        }.flowOn(Dispatchers.IO)
    }

    override fun getPacientesByStatus(status: SyncStatus): Flow<List<Paciente>> {
        return flow {
            emit(localDao.getItemsByStatus(status).map { it.toPaciente() })
        }.flowOn(Dispatchers.IO)
    }

    override fun getSyncStatistics(): Flow<SyncStatistics> {
        return flow {
            val total = localDao.getTotalPacientes()
            val synced = localDao.getItemsByStatus(SyncStatus.SYNCED).size
            val pending = localDao.getPendingSyncCount()
            val conflicts = localDao.getConflictCount()
            val failed = localDao.getFailedSyncCount()

            emit(
                SyncStatistics(
                    total = total,
                    synced = synced,
                    pending = pending,
                    conflicts = conflicts,
                    failed = failed,
                    totalSynced = TODO(),
                    uploaded = TODO(),
                    downloaded = TODO(),
                    duration = TODO()
                )
            )
        }.flowOn(Dispatchers.IO)
    }

    // ========== RESOLUÇÃO DE CONFLITOS ==========

    suspend fun resolveConflict(
        localId: String,
        resolution: ConflictResolution
    ): Result<Unit> {
        return try {
            val entity = localDao.getPacienteByLocalId(localId)
            if (entity?.syncStatus == SyncStatus.CONFLICT) {
                when (resolution) {
                    ConflictResolution.KEEP_LOCAL -> {
                        localDao.updateSyncStatus(localId, SyncStatus.PENDING_UPLOAD)
                        Log.d(TAG, "Conflito resolvido mantendo dados locais: $localId")
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
                            Log.d(TAG, "Conflito resolvido mantendo dados do servidor: $localId")
                        }
                    }
                    ConflictResolution.MANUAL -> {
                        // Permite resolução manual pelo usuário
                        Log.d(TAG, "Conflito marcado para resolução manual: $localId")
                        // Aqui você pode implementar a lógica específica para resolução manual
                    }

                    // Adiciona os casos não implementados com aviso para correção futura na API
                    ConflictResolution.MERGE_AUTOMATIC,
                    ConflictResolution.MERGE_MANUAL -> {
                        Log.w(TAG, "Tipo de resolução de conflito não implementado: $resolution para localId: $localId")
                        Log.w(TAG, "ATENÇÃO: Implementar lógica de resolução de conflitos na API para este tipo")

                        // Por enquanto, marca como pendente para upload (mantém dados locais)
                        localDao.updateSyncStatus(localId, SyncStatus.PENDING_UPLOAD)

                        // Você pode também retornar um erro se preferir que seja tratado em nível superior
                        // return Result.failure(Exception("Tipo de resolução não implementado: $resolution"))
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao resolver conflito: $localId", e)
            Result.failure(e)
        }
    }


    private fun parseServerData(jsonData: String): PacienteDto? {
        return try {
            Gson().fromJson(jsonData, PacienteDto::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao fazer parse dos dados do servidor", e)
            null
        }
    }

    // ========== FUNÇÕES AUXILIARES ==========

    // Função para parsing de datas
    private fun parseDate(dateString: String): Date {
        return try {
            // Implementar parsing conforme formato usado no servidor
            // Exemplo para ISO format: SimpleDateFormat("yyyy-MM-dd").parse(dateString)
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateString) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }

    // Função para formatação de datas
    private fun formatDate(date: Date): String {
        return try {
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
        } catch (e: Exception) {
            ""
        }
    }

    // Função para parsing de timestamps
    private fun parseTimestamp(timestampString: String): Long {
        return try {
            // Se for ISO format
            Instant.parse(timestampString).toEpochMilli()
        } catch (e: Exception) {
            // Se for timestamp em string
            timestampString.toLongOrNull() ?: System.currentTimeMillis()
        }
    }

    // Função para formatação de timestamps
    private fun formatTimestamp(timestamp: Long): String {
        return try {
            Instant.ofEpochMilli(timestamp).toString()
        } catch (e: Exception) {
            timestamp.toString()
        }
    }

    suspend fun getPendingSyncPacientes(): List<Paciente> {
        return localDao.getPendingUpload().map { it.toPaciente() }
    }

    suspend fun syncPaciente(paciente: Paciente) {
        syncPacienteToServer(paciente.localId)
    }
}
