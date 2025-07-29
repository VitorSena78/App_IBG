package com.example.projeto_ibg3.data.repository.impl

import android.util.Log
import com.example.projeto_ibg3.data.local.database.dao.EspecialidadeDao
import com.example.projeto_ibg3.data.local.database.dao.PacienteDao
import com.example.projeto_ibg3.data.local.database.dao.PacienteEspecialidadeDao
import com.example.projeto_ibg3.data.local.database.entities.PacienteEntity
import com.example.projeto_ibg3.data.mappers.*
import com.example.projeto_ibg3.data.remote.api.ApiResult
import com.example.projeto_ibg3.data.remote.api.ApiService
import com.example.projeto_ibg3.data.remote.api.NetworkManager
import com.example.projeto_ibg3.data.remote.dto.PacienteDto
import com.example.projeto_ibg3.data.remote.dto.PacienteEspecialidadeDTO
import com.example.projeto_ibg3.domain.model.SyncProgress
import com.example.projeto_ibg3.domain.model.SyncState
import com.example.projeto_ibg3.domain.model.SyncStatus
import com.example.projeto_ibg3.domain.repository.SyncRepository
import com.example.projeto_ibg3.domain.repository.PacienteRepository
import com.example.projeto_ibg3.domain.repository.EspecialidadeRepository
import com.example.projeto_ibg3.domain.repository.PacienteEspecialidadeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val networkManager: NetworkManager,
    private val especialidadeDao: EspecialidadeDao,
    private val pacienteDao: PacienteDao,
    private val pacienteEspecialidadeDao: PacienteEspecialidadeDao,
    private val pacienteRepository: PacienteRepository,
    private val especialidadeRepository: EspecialidadeRepository,
    private val pacienteEspecialidadeRepository: PacienteEspecialidadeRepository,
    private val apiService: ApiService
) : SyncRepository {

    companion object {
        private const val TAG = "SyncRepository"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val BATCH_SIZE = 50
        private const val TOTAL_SYNC_STEPS = 6
    }

    // ==================== ESTADOS OBSERVÁVEIS ====================

    private val _syncState = MutableStateFlow(SyncState())
    override val syncState: Flow<SyncState> = _syncState.asStateFlow()

    private val _syncProgress = MutableStateFlow(SyncProgress())
    override val syncProgress: Flow<SyncProgress> = _syncProgress.asStateFlow()

    // ==================== SINCRONIZAÇÃO COMPLETA ====================

    override suspend fun startSync(): Flow<SyncState> = flow {
        Log.d(TAG, "Iniciando sincronização completa...")

        updateSyncState("Iniciando sincronização completa...", isLoading = true)
        emit(_syncState.value)

        try {
            if (!isNetworkAvailable()) {
                emitErrorState("Sem conexão com a internet")
                return@flow
            }

            var currentStep = 0
            val steps = listOf(
                "Sincronizando especialidades..." to suspend { syncEspecialidades() },
                "Enviando pacientes pendentes..." to suspend { uploadPendingPacientes() },
                "Baixando pacientes atualizados..." to suspend { downloadUpdatedPacientes() },
                "Enviando relacionamentos pendentes..." to suspend { uploadPendingPacienteEspecialidades() },
                "Baixando relacionamentos atualizados..." to suspend { downloadUpdatedPacienteEspecialidades() },
                "Finalizando sincronização..." to suspend { Result.success(Unit) }
            )

            for ((message, operation) in steps) {
                Log.d(TAG, message)
                updateSyncState(message, currentStep++, TOTAL_SYNC_STEPS)
                emit(_syncState.value)

                val result = operation()
                if (result.isFailure && currentStep <= 3) { // Falha crítica nas 3 primeiras etapas
                    emitErrorState(result.exceptionOrNull()?.message ?: "Erro na sincronização")
                    return@flow
                } else if (result.isFailure) {
                    Log.w(TAG, "Falha não crítica: ${result.exceptionOrNull()?.message}")
                }
            }

            emitSuccessState("Sincronização concluída com sucesso!", TOTAL_SYNC_STEPS)
            updateLastSyncTimestamp(System.currentTimeMillis())

        } catch (e: Exception) {
            Log.e(TAG, "Erro inesperado durante sincronização", e)
            emitErrorState(e.message ?: "Erro inesperado durante a sincronização")
        }
    }

    // ==================== SINCRONIZAÇÃO ESPECÍFICA ====================

    override suspend fun startSyncPacientes(): Flow<SyncState> = flow {
        updateSyncState("Sincronizando pacientes...", isLoading = true)
        emit(_syncState.value)

        try {
            if (!isNetworkAvailable()) {
                emitErrorState("Sem conexão com a internet")
                return@flow
            }

            // Upload primeiro, depois download
            uploadPendingPacientes()
            val downloadResult = downloadUpdatedPacientes()

            if (downloadResult.isSuccess) {
                emitSuccessState("Pacientes sincronizados com sucesso!")
            } else {
                emitErrorState(downloadResult.exceptionOrNull()?.message ?: "Erro ao sincronizar pacientes")
            }
        } catch (e: Exception) {
            emitErrorState(e.message ?: "Erro inesperado")
        }
    }

    override suspend fun startSyncEspecialidades(): Flow<SyncState> = flow {
        updateSyncState("Sincronizando especialidades...", isLoading = true)
        emit(_syncState.value)

        try {
            val result = syncEspecialidades()
            if (result.isSuccess) {
                emitSuccessState("Especialidades sincronizadas com sucesso!")
            } else {
                emitErrorState(result.exceptionOrNull()?.message ?: "Erro ao sincronizar especialidades")
            }
        } catch (e: Exception) {
            emitErrorState(e.message ?: "Erro inesperado")
        }
    }

    // ==================== SINCRONIZAÇÃO DE ESPECIALIDADES ====================

    override suspend fun syncEspecialidades(): Result<Unit> {
        return try {
            Log.d(TAG, "Iniciando syncEspecialidades")

            if (!isNetworkAndServerAvailable()) {
                return Result.failure(Exception("Conectividade não disponível"))
            }

            val response = apiService.getAllEspecialidades()
            if (!response.isSuccessful) {
                return Result.failure(Exception("Erro HTTP: ${response.code()} - ${response.message()}"))
            }

            val apiResponse = response.body()
            if (apiResponse?.success != true) {
                return Result.failure(Exception(apiResponse?.error ?: "Resposta da API indica falha"))
            }

            val especialidadesDto = apiResponse.data ?: emptyList()
            Log.d(TAG, "Recebidas ${especialidadesDto.size} especialidades do servidor")

            processarEspecialidadesDoServidor(especialidadesDto)
            Log.d(TAG, "Sincronização de especialidades concluída com sucesso")

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Erro em syncEspecialidades", e)
            Result.failure(e)
        }
    }

    private suspend fun processarEspecialidadesDoServidor(especialidadesDto: List<com.example.projeto_ibg3.data.remote.dto.EspecialidadeDto>) {
        if (especialidadesDto.isEmpty()) {
            Log.w(TAG, "Nenhuma especialidade retornada do servidor")
            return
        }

        val especialidadesEntity = especialidadesDto.toEntityList(
            deviceId = "default_device",
            syncStatus = SyncStatus.SYNCED
        )

        especialidadesEntity.forEach { entity ->
            processarEspecialidadeIndividual(entity)
        }

        logEspecialidadesFinais()
    }

    private suspend fun processarEspecialidadeIndividual(entity: com.example.projeto_ibg3.data.local.database.entities.EspecialidadeEntity) {
        if (entity.serverId == null || entity.localId.isBlank()) {
            Log.w(TAG, "ServerId nulo ou LocalId vazio para especialidade: ${entity.nome} - Ignorando...")
            return
        }

        val existingByServerId = especialidadeDao.getEspecialidadeByServerId(entity.serverId)

        when {
            existingByServerId == null -> {
                handleNovaEspecialidade(entity)
            }
            existingByServerId.updatedAt < entity.updatedAt -> {
                handleAtualizacaoEspecialidade(entity, existingByServerId)
            }
            else -> {
                Log.d(TAG, "Especialidade já está atualizada: ${entity.nome}")
            }
        }
    }

    private suspend fun handleNovaEspecialidade(entity: com.example.projeto_ibg3.data.local.database.entities.EspecialidadeEntity) {
        val existingByName = especialidadeDao.getEspecialidadeByName(entity.nome)

        if (existingByName != null && existingByName.serverId == null) {
            // Atualizar especialidade local existente
            val updatedEntity = existingByName.copy(
                serverId = entity.serverId,
                syncStatus = SyncStatus.SYNCED,
                updatedAt = entity.updatedAt,
                lastSyncTimestamp = System.currentTimeMillis()
            )
            especialidadeDao.updateEspecialidade(updatedEntity)
            Log.d(TAG, "Atualizada especialidade local existente: ${entity.nome}")
        } else {
            // Inserir nova especialidade
            especialidadeDao.insertEspecialidade(entity)
            Log.d(TAG, "Inserida nova especialidade: ${entity.nome}")
        }
    }

    private suspend fun handleAtualizacaoEspecialidade(
        entity: com.example.projeto_ibg3.data.local.database.entities.EspecialidadeEntity,
        existing: com.example.projeto_ibg3.data.local.database.entities.EspecialidadeEntity
    ) {
        val updatedEntity = entity.copy(localId = existing.localId)
        especialidadeDao.updateEspecialidade(updatedEntity)
        Log.d(TAG, "Atualizada especialidade: ${entity.nome}")
    }

    private suspend fun logEspecialidadesFinais() {
        val finalEspecialidades = especialidadeDao.getAllEspecialidadesList()
        Log.d(TAG, "Especialidades após sync: ${finalEspecialidades.size}")
        finalEspecialidades.forEach { esp ->
            Log.d(TAG, "Final DB: ${esp.nome} - LocalId: '${esp.localId}' - ServerId: ${esp.serverId}")
        }
    }

    // ==================== SINCRONIZAÇÃO DE PACIENTES ====================

    override suspend fun syncPacientes(): Result<Unit> {
        return try {
            Log.d(TAG, "Iniciando syncPacientes")

            if (!isNetworkAndServerAvailable()) {
                return Result.failure(Exception("Conectividade não disponível"))
            }

            // Upload primeiro, depois download
            val uploadResult = uploadPendingPacientes()
            if (uploadResult.isFailure) {
                Log.w(TAG, "Falha no upload de pacientes: ${uploadResult.exceptionOrNull()?.message}")
            }

            val downloadResult = downloadUpdatedPacientes()
            if (downloadResult.isFailure) {
                Log.e(TAG, "Falha no download de pacientes: ${downloadResult.exceptionOrNull()?.message}")
                return downloadResult
            }

            Log.d(TAG, "syncPacientes concluído com sucesso")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Erro em syncPacientes", e)
            Result.failure(e)
        }
    }

    // ==================== UPLOAD DE PACIENTES ====================

    private suspend fun uploadPendingPacientes(): Result<Unit> {
        return try {
            Log.d(TAG, "=== INICIANDO UPLOAD DE PACIENTES PENDENTES ===")

            val pendingPacientes = getPacientesPendentes()
            if (pendingPacientes.isEmpty()) {
                Log.d(TAG, "Nenhum paciente pendente para upload")
                return Result.success(Unit)
            }

            val operacoes = categorizarPacientesPorOperacao(pendingPacientes)
            executarOperacoesPacientes(operacoes)

            Log.d(TAG, "✅ Upload de pacientes concluído")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no upload de pacientes", e)
            Result.failure(e)
        }
    }

    private suspend fun getPacientesPendentes(): List<PacienteEntity> {
        val statusPendentes = listOf(
            SyncStatus.PENDING_UPLOAD,
            SyncStatus.PENDING_DELETE,
            SyncStatus.UPLOAD_FAILED,
            SyncStatus.DELETE_FAILED
        )

        val pacientes = pacienteDao.getItemsNeedingSync(statusPendentes)
        Log.d(TAG, "Encontrados ${pacientes.size} pacientes para sincronizar")

        logPacientesPendentesDetalhado(pacientes)
        return pacientes
    }

    private fun categorizarPacientesPorOperacao(pacientes: List<PacienteEntity>): Map<String, List<PacienteEntity>> {
        val forUpload = pacientes.filter {
            it.syncStatus == SyncStatus.PENDING_UPLOAD && it.serverId == null && !it.isDeleted
        }
        val forUpdate = pacientes.filter {
            it.syncStatus == SyncStatus.PENDING_UPLOAD && it.serverId != null && !it.isDeleted
        }
        val forDelete = pacientes.filter {
            (it.syncStatus == SyncStatus.PENDING_DELETE || it.isDeleted) && it.serverId != null
        }

        Log.d(TAG, "📊 SEPARAÇÃO POR OPERAÇÃO:")
        Log.d(TAG, "  - Para criar: ${forUpload.size}")
        Log.d(TAG, "  - Para atualizar: ${forUpdate.size}")
        Log.d(TAG, "  - Para deletar: ${forDelete.size}")

        return mapOf(
            "create" to forUpload,
            "update" to forUpdate,
            "delete" to forDelete
        )
    }

    private suspend fun executarOperacoesPacientes(operacoes: Map<String, List<PacienteEntity>>) {
        operacoes["create"]?.takeIf { it.isNotEmpty() }?.let {
            Log.d(TAG, "🆕 Processando criações...")
            processarCriacaoPacientes(it)
        }

        operacoes["update"]?.takeIf { it.isNotEmpty() }?.let {
            Log.d(TAG, "🔄 Processando atualizações...")
            processarAtualizacaoPacientes(it)
        }

        operacoes["delete"]?.takeIf { it.isNotEmpty() }?.let {
            Log.d(TAG, "🗑️ Processando deleções...")
            processarDelecaoPacientes(it)
        }
    }

    private suspend fun processarCriacaoPacientes(pacientes: List<PacienteEntity>) {
        pacientes.chunked(BATCH_SIZE).forEach { batch ->
            try {
                val pacientesDto = batch.map { it.toPacienteDto() }
                val response = apiService.createPacientesBatch(pacientesDto)

                if (response.isSuccessful && response.body()?.success == true) {
                    handleCriacaoSucesso(batch, response.body()?.data ?: emptyList())
                } else {
                    handleCriacaoFalha(batch, response.message())
                }
            } catch (e: Exception) {
                handleCriacaoExcecao(batch, e)
            }
        }
    }

    private suspend fun processarAtualizacaoPacientes(pacientes: List<PacienteEntity>) {
        Log.d(TAG, "Processando ${pacientes.size} pacientes para atualização")

        pacientes.forEach { entity ->
            try {
                val resultado = tentarAtualizarPaciente(entity)
                processarResultadoAtualizacao(entity, resultado)
            } catch (e: Exception) {
                handleAtualizacaoExcecao(entity, e)
            }
        }
    }

    private suspend fun processarDelecaoPacientes(pacientes: List<PacienteEntity>) {
        pacientes.forEach { entity ->
            try {
                val response = apiService.deletePaciente(entity.serverId!!)

                if (response.isSuccessful) {
                    pacienteDao.deletePacientePermanently(entity.localId)
                    Log.d(TAG, "Paciente ${entity.localId} deletado com sucesso")
                } else {
                    pacienteDao.updateSyncStatus(entity.localId, SyncStatus.DELETE_FAILED)
                    Log.e(TAG, "Falha na deleção do paciente ${entity.localId}: ${response.message()}")
                }
            } catch (e: Exception) {
                pacienteDao.incrementSyncAttempts(entity.localId, System.currentTimeMillis(), e.message)
                Log.e(TAG, "Erro na deleção do paciente ${entity.localId}", e)
            }
        }
    }

    // ==================== DOWNLOAD DE PACIENTES ====================

    private suspend fun downloadUpdatedPacientes(): Result<Unit> {
        return try {
            Log.d(TAG, "Iniciando download de pacientes atualizados")

            val lastSync = getLastSyncTimestamp()
            val response = apiService.getUpdatedPacientes(lastSync)

            if (!response.isSuccessful || response.body()?.success != true) {
                val error = response.body()?.error ?: "Erro HTTP: ${response.code()}"
                Log.e(TAG, "Erro no download de pacientes: $error")
                return Result.failure(Exception(error))
            }

            val pacientesDto = response.body()?.data ?: emptyList()
            Log.d(TAG, "Recebidos ${pacientesDto.size} pacientes do servidor")

            if (pacientesDto.isNotEmpty()) {
                processarPacientesDoServidor(pacientesDto)
            }

            Log.d(TAG, "Download de pacientes concluído")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Erro no download de pacientes", e)
            Result.failure(e)
        }
    }

    private suspend fun processarPacientesDoServidor(pacientesDto: List<PacienteDto>) {
        pacientesDto.forEach { dto ->
            try {
                Log.d(TAG, "Processando paciente: ${dto.nome} - Idade DTO: ${dto.idade}")

                val existingByServerId = pacienteDao.getPacienteByServerId(dto.serverId ?: 0L)
                val existingByCpf = dto.cpf?.let { pacienteDao.getPacienteByCpf(it) }

                when {
                    existingByServerId != null -> handlePacienteExistentePorServerId(dto, existingByServerId)
                    existingByCpf != null && existingByCpf.serverId == null -> handlePacienteLocalSincronizar(dto, existingByCpf)
                    else -> handleNovoPacienteDoServidor(dto)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao processar paciente ${dto.nome}", e)
            }
        }
    }

    // ==================== SINCRONIZAÇÃO DE RELACIONAMENTOS ====================

    private suspend fun uploadPendingPacienteEspecialidades(): Result<Unit> {
        return try {
            Log.d(TAG, "Iniciando upload de relacionamentos pendentes")

            val pendingRelations = pacienteEspecialidadeDao.getItemsNeedingSync()
            Log.d(TAG, "Encontrados ${pendingRelations.size} relacionamentos para sincronizar")

            if (pendingRelations.isEmpty()) {
                Log.d(TAG, "Nenhum relacionamento pendente para upload")
                return Result.success(Unit)
            }

            val forCreate = pendingRelations.filter {
                it.syncStatus == SyncStatus.PENDING_UPLOAD && !it.isDeleted
            }
            val forDelete = pendingRelations.filter {
                it.syncStatus == SyncStatus.PENDING_DELETE
            }

            Log.d(TAG, "Para criar: ${forCreate.size}, Para deletar: ${forDelete.size}")

            if (forCreate.isNotEmpty()) {
                processarCriacaoRelacionamentos(forCreate)
            }

            if (forDelete.isNotEmpty()) {
                processarDelecaoRelacionamentos(forDelete)
            }

            Log.d(TAG, "Upload de relacionamentos concluído")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Erro no upload de relacionamentos", e)
            Result.failure(e)
        }
    }

    private suspend fun downloadUpdatedPacienteEspecialidades(): Result<Unit> {
        return try {
            Log.d(TAG, "Iniciando download de relacionamentos atualizados")

            val lastSync = getLastSyncTimestamp()
            val response = apiService.getUpdatedPacienteEspecialidades(lastSync)

            if (!response.isSuccessful) {
                val error = "Erro HTTP: ${response.code()} - ${response.message()}"
                Log.e(TAG, "Erro no download de relacionamentos: $error")
                return Result.failure(Exception(error))
            }

            val apiResponse = response.body()
            if (apiResponse?.success != true) {
                val error = apiResponse?.error ?: "API retornou success=false"
                Log.e(TAG, "Erro no download de relacionamentos: $error")
                return Result.failure(Exception(error))
            }

            val relationsDto = apiResponse.data ?: emptyList()
            Log.d(TAG, "Recebidos ${relationsDto.size} relacionamentos do servidor")

            if (relationsDto.isNotEmpty()) {
                processarRelacionamentosDoServidor(relationsDto)
            }

            Log.d(TAG, "Download de relacionamentos concluído")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Erro no download de relacionamentos", e)
            Result.failure(e)
        }
    }

    // ==================== MÉTODOS AUXILIARES DE REDE ====================

    private suspend fun isNetworkAvailable(): Boolean {
        return networkManager.checkConnection()
    }

    private suspend fun isNetworkAndServerAvailable(): Boolean {
        if (!networkManager.checkConnection()) {
            Log.e(TAG, "Sem conexão com internet")
            return false
        }

        if (!networkManager.testServerConnection()) {
            Log.e(TAG, "Servidor não acessível")
            return false
        }

        return true
    }

    // ==================== MÉTODOS AUXILIARES DE ESTADO ====================

    private fun updateSyncState(
        message: String,
        currentStep: Int = 0,
        totalSteps: Int = 0,
        isLoading: Boolean = false
    ) {
        _syncState.value = _syncState.value.copy(
            isLoading = isLoading,
            message = message,
            totalItems = totalSteps,
            processedItems = currentStep,
            error = null
        )
    }

    private suspend fun emitErrorState(message: String) {
        val errorState = SyncState(
            isLoading = false,
            error = message,
            message = "Erro na sincronização"
        )
        _syncState.value = errorState
        _syncState.emit(errorState)
    }

    private suspend fun emitSuccessState(message: String, processedItems: Int = 0) {
        val successState = SyncState(
            isLoading = false,
            message = message,
            lastSyncTime = System.currentTimeMillis(),
            totalItems = processedItems,
            processedItems = processedItems
        )
        _syncState.value = successState
        _syncState.emit(successState)
    }

    // ==================== MÉTODOS AUXILIARES DE LOGGING ====================

    private fun logPacientesPendentesDetalhado(pacientes: List<PacienteEntity>) {
        pacientes.forEachIndexed { index, paciente ->
            Log.d(TAG, "Paciente $index:")
            Log.d(TAG, "  - Nome: ${paciente.nome}")
            Log.d(TAG, "  - LocalId: ${paciente.localId}")
            Log.d(TAG, "  - ServerId: ${paciente.serverId}")
            Log.d(TAG, "  - SyncStatus: ${paciente.syncStatus}")
            Log.d(TAG, "  - IsDeleted: ${paciente.isDeleted}")
            Log.d(TAG, "  - UpdatedAt: ${paciente.updatedAt}")
        }
    }

    // ==================== IMPLEMENTAÇÕES DOS MÉTODOS DA INTERFACE ====================

    override suspend fun syncAll(): Result<Unit> {
        return try {
            val especialidadesResult = syncEspecialidades()
            if (especialidadesResult.isFailure) return especialidadesResult

            val pacientesResult = syncPacientes()
            if (pacientesResult.isFailure) return pacientesResult

            val uploadRelacionamentosResult = uploadPendingPacienteEspecialidades()
            if (uploadRelacionamentosResult.isFailure) {
                Log.w(TAG, "Falha no upload de relacionamentos: ${uploadRelacionamentosResult.exceptionOrNull()?.message}")
            }

            val downloadRelacionamentosResult = downloadUpdatedPacienteEspecialidades()
            if (downloadRelacionamentosResult.isFailure) {
                Log.w(TAG, "Falha no download de relacionamentos: ${downloadRelacionamentosResult.exceptionOrNull()?.message}")
            }

            updateLastSyncTimestamp(System.currentTimeMillis())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun hasPendingChanges(): Boolean {
        val pacientesPending = pacienteDao.getUnsyncedCount() > 0
        val especialidadesPending = especialidadeRepository.hasPendingChanges()
        val relacionamentosPending = pacienteEspecialidadeDao.countPendingUploads() > 0

        return pacientesPending || especialidadesPending || relacionamentosPending
    }

    override suspend fun getLastSyncTimestamp(): Long {
        // Implementar usando SharedPreferences ou tabela de metadados
        return 0L
    }

    override suspend fun updateLastSyncTimestamp(timestamp: Long) {
        // Implementar salvamento do timestamp da última sincronização
    }

    override fun clearError() {
        _syncState.value = _syncState.value.copy(error = null)
    }

    // ==================== MÉTODOS ESPECÍFICOS DE SINCRONIZAÇÃO ====================

    override suspend fun syncPacienteEspecialidadesOnly(): Result<Unit> {
        return try {
            Log.d(TAG, "Iniciando sincronização apenas de relacionamentos")

            if (!networkManager.checkConnection()) {
                return Result.failure(Exception("Sem conexão com internet"))
            }

            val uploadResult = uploadPendingPacienteEspecialidades()
            if (uploadResult.isFailure) {
                Log.w(TAG, "Falha no upload de relacionamentos: ${uploadResult.exceptionOrNull()?.message}")
            }

            val downloadResult = downloadUpdatedPacienteEspecialidades()
            if (downloadResult.isFailure) {
                Log.w(TAG, "Falha no download de relacionamentos: ${downloadResult.exceptionOrNull()?.message}")
                return downloadResult
            }

            Log.d(TAG, "Sincronização de relacionamentos concluída")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Erro na sincronização de relacionamentos", e)
            Result.failure(e)
        }
    }

    override suspend fun uploadPacienteEspecialidadesPending(): Result<Unit> {
        return uploadPendingPacienteEspecialidades()
    }

    override suspend fun downloadPacienteEspecialidadesUpdated(): Result<Unit> {
        return downloadUpdatedPacienteEspecialidades()
    }

    override suspend fun syncPacientesUpdated(): Result<Unit> {
        return try {
            Log.d(TAG, "Iniciando sincronização de pacientes atualizados...")

            if (!networkManager.checkConnection()) {
                return Result.failure(Exception("Sem conexão com internet"))
            }

            val pacientesParaAtualizar = pacienteDao.getPacientesParaAtualizar(SyncStatus.PENDING_UPLOAD)
            Log.d(TAG, "Encontrados ${pacientesParaAtualizar.size} pacientes para atualizar")

            if (pacientesParaAtualizar.isEmpty()) {
                Log.d(TAG, "Nenhum paciente para atualizar")
                return Result.success(Unit)
            }

            processarAtualizacaoPacientes(pacientesParaAtualizar)
            Log.d(TAG, "Sincronização de pacientes atualizados concluída")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Erro na sincronização de pacientes atualizados", e)
            Result.failure(e)
        }
    }

    override suspend fun syncNovosPacientes(): Result<Unit> {
        return try {
            Log.d(TAG, "Iniciando sincronização de novos pacientes...")

            if (!networkManager.checkConnection()) {
                return Result.failure(Exception("Sem conexão com internet"))
            }

            val novosPacientes = pacienteDao.getNovosPacientes(SyncStatus.PENDING_UPLOAD)
            Log.d(TAG, "Encontrados ${novosPacientes.size} novos pacientes para sincronizar")

            if (novosPacientes.isEmpty()) {
                Log.d(TAG, "Nenhum novo paciente para sincronizar")
                return Result.success(Unit)
            }

            processarCriacaoPacientes(novosPacientes)
            Log.d(TAG, "Sincronização de novos pacientes concluída")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Erro na sincronização de novos pacientes", e)
            Result.failure(e)
        }
    }

    override suspend fun syncAllPendingPacientes(): Result<Unit> {
        return try {
            Log.d(TAG, "Iniciando sincronização de todos os pacientes pendentes...")
            val result = uploadPendingPacientes()

            if (result.isSuccess) {
                Log.d(TAG, "Sincronização de todos os pacientes pendentes concluída")
            } else {
                Log.e(TAG, "Erro na sincronização de pacientes pendentes: ${result.exceptionOrNull()?.message}")
            }

            result

        } catch (e: Exception) {
            Log.e(TAG, "Erro na sincronização de todos os pacientes pendentes", e)
            Result.failure(e)
        }
    }

    // ==================== MÉTODOS AUXILIARES DE PROCESSAMENTO ====================

    private suspend fun handleCriacaoSucesso(batch: List<PacienteEntity>, createdPacientes: List<PacienteDto>) {
        createdPacientes.forEachIndexed { index, createdDto ->
            val originalEntity = batch[index]
            pacienteDao.updateSyncStatusAndServerId(
                originalEntity.localId,
                SyncStatus.SYNCED,
                createdDto.serverId ?: 0L
            )
        }
        Log.d(TAG, "Batch de ${batch.size} pacientes criado com sucesso")
    }

    private suspend fun handleCriacaoFalha(batch: List<PacienteEntity>, errorMessage: String) {
        batch.forEach { entity ->
            pacienteDao.updateSyncStatus(entity.localId, SyncStatus.UPLOAD_FAILED)
        }
        Log.e(TAG, "Falha na criação em batch: $errorMessage")
    }

    private suspend fun handleCriacaoExcecao(batch: List<PacienteEntity>, exception: Exception) {
        batch.forEach { entity ->
            pacienteDao.incrementSyncAttempts(entity.localId, System.currentTimeMillis(), exception.message)
        }
        Log.e(TAG, "Erro na criação em batch", exception)
    }

    private suspend fun tentarAtualizarPaciente(entity: PacienteEntity): Result<Any> {
        Log.d(TAG, "Atualizando paciente: ${entity.nome} (serverId: ${entity.serverId})")

        val pacienteDto = entity.toPacienteDto()
        val response = apiService.updatePaciente(entity.serverId!!, pacienteDto)

        Log.d(TAG, "Response code: ${response.code()}, isSuccessful: ${response.isSuccessful()}")

        return if (response.isSuccessful) {
            val responseBody = response.body()
            if (responseBody?.success == true) {
                Result.success(responseBody)
            } else {
                Result.failure(Exception(responseBody?.error ?: "Resposta indica falha"))
            }
        } else {
            val errorBody = response.errorBody()?.string()
            Result.failure(Exception("HTTP ${response.code()}: ${response.message()} - $errorBody"))
        }
    }

    private suspend fun processarResultadoAtualizacao(entity: PacienteEntity, resultado: Result<Any>) {
        if (resultado.isSuccess) {
            pacienteDao.updateSyncStatus(entity.localId, SyncStatus.SYNCED)
            Log.d(TAG, "Paciente ${entity.localId} atualizado com sucesso")
        } else {
            pacienteDao.updateSyncStatus(entity.localId, SyncStatus.UPLOAD_FAILED)
            Log.e(TAG, "Falha na atualização do paciente ${entity.localId}: ${resultado.exceptionOrNull()?.message}")
        }
    }

    private suspend fun handleAtualizacaoExcecao(entity: PacienteEntity, exception: Exception) {
        Log.e(TAG, "Erro na atualização do paciente ${entity.localId}", exception)
        pacienteDao.incrementSyncAttempts(entity.localId, System.currentTimeMillis(), exception.message)
    }

    private suspend fun handlePacienteExistentePorServerId(dto: PacienteDto, existing: PacienteEntity) {
        if (dto.updatedAt.toDateLong()!! > existing.updatedAt) {
            val updatedEntity = dto.toPacienteEntity().copy(
                localId = existing.localId,
                syncStatus = SyncStatus.SYNCED,
                lastSyncTimestamp = System.currentTimeMillis()
            )
            Log.d(TAG, "Entity atualizada: ${updatedEntity.nome} - Idade Entity: ${updatedEntity.idade}")
            pacienteDao.updatePaciente(updatedEntity)
            Log.d(TAG, "Paciente atualizado: ${dto.nome}")
        }
    }

    private suspend fun handlePacienteLocalSincronizar(dto: PacienteDto, existing: PacienteEntity) {
        val updatedEntity = existing.copy(
            serverId = dto.serverId,
            syncStatus = SyncStatus.SYNCED,
            updatedAt = dto.updatedAt.toDateLong(),
            lastSyncTimestamp = System.currentTimeMillis()
        )
        pacienteDao.updatePaciente(updatedEntity)
        Log.d(TAG, "Paciente local sincronizado: ${dto.nome}")
    }

    private suspend fun handleNovoPacienteDoServidor(dto: PacienteDto) {
        val newEntity = dto.toPacienteEntity().copy(
            syncStatus = SyncStatus.SYNCED,
            lastSyncTimestamp = System.currentTimeMillis()
        )
        Log.d(TAG, "Nova entity criada: ${newEntity.nome} - Idade Entity: ${newEntity.idade}")
        pacienteDao.insertPaciente(newEntity)
        Log.d(TAG, "Novo paciente inserido: ${dto.nome}")
    }

    private suspend fun processarCriacaoRelacionamentos(relations: List<com.example.projeto_ibg3.data.local.database.entities.PacienteEspecialidadeEntity>) {
        relations.chunked(BATCH_SIZE).forEach { batch ->
            try {
                val relationsDto = batch.mapNotNull { entity ->
                    criarRelacionamentoDto(entity)
                }

                if (relationsDto.isNotEmpty()) {
                    val response = apiService.syncPacienteEspecialidades(relationsDto)

                    if (response.isSuccessful && response.body()?.success == true) {
                        marcarRelacionamentosComoSincronizados(batch)
                        Log.d(TAG, "Batch de ${batch.size} relacionamentos criado com sucesso")
                    } else {
                        marcarRelacionamentosComoFalha(batch)
                        Log.e(TAG, "Falha na criação em batch de relacionamentos: ${response.message()}")
                    }
                }
            } catch (e: Exception) {
                incrementarTentativasRelacionamentos(batch, e)
                Log.e(TAG, "Erro na criação em batch de relacionamentos", e)
            }
        }
    }

    private suspend fun processarDelecaoRelacionamentos(relations: List<com.example.projeto_ibg3.data.local.database.entities.PacienteEspecialidadeEntity>) {
        relations.forEach { entity ->
            try {
                if (entity.pacienteServerId != null && entity.especialidadeServerId != null) {
                    val response = apiService.removeEspecialidadeFromPaciente(
                        entity.pacienteServerId!!,
                        entity.especialidadeServerId!!
                    )

                    if (response.isSuccessful) {
                        pacienteEspecialidadeDao.deletePermanently(
                            entity.pacienteLocalId,
                            entity.especialidadeLocalId
                        )
                        Log.d(TAG, "Relacionamento deletado com sucesso: ${entity.relationId}")
                    } else {
                        pacienteEspecialidadeDao.updateSyncStatus(
                            entity.pacienteLocalId,
                            entity.especialidadeLocalId,
                            SyncStatus.DELETE_FAILED
                        )
                        Log.e(TAG, "Falha na deleção do relacionamento ${entity.relationId}: ${response.message()}")
                    }
                } else {
                    Log.w(TAG, "Relacionamento ignorado na deleção - serverIds faltando: ${entity.relationId}")
                }
            } catch (e: Exception) {
                pacienteEspecialidadeDao.incrementSyncAttempts(
                    entity.pacienteLocalId,
                    entity.especialidadeLocalId,
                    System.currentTimeMillis(),
                    e.message
                )
                Log.e(TAG, "Erro na deleção do relacionamento ${entity.relationId}", e)
            }
        }
    }

    private suspend fun processarRelacionamentosDoServidor(relationsDto: List<PacienteEspecialidadeDTO>) {
        Log.d(TAG, "Processando ${relationsDto.size} relacionamentos do servidor")

        relationsDto.forEach { dto ->
            try {
                processarRelacionamentoIndividual(dto)
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao processar relacionamento do servidor - PacienteId: ${dto.pacienteServerId}, EspecialidadeId: ${dto.especialidadeServerId}", e)
            }
        }

        Log.d(TAG, "Processamento de relacionamentos concluído")
    }

    private suspend fun processarRelacionamentoIndividual(dto: PacienteEspecialidadeDTO) {
        if (dto.pacienteServerId == null || dto.especialidadeServerId == null) {
            Log.w(TAG, "IDs nulos no DTO: pacienteId=${dto.pacienteServerId}, especialidadeId=${dto.especialidadeServerId}")
            return
        }

        Log.d(TAG, "Processando relacionamento: PacienteServerId=${dto.pacienteServerId}, EspecialidadeServerId=${dto.especialidadeServerId}")

        val paciente = pacienteDao.getPacienteByServerId(dto.pacienteServerId!!)
        val especialidade = especialidadeDao.getEspecialidadeByServerId(dto.especialidadeServerId!!)

        Log.d(TAG, "Paciente encontrado: ${paciente?.let { "${it.nome} (localId: ${it.localId})" } ?: "null"}")
        Log.d(TAG, "Especialidade encontrada: ${especialidade?.let { "${it.nome} (localId: ${it.localId})" } ?: "null"}")

        if (paciente != null && especialidade != null) {
            processarRelacionamentoValido(dto, paciente, especialidade)
        } else {
            logRelacionamentoInvalido(dto)
        }
    }

    private suspend fun processarRelacionamentoValido(
        dto: PacienteEspecialidadeDTO,
        paciente: PacienteEntity,
        especialidade: com.example.projeto_ibg3.data.local.database.entities.EspecialidadeEntity
    ) {
        if (dto.isDeleted) {
            pacienteEspecialidadeDao.deletePermanently(
                paciente.localId,
                especialidade.localId
            )
            Log.d(TAG, "Relacionamento removido: ${paciente.localId}_${especialidade.localId}")
        } else {
            val existing = pacienteEspecialidadeDao.getById(
                paciente.localId,
                especialidade.localId
            )

            if (existing == null) {
                criarNovoRelacionamento(dto, paciente, especialidade)
            } else {
                atualizarRelacionamentoExistente(dto, existing)
            }
        }
    }

    private suspend fun criarNovoRelacionamento(
        dto: PacienteEspecialidadeDTO,
        paciente: PacienteEntity,
        especialidade: com.example.projeto_ibg3.data.local.database.entities.EspecialidadeEntity
    ) {
        val newRelation = dto.toEntity(
            pacienteLocalId = paciente.localId,
            especialidadeLocalId = especialidade.localId,
            deviceId = "server",
            syncStatus = SyncStatus.SYNCED
        )

        pacienteEspecialidadeDao.insert(newRelation)
        Log.d(TAG, "Novo relacionamento inserido: ${paciente.localId}_${especialidade.localId} - Data: ${dto.dataAtendimento}")
    }

    private suspend fun atualizarRelacionamentoExistente(
        dto: PacienteEspecialidadeDTO,
        existing: com.example.projeto_ibg3.data.local.database.entities.PacienteEspecialidadeEntity
    ) {
        val dtoUpdatedAt = dto.updatedAt.toIsoDateLong()
        if (dtoUpdatedAt > existing.updatedAt) {
            val updatedRelation = existing.copy(
                dataAtendimento = dto.dataAtendimento?.toDateLong(),
                pacienteServerId = dto.pacienteServerId,
                especialidadeServerId = dto.especialidadeServerId,
                syncStatus = SyncStatus.SYNCED,
                updatedAt = dtoUpdatedAt,
                lastSyncTimestamp = System.currentTimeMillis(),
                isDeleted = false
            )
            pacienteEspecialidadeDao.update(updatedRelation)
            Log.d(TAG, "Relacionamento atualizado: ${existing.pacienteLocalId}_${existing.especialidadeLocalId}")
        } else {
            Log.d(TAG, "Relacionamento já está atualizado: ${existing.pacienteLocalId}_${existing.especialidadeLocalId}")
        }
    }

    // ==================== MÉTODOS AUXILIARES DE RELACIONAMENTOS ====================

    private suspend fun criarRelacionamentoDto(entity: com.example.projeto_ibg3.data.local.database.entities.PacienteEspecialidadeEntity): PacienteEspecialidadeDTO? {
        val paciente = pacienteDao.getPacienteById(entity.pacienteLocalId)
        val especialidade = especialidadeDao.getEspecialidadeById(entity.especialidadeLocalId)

        return if (paciente?.serverId != null && especialidade?.serverId != null) {
            PacienteEspecialidadeDTO(
                pacienteServerId = paciente.serverId!!,
                especialidadeServerId = especialidade.serverId!!,
                pacienteLocalId = entity.pacienteLocalId,
                especialidadeLocalId = entity.especialidadeLocalId,
                dataAtendimento = entity.dataAtendimento?.let { dateFormat.format(Date(it)) },
                createdAt = dateTimeFormat.format(Date(entity.createdAt)),
                updatedAt = dateTimeFormat.format(Date(entity.updatedAt)),
                lastSyncTimestamp = entity.lastSyncTimestamp
            )
        } else {
            Log.w(TAG, "Relacionamento ignorado - serverIds faltando: paciente=${paciente?.serverId}, especialidade=${especialidade?.serverId}")
            null
        }
    }

    private suspend fun marcarRelacionamentosComoSincronizados(batch: List<com.example.projeto_ibg3.data.local.database.entities.PacienteEspecialidadeEntity>) {
        batch.forEach { entity ->
            pacienteEspecialidadeDao.updateSyncStatus(
                entity.pacienteLocalId,
                entity.especialidadeLocalId,
                SyncStatus.SYNCED
            )
        }
    }

    private suspend fun marcarRelacionamentosComoFalha(batch: List<com.example.projeto_ibg3.data.local.database.entities.PacienteEspecialidadeEntity>) {
        batch.forEach { entity ->
            pacienteEspecialidadeDao.updateSyncStatus(
                entity.pacienteLocalId,
                entity.especialidadeLocalId,
                SyncStatus.UPLOAD_FAILED
            )
        }
    }

    private suspend fun incrementarTentativasRelacionamentos(
        batch: List<com.example.projeto_ibg3.data.local.database.entities.PacienteEspecialidadeEntity>,
        exception: Exception
    ) {
        batch.forEach { entity ->
            pacienteEspecialidadeDao.incrementSyncAttempts(
                entity.pacienteLocalId,
                entity.especialidadeLocalId,
                System.currentTimeMillis(),
                exception.message
            )
        }
    }

    private suspend fun logRelacionamentoInvalido(dto: PacienteEspecialidadeDTO) {
        Log.w(TAG, "Relacionamento ignorado - paciente ou especialidade não encontrados: pacienteId=${dto.pacienteServerId}, especialidadeId=${dto.especialidadeServerId}")

        // Log detalhado para debug
        val allPacientes = pacienteDao.getAllPacientesList()
        val allEspecialidades = especialidadeDao.getAllEspecialidadesList()

        Log.d(TAG, "Pacientes disponíveis:")
        allPacientes.forEach { p: PacienteEntity ->
            Log.d(TAG, "  - ${p.nome} (localId: ${p.localId}, serverId: ${p.serverId})")
        }

        Log.d(TAG, "Especialidades disponíveis:")
        allEspecialidades.forEach { e ->
            Log.d(TAG, "  - ${e.nome} (localId: ${e.localId}, serverId: ${e.serverId})")
        }
    }

    // ==================== MÉTODOS DE COMPATIBILIDADE ====================

    fun syncPacientes(lastSyncTimestamp: Long): Flow<ApiResult<List<PacienteDto>>> = flow {
        emit(ApiResult.Loading())

        try {
            if (!networkManager.checkConnection()) {
                emit(ApiResult.Error("Sem conexão com a internet"))
                return@flow
            }

            val response = apiService.getUpdatedPacientes(lastSyncTimestamp)

            if (response.isSuccessful) {
                val apiResponse = response.body()
                if (apiResponse?.success == true) {
                    emit(ApiResult.Success(apiResponse.data ?: emptyList()))
                } else {
                    emit(ApiResult.Error(apiResponse?.error ?: "Erro desconhecido"))
                }
            } else {
                emit(ApiResult.Error("Erro HTTP: ${response.code()}"))
            }
        } catch (e: Exception) {
            emit(ApiResult.Error(e.toString()))
        }
    }

    suspend fun createPaciente(paciente: PacienteDto): ApiResult<PacienteDto> {
        return try {
            val response = apiService.createPaciente(paciente)
            if (response.isSuccessful) {
                val apiResponse = response.body()
                if (apiResponse?.success == true && apiResponse.data != null) {
                    ApiResult.Success(apiResponse.data)
                } else {
                    ApiResult.Error(apiResponse?.error ?: "Erro ao criar paciente")
                }
            } else {
                ApiResult.Error("Erro HTTP: ${response.code()}")
            }
        } catch (e: Exception) {
            ApiResult.Error(e.toString())
        }
    }

    // ==================== MÉTODOS DE DEBUG ====================

    suspend fun debugPacientesPendentes() {
        try {
            val todosPendentes = pacienteDao.getPacientesBySyncStatus(SyncStatus.PENDING_UPLOAD)
            val novos = pacienteDao.getNovosPacientes(SyncStatus.PENDING_UPLOAD)
            val paraAtualizar = pacienteDao.getPacientesParaAtualizar(SyncStatus.PENDING_UPLOAD)

            Log.d(TAG, "=== DEBUG PACIENTES PENDENTES ===")
            Log.d(TAG, "Total pendentes: ${todosPendentes.size}")
            Log.d(TAG, "Novos (sem serverId): ${novos.size}")
            Log.d(TAG, "Para atualizar (com serverId): ${paraAtualizar.size}")

            Log.d(TAG, "NOVOS PACIENTES:")
            novos.forEach { p ->
                Log.d(TAG, "  - ${p.nome} (localId: ${p.localId}, serverId: ${p.serverId})")
            }

            Log.d(TAG, "PACIENTES PARA ATUALIZAR:")
            paraAtualizar.forEach { p ->
                Log.d(TAG, "  - ${p.nome} (localId: ${p.localId}, serverId: ${p.serverId})")
            }

            Log.d(TAG, "=== FIM DEBUG ===")

        } catch (e: Exception) {
            Log.e(TAG, "Erro no debug de pacientes pendentes", e)
        }
    }

    suspend fun forceSyncWithDetailedLogging(): Result<Unit> {
        return try {
            Log.d(TAG, "🚀 INICIANDO SINCRONIZAÇÃO FORÇADA COM LOGGING DETALHADO")

            debugPacientesPendentes()

            Log.d(TAG, "📡 Verificando conectividade...")
            if (!isNetworkAndServerAvailable()) {
                Log.e(TAG, "❌ Conectividade não disponível")
                return Result.failure(Exception("Conectividade não disponível"))
            }
            Log.d(TAG, "✅ Conectividade OK")

            val result = syncAllPendingPacientes()

            if (result.isSuccess) {
                Log.d(TAG, "🎉 SINCRONIZAÇÃO CONCLUÍDA COM SUCESSO")
            } else {
                Log.e(TAG, "💥 SINCRONIZAÇÃO FALHOU: ${result.exceptionOrNull()?.message}")
            }

            debugPacientesPendentes()
            result

        } catch (e: Exception) {
            Log.e(TAG, "💥 ERRO INESPERADO NA SINCRONIZAÇÃO FORÇADA", e)
            Result.failure(e)
        }
    }
}