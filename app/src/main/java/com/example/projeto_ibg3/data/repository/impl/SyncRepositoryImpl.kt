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
): SyncRepository {

    companion object {
        private const val TAG = "SyncRepository"
        private const val MAX_RETRY_ATTEMPTS = 3
        private const val BATCH_SIZE = 50
    }

    // Estados observáveis
    private val _syncState = MutableStateFlow(SyncState())
    override val syncState: Flow<SyncState> = _syncState.asStateFlow()

    private val _syncProgress = MutableStateFlow(SyncProgress())
    override val syncProgress: Flow<SyncProgress> = _syncProgress.asStateFlow()

    // ==================== SINCRONIZAÇÃO COMPLETA ====================

    override suspend fun startSync(): Flow<SyncState> = flow {
        _syncState.value = SyncState(
            isLoading = true,
            message = "Iniciando sincronização completa...",
            error = null
        )
        emit(_syncState.value)

        try {
            if (!networkManager.checkConnection()) {
                val errorState = createErrorState("Sem conexão com a internet")
                _syncState.value = errorState
                emit(errorState)
                return@flow
            }

            // Definir total de etapas
            val totalSteps = 6 // Especialidades, Pacientes, PacienteEspecialidade (upload e download para cada)
            var currentStep = 0

            // Etapa 1: Sincronizar Especialidades
            Log.d(TAG, "Iniciando sincronização de especialidades")
            updateSyncState("Sincronizando especialidades...", currentStep++, totalSteps)
            emit(_syncState.value)

            val especialidadesResult = syncEspecialidades()
            if (especialidadesResult.isFailure) {
                val errorState = createErrorState(
                    especialidadesResult.exceptionOrNull()?.message ?: "Erro ao sincronizar especialidades"
                )
                _syncState.value = errorState
                emit(errorState)
                return@flow
            }

            // Etapa 2: Upload Pacientes Pendentes
            Log.d(TAG, "Iniciando upload de pacientes pendentes")
            updateSyncState("Enviando pacientes pendentes...", currentStep++, totalSteps)
            emit(_syncState.value)

            val uploadPacientesResult = uploadPendingPacientes()
            if (uploadPacientesResult.isFailure) {
                Log.w(TAG, "Falha no upload de pacientes: ${uploadPacientesResult.exceptionOrNull()?.message}")
                // Continua mesmo com falha no upload
            }

            // Etapa 3: Download Pacientes Atualizados
            Log.d(TAG, "Iniciando download de pacientes atualizados")
            updateSyncState("Baixando pacientes atualizados...", currentStep++, totalSteps)
            emit(_syncState.value)

            val downloadPacientesResult = downloadUpdatedPacientes()
            if (downloadPacientesResult.isFailure) {
                val errorState = createErrorState(
                    downloadPacientesResult.exceptionOrNull()?.message ?: "Erro ao baixar pacientes"
                )
                _syncState.value = errorState
                emit(errorState)
                return@flow
            }

            // Etapa 4: Upload Relacionamentos Pendentes
            Log.d(TAG, "Iniciando upload de relacionamentos pendentes")
            updateSyncState("Enviando relacionamentos pendentes...", currentStep++, totalSteps)
            emit(_syncState.value)

            val uploadRelacionamentosResult = uploadPendingPacienteEspecialidades()
            if (uploadRelacionamentosResult.isFailure) {
                Log.w(TAG, "Falha no upload de relacionamentos: ${uploadRelacionamentosResult.exceptionOrNull()?.message}")
                // Continua mesmo com falha no upload
            }

            // Etapa 5: Download Relacionamentos Atualizados
            Log.d(TAG, "Iniciando download de relacionamentos atualizados")
            updateSyncState("Baixando relacionamentos atualizados...", currentStep++, totalSteps)
            emit(_syncState.value)

            val downloadRelacionamentosResult = downloadUpdatedPacienteEspecialidades()
            if (downloadRelacionamentosResult.isFailure) {
                Log.w(TAG, "Falha no download de relacionamentos: ${downloadRelacionamentosResult.exceptionOrNull()?.message}")
                // Continua mesmo com falha no download
            }

            // Etapa 6: Finalização
            Log.d(TAG, "Finalizando sincronização...")
            updateSyncState("Finalizando sincronização...", currentStep, totalSteps)
            emit(_syncState.value)

            // Sucesso
            val successState = SyncState(
                isLoading = false,
                message = "Sincronização concluída com sucesso!",
                lastSyncTime = System.currentTimeMillis(),
                totalItems = totalSteps,
                processedItems = totalSteps
            )
            _syncState.value = successState
            emit(successState)

            updateLastSyncTimestamp(System.currentTimeMillis())

        } catch (e: Exception) {
            Log.e(TAG, "Erro inesperado durante sincronização", e)
            val errorState = createErrorState(e.message ?: "Erro inesperado durante a sincronização")
            _syncState.value = errorState
            emit(errorState)
        }
    }

    // ==================== SINCRONIZAÇÃO DE PACIENTES ====================

    override suspend fun startSyncPacientes(): Flow<SyncState> = flow {
        _syncState.value = SyncState(
            isLoading = true,
            message = "Sincronizando pacientes...",
            error = null
        )
        emit(_syncState.value)

        try {
            if (!networkManager.checkConnection()) {
                val errorState = createErrorState("Sem conexão com a internet")
                _syncState.value = errorState
                emit(errorState)
                return@flow
            }

            // Upload primeiro
            val uploadResult = uploadPendingPacientes()
            if (uploadResult.isFailure) {
                Log.w(TAG, "Falha no upload de pacientes: ${uploadResult.exceptionOrNull()?.message}")
            }

            // Download depois
            val downloadResult = downloadUpdatedPacientes()
            if (downloadResult.isSuccess) {
                val successState = SyncState(
                    isLoading = false,
                    message = "Pacientes sincronizados com sucesso!",
                    lastSyncTime = System.currentTimeMillis()
                )
                _syncState.value = successState
                emit(successState)
            } else {
                val errorState = createErrorState(
                    downloadResult.exceptionOrNull()?.message ?: "Erro ao sincronizar pacientes"
                )
                _syncState.value = errorState
                emit(errorState)
            }
        } catch (e: Exception) {
            val errorState = createErrorState(e.message ?: "Erro inesperado")
            _syncState.value = errorState
            emit(errorState)
        }
    }

    // ==================== UPLOAD DE PACIENTES PENDENTES ====================

    private suspend fun uploadPendingPacientes(): Result<Unit> {
        return try {
            Log.d(TAG, "=== INICIANDO UPLOAD DE PACIENTES PENDENTES ===")

            // USAR O MÉTODO ESPECÍFICO DO DAO
            val pendingPacientes = pacienteDao.getItemsNeedingSync(
                listOf(
                    SyncStatus.PENDING_UPLOAD,
                    SyncStatus.PENDING_DELETE,
                    SyncStatus.UPLOAD_FAILED,
                    SyncStatus.DELETE_FAILED
                )
            )

            Log.d(TAG, "Encontrados ${pendingPacientes.size} pacientes para sincronizar")

            // LOG DETALHADO DE CADA PACIENTE
            pendingPacientes.forEachIndexed { index, paciente ->
                Log.d(TAG, "Paciente $index:")
                Log.d(TAG, "  - Nome: ${paciente.nome}")
                Log.d(TAG, "  - LocalId: ${paciente.localId}")
                Log.d(TAG, "  - ServerId: ${paciente.serverId}")
                Log.d(TAG, "  - SyncStatus: ${paciente.syncStatus}")
                Log.d(TAG, "  - IsDeleted: ${paciente.isDeleted}")
                Log.d(TAG, "  - UpdatedAt: ${paciente.updatedAt}")
            }

            if (pendingPacientes.isEmpty()) {
                Log.d(TAG, "❌ Nenhum paciente pendente para upload")
                return Result.success(Unit)
            }

            // Separar por tipo de operação - CORRIGIDO
            val forUpload = pendingPacientes.filter {
                it.syncStatus == SyncStatus.PENDING_UPLOAD && it.serverId == null && !it.isDeleted
            }
            val forUpdate = pendingPacientes.filter {
                it.syncStatus == SyncStatus.PENDING_UPLOAD && it.serverId != null && !it.isDeleted
            }
            val forDelete = pendingPacientes.filter {
                (it.syncStatus == SyncStatus.PENDING_DELETE || it.isDeleted) && it.serverId != null
            }

            Log.d(TAG, "📊 SEPARAÇÃO POR OPERAÇÃO:")
            Log.d(TAG, "  - Para criar: ${forUpload.size}")
            Log.d(TAG, "  - Para atualizar: ${forUpdate.size}")
            Log.d(TAG, "  - Para deletar: ${forDelete.size}")

            // Log detalhado dos pacientes para atualização
            if (forUpdate.isNotEmpty()) {
                Log.d(TAG, "🔄 PACIENTES PARA ATUALIZAR:")
                forUpdate.forEach { paciente ->
                    Log.d(TAG, "  - ${paciente.nome} (localId: ${paciente.localId}, serverId: ${paciente.serverId})")
                }
            }

            // Processar criações
            if (forUpload.isNotEmpty()) {
                Log.d(TAG, "🆕 Processando criações...")
                processarCriacaoPacientes(forUpload)
            }

            // Processar atualizações
            if (forUpdate.isNotEmpty()) {
                Log.d(TAG, "🔄 Processando atualizações...")
                processarAtualizacaoPacientes(forUpdate)
            }

            // Processar deleções
            if (forDelete.isNotEmpty()) {
                Log.d(TAG, "🗑️ Processando deleções...")
                processarDelecaoPacientes(forDelete)
            }

            Log.d(TAG, "✅ Upload de pacientes concluído")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no upload de pacientes", e)
            Result.failure(e)
        }
    }

    private suspend fun processarCriacaoPacientes(pacientes: List<com.example.projeto_ibg3.data.local.database.entities.PacienteEntity>) {
        pacientes.chunked(BATCH_SIZE).forEach { batch ->
            try {
                val pacientesDto = batch.map { it.toPacienteDto() }
                val response = apiService.createPacientesBatch(pacientesDto)

                if (response.isSuccessful && response.body()?.success == true) {
                    val createdPacientes = response.body()?.data ?: emptyList()

                    // Atualizar com server_id retornado
                    createdPacientes.forEachIndexed { index, createdDto ->
                        val originalEntity = batch[index]
                        pacienteDao.updateSyncStatusAndServerId(
                            originalEntity.localId,
                            SyncStatus.SYNCED,
                            createdDto.serverId ?: 0L
                        )
                    }

                    Log.d(TAG, "Batch de ${batch.size} pacientes criado com sucesso")
                } else {
                    // Marcar como falha
                    batch.forEach { entity ->
                        pacienteDao.updateSyncStatus(entity.localId, SyncStatus.UPLOAD_FAILED)
                    }
                    Log.e(TAG, "Falha na criação em batch: ${response.message()}")
                }
            } catch (e: Exception) {
                batch.forEach { entity ->
                    pacienteDao.incrementSyncAttempts(entity.localId, System.currentTimeMillis(), e.message)
                }
                Log.e(TAG, "Erro na criação em batch", e)
            }
        }
    }

    private suspend fun processarAtualizacaoPacientes(pacientes: List<PacienteEntity>) {
        Log.d(TAG, "Processando ${pacientes.size} pacientes para atualização")

        pacientes.forEach { entity ->
            try {
                Log.d(TAG, "Atualizando paciente: ${entity.nome} (serverId: ${entity.serverId})")

                val pacienteDto = entity.toPacienteDto()
                Log.d(TAG, "DTO criado: ${pacienteDto}")

                val response = apiService.updatePaciente(entity.serverId!!, pacienteDto)
                Log.d(TAG, "Response code: ${response.code()}, isSuccessful: ${response.isSuccessful()}")

                if (response.isSuccessful) {
                    val responseBody = response.body()
                    Log.d(TAG, "Response body: ${responseBody}")

                    if (responseBody?.success == true) {
                        pacienteDao.updateSyncStatus(entity.localId, SyncStatus.SYNCED)
                        Log.d(TAG, "Paciente ${entity.localId} atualizado com sucesso")
                    } else {
                        val errorMsg = responseBody?.error ?: "Resposta indica falha"
                        Log.e(TAG, "API retornou falha: $errorMsg")
                        pacienteDao.updateSyncStatus(entity.localId, SyncStatus.UPLOAD_FAILED)
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Falha na atualização do paciente ${entity.localId}: ${response.code()} - ${response.message()}")
                    Log.e(TAG, "Error body: $errorBody")
                    pacienteDao.updateSyncStatus(entity.localId, SyncStatus.UPLOAD_FAILED)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro na atualização do paciente ${entity.localId}", e)
                pacienteDao.incrementSyncAttempts(entity.localId, System.currentTimeMillis(), e.message)
            }
        }
    }

    private suspend fun processarDelecaoPacientes(pacientes: List<com.example.projeto_ibg3.data.local.database.entities.PacienteEntity>) {
        pacientes.forEach { entity ->
            try {
                val response = apiService.deletePaciente(entity.serverId!!)

                if (response.isSuccessful) {
                    // Remover permanentemente do banco local
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

    // ==================== DOWNLOAD DE PACIENTES ATUALIZADOS ====================

    private suspend fun downloadUpdatedPacientes(): Result<Unit> {
        return try {
            Log.d(TAG, "Iniciando download de pacientes atualizados")

            val lastSync = getLastSyncTimestamp()
            val response = apiService.getUpdatedPacientes(lastSync)
            Log.d(TAG, "response: ${response.body()}")

            if (response.isSuccessful && response.body()?.success == true) {
                val pacientesDto = response.body()?.data ?: emptyList()
                Log.d(TAG, "Recebidos ${pacientesDto.size} pacientes do servidor")
                Log.d(TAG, "Pacientes: $pacientesDto")

                if (pacientesDto.isNotEmpty()) {
                    processarPacientesDoServidor(pacientesDto)
                }

                Log.d(TAG, "Download de pacientes concluído")
                Result.success(Unit)
            } else {
                val error = response.body()?.error ?: "Erro HTTP: ${response.code()}"
                Log.e(TAG, "Erro no download de pacientes: $error")
                Result.failure(Exception(error))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Erro no download de pacientes", e)
            Result.failure(e)
        }
    }

    private suspend fun processarPacientesDoServidor(pacientesDto: List<PacienteDto>) {
        pacientesDto.forEach { dto ->
            // LOG DE DEBUG - adicione esta linha
            Log.d(TAG, "Processando paciente: ${dto.nome} - Idade DTO: ${dto.idade}")

            // Verificar se já existe no banco local
            val existingByServerId = pacienteDao.getPacienteByServerId(dto.serverId ?: 0L)

            try {
                // Verificar se já existe no banco local
                val existingByServerId = pacienteDao.getPacienteByServerId(dto.serverId ?: 0L)
                val existingByCpf = dto.cpf?.let { pacienteDao.getPacienteByCpf(it) }

                when {
                    existingByServerId != null -> {
                        // Atualizar paciente existente se for mais recente
                        if (dto.updatedAt.toDateLong()!! > existingByServerId.updatedAt) {
                            val updatedEntity = dto.toPacienteEntity().copy(
                                localId = existingByServerId.localId,
                                syncStatus = SyncStatus.SYNCED,
                                lastSyncTimestamp = System.currentTimeMillis()
                            )
                            // LOG DE DEBUG - adicione esta linha
                            Log.d(TAG, "Entity criada: ${updatedEntity.nome} - Idade Entity: ${updatedEntity.idade}")

                            pacienteDao.updatePaciente(updatedEntity)
                            Log.d(TAG, "Paciente atualizado: ${dto.nome}")

                        }
                    }
                    existingByCpf != null && existingByCpf.serverId == null -> {
                        // Paciente local que agora existe no servidor - atualizar com serverId
                        val updatedEntity = existingByCpf.copy(
                            serverId = dto.serverId,
                            syncStatus = SyncStatus.SYNCED,
                            updatedAt = dto.updatedAt.toDateLong(),
                            lastSyncTimestamp = System.currentTimeMillis()
                        )
                        pacienteDao.updatePaciente(updatedEntity)
                        Log.d(TAG, "Paciente local sincronizado: ${dto.nome}")
                    }
                    else -> {
                        // Novo paciente do servidor
                        val newEntity = dto.toPacienteEntity().copy(
                            syncStatus = SyncStatus.SYNCED,
                            lastSyncTimestamp = System.currentTimeMillis()
                        )
                        // LOG DE DEBUG - adicione esta linha
                        Log.d(TAG, "Nova entity criada: ${newEntity.nome} - Idade Entity: ${newEntity.idade}")

                        pacienteDao.insertPaciente(newEntity)
                        Log.d(TAG, "Novo paciente inserido: ${dto.nome}")

                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao processar paciente ${dto.nome}", e)
            }
        }
    }

    // ==================== UPLOAD DE RELACIONAMENTOS PENDENTES ====================

    private suspend fun uploadPendingPacienteEspecialidades(): Result<Unit> {
        return try {
            Log.d(TAG, "Iniciando upload de relacionamentos pendentes")

            val pendingRelations = pacienteEspecialidadeDao.getItemsNeedingSync()
            Log.d(TAG, "Encontrados ${pendingRelations.size} relacionamentos para sincronizar")

            if (pendingRelations.isEmpty()) {
                Log.d(TAG, "Nenhum relacionamento pendente para upload")
                return Result.success(Unit)
            }

            // Separar por tipo de operação
            val forCreate = pendingRelations.filter {
                it.syncStatus == SyncStatus.PENDING_UPLOAD && !it.isDeleted
            }
            val forDelete = pendingRelations.filter {
                it.syncStatus == SyncStatus.PENDING_DELETE
            }

            Log.d(TAG, "Para criar: ${forCreate.size}, Para deletar: ${forDelete.size}")

            // Processar criações
            if (forCreate.isNotEmpty()) {
                processarCriacaoRelacionamentos(forCreate)
            }

            // Processar deleções
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

    private suspend fun processarCriacaoRelacionamentos(relations: List<com.example.projeto_ibg3.data.local.database.entities.PacienteEspecialidadeEntity>) {
        relations.chunked(BATCH_SIZE).forEach { batch ->
            try {
                val relationsDto = batch.mapNotNull { entity ->
                    // Buscar os serverIds correspondentes
                    val paciente = pacienteDao.getPacienteById(entity.pacienteLocalId)
                    val especialidade = especialidadeDao.getEspecialidadeById(entity.especialidadeLocalId)

                    if (paciente?.serverId != null && especialidade?.serverId != null) {
                        PacienteEspecialidadeDTO(
                            pacienteServerId = paciente.serverId!!,
                            especialidadeServerId = especialidade.serverId!!,
                            pacienteLocalId = entity.pacienteLocalId,
                            especialidadeLocalId = entity.especialidadeLocalId,
                            dataAtendimento = entity.dataAtendimento?.let { dateFormat.format(
                                Date(
                                    it
                                )
                            ) }, // Converter Long para String
                            createdAt = dateTimeFormat.format(Date(entity.createdAt)), // Converter Long para String
                            updatedAt = dateTimeFormat.format(Date(entity.updatedAt)), // Converter Long para String
                            lastSyncTimestamp = entity.lastSyncTimestamp
                        )
                    } else {
                        Log.w(TAG, "Relacionamento ignorado - serverIds faltando: paciente=${paciente?.serverId}, especialidade=${especialidade?.serverId}")
                        null
                    }
                }

                if (relationsDto.isNotEmpty()) {
                    val response = apiService.syncPacienteEspecialidades(relationsDto)

                    if (response.isSuccessful && response.body()?.success == true) {
                        // Marcar como sincronizado
                        batch.forEach { entity ->
                            pacienteEspecialidadeDao.updateSyncStatus(
                                entity.pacienteLocalId,
                                entity.especialidadeLocalId,
                                SyncStatus.SYNCED
                            )
                        }
                        Log.d(TAG, "Batch de ${batch.size} relacionamentos criado com sucesso")
                    } else {
                        // Marcar como falha
                        batch.forEach { entity ->
                            pacienteEspecialidadeDao.updateSyncStatus(
                                entity.pacienteLocalId,
                                entity.especialidadeLocalId,
                                SyncStatus.UPLOAD_FAILED
                            )
                        }
                        Log.e(TAG, "Falha na criação em batch de relacionamentos: ${response.message()}")
                    }
                }
            } catch (e: Exception) {
                batch.forEach { entity ->
                    pacienteEspecialidadeDao.incrementSyncAttempts(
                        entity.pacienteLocalId,
                        entity.especialidadeLocalId,
                        System.currentTimeMillis(),
                        e.message
                    )
                }
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
                        // Remover permanentemente do banco local
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

    // ==================== DOWNLOAD DE RELACIONAMENTOS ATUALIZADOS ====================

    private suspend fun downloadUpdatedPacienteEspecialidades(): Result<Unit> {
        return try {
            Log.d(TAG, "Iniciando download de relacionamentos atualizados")

            val lastSync = getLastSyncTimestamp()
            val response = apiService.getUpdatedPacienteEspecialidades(lastSync)

            if (response.isSuccessful) {
                // Acessar o campo 'data' da ApiResponse
                val apiResponse = response.body()
                if (apiResponse?.success == true) {
                    val relationsDto = apiResponse.data ?: emptyList()
                    Log.d(TAG, "Recebidos ${relationsDto.size} relacionamentos do servidor")

                    if (relationsDto.isNotEmpty()) {
                        // Log dos dados recebidos para debug
                        relationsDto.forEachIndexed { index, dto ->
                            Log.d(TAG, "Relacionamento $index: PacienteServerId=${dto.pacienteServerId}, EspecialidadeServerId=${dto.especialidadeServerId}, Data=${dto.dataAtendimento}")
                        }

                        // Verificar se temos pacientes e especialidades necessários
                        val allPacientes = pacienteDao.getAllPacientesList()
                        val allEspecialidades = especialidadeDao.getAllEspecialidadesList()

                        Log.d(TAG, "Pacientes disponíveis no banco local: ${allPacientes.size}")
                        allPacientes.forEach { p: PacienteEntity ->
                            Log.d(TAG, "  Paciente: ${p.nome} (localId: ${p.localId}, serverId: ${p.serverId})")
                        }

                        Log.d(TAG, "Especialidades disponíveis no banco local: ${allEspecialidades.size}")
                        allEspecialidades.forEach { e ->
                            Log.d(TAG, "  Especialidade: ${e.nome} (localId: ${e.localId}, serverId: ${e.serverId})")
                        }

                        // Processar relacionamentos
                        processarRelacionamentosDoServidor(relationsDto)

                        // Verificar quantos relacionamentos foram salvos
                        val totalRelacionamentos = pacienteEspecialidadeDao.getTotalAssociations()
                        Log.d(TAG, "Total de relacionamentos após sincronização: $totalRelacionamentos")
                    }

                    Log.d(TAG, "Download de relacionamentos concluído")
                    Result.success(Unit)
                } else {
                    val error = apiResponse?.error ?: "API retornou success=false"
                    Log.e(TAG, "Erro no download de relacionamentos: $error")
                    Result.failure(Exception(error))
                }
            } else {
                val error = "Erro HTTP: ${response.code()} - ${response.message()}"
                Log.e(TAG, "Erro no download de relacionamentos: $error")
                Result.failure(Exception(error))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Erro no download de relacionamentos (Ask Gemini)", e)
            Result.failure(e)
        }
    }

    private suspend fun processarRelacionamentosDoServidor(relationsDto: List<PacienteEspecialidadeDTO>) {
        Log.d(TAG, "Processando ${relationsDto.size} relacionamentos do servidor")

        relationsDto.forEach { dto ->
            try {
                // Verificar se os IDs do servidor não são nulos
                if (dto.pacienteServerId == null || dto.especialidadeServerId == null) {
                    Log.w(TAG, "IDs nulos no DTO: pacienteId=${dto.pacienteServerId}, especialidadeId=${dto.especialidadeServerId}")
                    return@forEach
                }

                Log.d(TAG, "Processando relacionamento: PacienteServerId=${dto.pacienteServerId}, EspecialidadeServerId=${dto.especialidadeServerId}")

                // Buscar os registros locais pelos serverIds
                val paciente = pacienteDao.getPacienteByServerId(dto.pacienteServerId!!)
                val especialidade = especialidadeDao.getEspecialidadeByServerId(dto.especialidadeServerId!!)

                Log.d(TAG, "Paciente encontrado: ${paciente?.let { "${it.nome} (localId: ${it.localId})" } ?: "null"}")
                Log.d(TAG, "Especialidade encontrada: ${especialidade?.let { "${it.nome} (localId: ${it.localId})" } ?: "null"}")

                if (paciente != null && especialidade != null) {
                    if (dto.isDeleted) {
                        // Remover relacionamento
                        pacienteEspecialidadeDao.deletePermanently(
                            paciente.localId,
                            especialidade.localId
                        )
                        Log.d(TAG, "Relacionamento removido: ${paciente.localId}_${especialidade.localId}")
                    } else {
                        // Verificar se já existe o relacionamento
                        val existing = pacienteEspecialidadeDao.getById(
                            paciente.localId,
                            especialidade.localId
                        )

                        Log.d(TAG, "Relacionamento existente: ${existing?.let { "existe" } ?: "não existe"}")

                        if (existing == null) {
                            // Criar novo relacionamento
                            val newRelation = dto.toEntity(
                                pacienteLocalId = paciente.localId,
                                especialidadeLocalId = especialidade.localId,
                                deviceId = "server",
                                syncStatus = SyncStatus.SYNCED
                            )

                            pacienteEspecialidadeDao.insert(newRelation)
                            Log.d(TAG, "Novo relacionamento inserido: ${paciente.localId}_${especialidade.localId} - Data: ${dto.dataAtendimento}")
                        } else {
                            // Comparar timestamps para decidir se deve atualizar
                            val dtoUpdatedAt = dto.updatedAt.toIsoDateLong()
                            if (dtoUpdatedAt > existing.updatedAt) {
                                // Atualizar relacionamento existente
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
                                Log.d(TAG, "Relacionamento atualizado: ${paciente.localId}_${especialidade.localId}")
                            } else {
                                Log.d(TAG, "Relacionamento já está atualizado: ${paciente.localId}_${especialidade.localId}")
                            }
                        }
                    }
                } else {
                    // Log mais detalhado para debug
                    if (paciente == null) {
                        // Verificar se existe paciente com esse serverId mas com problema na consulta
                        val allPacientes = pacienteDao.getAllPacientesList()
                        val pacienteComServerId = allPacientes.find { it.serverId == dto.pacienteServerId }
                        if (pacienteComServerId != null) {
                            Log.w(TAG, "Paciente encontrado em lista completa: ${pacienteComServerId.nome} (serverId: ${pacienteComServerId.serverId})")
                        } else {
                            Log.w(TAG, "Paciente não encontrado para serverId=${dto.pacienteServerId}")
                            Log.d(TAG, "Pacientes disponíveis:")
                            allPacientes.forEach { p: PacienteEntity ->
                                Log.d(TAG, "  - ${p.nome} (localId: ${p.localId}, serverId: ${p.serverId})")
                            }
                        }
                    }

                    if (especialidade == null) {
                        // Verificar se existe especialidade com esse serverId
                        val allEspecialidades = especialidadeDao.getAllEspecialidadesList()
                        val especialidadeComServerId = allEspecialidades.find { it.serverId == dto.especialidadeServerId }
                        if (especialidadeComServerId != null) {
                            Log.w(TAG, "Especialidade encontrada em lista completa: ${especialidadeComServerId.nome} (serverId: ${especialidadeComServerId.serverId})")
                        } else {
                            Log.w(TAG, "Especialidade não encontrada para serverId=${dto.especialidadeServerId}")
                            Log.d(TAG, "Especialidades disponíveis:")
                            allEspecialidades.forEach { e ->
                                Log.d(TAG, "  - ${e.nome} (localId: ${e.localId}, serverId: ${e.serverId})")
                            }
                        }
                    }

                    Log.w(TAG, "Relacionamento ignorado - paciente ou especialidade não encontrados: pacienteId=${dto.pacienteServerId}, especialidadeId=${dto.especialidadeServerId}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao processar relacionamento do servidor - PacienteId: ${dto.pacienteServerId}, EspecialidadeId: ${dto.especialidadeServerId}", e)
            }
        }

        Log.d(TAG, "Processamento de relacionamentos concluído")
    }

    // ==================== MÉTODOS AUXILIARES ====================

    private fun createErrorState(message: String): SyncState {
        return SyncState(
            isLoading = false,
            error = message,
            message = "Erro na sincronização"
        )
    }

    private fun updateSyncState(message: String, currentStep: Int, totalSteps: Int) {
        _syncState.value = _syncState.value.copy(
            message = message,
            totalItems = totalSteps,
            processedItems = currentStep
        )
    }

    // ==================== MÉTODOS HERDADOS ====================

    override suspend fun startSyncEspecialidades(): Flow<SyncState> = flow {
        _syncState.value = SyncState(
            isLoading = true,
            message = "Sincronizando especialidades...",
            error = null
        )
        emit(_syncState.value)

        try {
            val result = syncEspecialidades()
            if (result.isSuccess) {
                val successState = SyncState(
                    isLoading = false,
                    message = "Especialidades sincronizadas com sucesso!",
                    lastSyncTime = System.currentTimeMillis()
                )
                _syncState.value = successState
                emit(successState)
            } else {
                val errorState = SyncState(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message,
                    message = "Erro ao sincronizar especialidades"
                )
                _syncState.value = errorState
                emit(errorState)
            }
        } catch (e: Exception) {
            val errorState = SyncState(
                isLoading = false,
                error = e.message,
                message = "Erro inesperado"
            )
            _syncState.value = errorState
            emit(errorState)
        }
    }

    override suspend fun syncEspecialidades(): Result<Unit> {
        return try {
            Log.d(TAG, "Iniciando syncEspecialidades")

            // Verificar conexão
            if (!networkManager.checkConnection()) {
                Log.e(TAG, "Sem conexão com internet")
                return Result.failure(Exception("Sem conexão com internet"))
            }

            // Testar servidor
            if (!networkManager.testServerConnection()) {
                Log.e(TAG, "Servidor não acessível")
                return Result.failure(Exception("Servidor não acessível"))
            }

            Log.d(TAG, "Fazendo chamada para API...")

            // Fazer chamada para API
            val response = apiService.getAllEspecialidades()

            if (response.isSuccessful) {
                val apiResponse = response.body()

                if (apiResponse?.success == true) {
                    val especialidadesDto = apiResponse.data ?: emptyList()
                    Log.d(TAG, "Recebidas ${especialidadesDto.size} especialidades do servidor")

                    if (especialidadesDto.isNotEmpty()) {
                        // Converter DTO para Entity e salvar no banco
                        val especialidadesEntity = especialidadesDto.toEntityList(
                            deviceId = "default_device",
                            syncStatus = SyncStatus.SYNCED
                        )

                        val currentEspecialidades = especialidadeDao.getAllEspecialidadesList()
                        Log.d(TAG, "Especialidades atuais no banco: ${currentEspecialidades.size}")

                        // Processar cada especialidade
                        especialidadesEntity.forEach { entity ->
                            if (entity.serverId != null && entity.localId.isNotBlank()) {
                                val existingByServerId = especialidadeDao.getEspecialidadeByServerId(entity.serverId)

                                Log.d(TAG, "Processando: ${entity.nome}")
                                Log.d(TAG, "  - LocalId: '${entity.localId}'")
                                Log.d(TAG, "  - ServerId: ${entity.serverId}")
                                Log.d(TAG, "  - Existing by ServerId: $existingByServerId")

                                if (existingByServerId == null) {
                                    val existingByName = especialidadeDao.getEspecialidadeByName(entity.nome)

                                    if (existingByName != null && existingByName.serverId == null) {
                                        // Atualizar a especialidade local existente com os dados do servidor
                                        val updatedEntity = existingByName.copy(
                                            serverId = entity.serverId,
                                            syncStatus = SyncStatus.SYNCED,
                                            updatedAt = entity.updatedAt,
                                            lastSyncTimestamp = System.currentTimeMillis()
                                        )
                                        especialidadeDao.updateEspecialidade(updatedEntity)
                                        Log.d(TAG, "Atualizada especialidade local existente: ${entity.nome} (serverId: ${entity.serverId})")
                                    } else {
                                        // Inserir nova especialidade
                                        especialidadeDao.insertEspecialidade(entity)
                                        Log.d(TAG, "Inserida nova especialidade: ${entity.nome} (serverId: ${entity.serverId})")
                                    }
                                } else if (existingByServerId.updatedAt < entity.updatedAt) {
                                    // Manter o localId original, mas atualizar outros dados
                                    val updatedEntity = entity.copy(localId = existingByServerId.localId)
                                    especialidadeDao.updateEspecialidade(updatedEntity)
                                    Log.d(TAG, "Atualizada especialidade: ${entity.nome} (serverId: ${entity.serverId})")
                                } else {
                                    Log.d(TAG, "Especialidade já está atualizada: ${entity.nome} (serverId: ${entity.serverId})")
                                }
                            } else {
                                Log.w(TAG, "ServerId é null ou LocalId está vazio para especialidade: ${entity.nome} - Dados: ServerId=${entity.serverId}, LocalId='${entity.localId}' - Ignorando...")
                            }
                        }

                        // DEBUG final: Mostrar estado após processamento
                        val finalEspecialidades = especialidadeDao.getAllEspecialidadesList()
                        Log.d(TAG, "Especialidades após sync: ${finalEspecialidades.size}")
                        finalEspecialidades.forEach { esp ->
                            Log.d(TAG, "Final DB: ${esp.nome} - LocalId: '${esp.localId}' - ServerId: ${esp.serverId}")
                        }

                    } else {
                        Log.w(TAG, "Nenhuma especialidade retornada do servidor")
                    }

                    Log.d(TAG, "Sincronização de especialidades concluída com sucesso")
                    Result.success(Unit)
                } else {
                    val error = apiResponse?.error ?: "Resposta da API indica falha"
                    Log.e(TAG, "Erro na resposta da API: $error")
                    Result.failure(Exception(error))
                }
            } else {
                val error = "Erro HTTP: ${response.code()} - ${response.message()}"
                Log.e(TAG, error)
                Result.failure(Exception(error))
            }

        } catch (e: Exception) {
            Log.e(TAG, "Erro em syncEspecialidades", e)
            Result.failure(e)
        }
    }

    override suspend fun syncPacientes(): Result<Unit> {
        return try {
            Log.d(TAG, "Iniciando syncPacientes")

            if (!networkManager.checkConnection()) {
                Log.e(TAG, "Sem conexão com internet")
                return Result.failure(Exception("Sem conexão com internet"))
            }

            if (!networkManager.testServerConnection()) {
                Log.e(TAG, "Servidor não acessível")
                return Result.failure(Exception("Servidor não acessível"))
            }

            // Upload primeiro
            val uploadResult = uploadPendingPacientes()
            if (uploadResult.isFailure) {
                Log.w(TAG, "Falha no upload de pacientes: ${uploadResult.exceptionOrNull()?.message}")
            }

            // Download depois
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

    // Método existente para compatibilidade
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

    override suspend fun syncAll(): Result<Unit> {
        return try {
            // Sincronizar especialidades primeiro (são menos dados e necessárias para outras operações)
            val especialidadesResult = syncEspecialidades()
            if (especialidadesResult.isFailure) return especialidadesResult

            val pacientesResult = syncPacientes()
            if (pacientesResult.isFailure) return pacientesResult

            // Sincronizar relacionamentos
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
        // Por enquanto retorna 0, mas você pode implementar usando SharedPreferences
        // ou uma tabela de metadados no banco
        return 0L
    }

    override suspend fun updateLastSyncTimestamp(timestamp: Long) {
        // Implementar salvamento do timestamp da última sincronização
        // Pode usar SharedPreferences ou salvar no banco
    }

    override fun clearError() {
        _syncState.value = _syncState.value.copy(error = null)
    }

    // ==================== MÉTODOS PÚBLICOS PARA RELACIONAMENTOS ====================

    override suspend fun syncPacienteEspecialidadesOnly(): Result<Unit> {
        return try {
            Log.d(TAG, "Iniciando sincronização apenas de relacionamentos")

            if (!networkManager.checkConnection()) {
                return Result.failure(Exception("Sem conexão com internet"))
            }

            // Upload primeiro
            val uploadResult = uploadPendingPacienteEspecialidades()
            if (uploadResult.isFailure) {
                Log.w(TAG, "Falha no upload de relacionamentos: ${uploadResult.exceptionOrNull()?.message}")
            }

            // Download depois
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

    // ==================== NOVOS MÉTODOS PARA SINCRONIZAÇÃO ESPECÍFICA ====================

    /**
     * Sincroniza pacientes que foram atualizados (status PENDING_UPLOAD com serverId)
     */
    override suspend fun syncPacientesUpdated(): Result<Unit> {
        return try {
            Log.d(TAG, "Iniciando sincronização de pacientes atualizados...")

            if (!networkManager.checkConnection()) {
                return Result.failure(Exception("Sem conexão com internet"))
            }

            // Buscar pacientes com status PENDING_UPLOAD que têm serverId (já existem no servidor)
            val pacientesParaAtualizar = pacienteDao.getPacientesParaAtualizar(SyncStatus.PENDING_UPLOAD)

            Log.d(TAG, "Encontrados ${pacientesParaAtualizar.size} pacientes para atualizar")

            if (pacientesParaAtualizar.isEmpty()) {
                Log.d(TAG, "Nenhum paciente para atualizar")
                return Result.success(Unit)
            }

            // Processar atualizações
            processarAtualizacaoPacientes(pacientesParaAtualizar)

            Log.d(TAG, "Sincronização de pacientes atualizados concluída")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Erro na sincronização de pacientes atualizados", e)
            Result.failure(e)
        }
    }

    /**
     * Sincroniza apenas novos pacientes (que ainda não têm serverId)
     */
    override suspend fun syncNovosPacientes(): Result<Unit> {
        return try {
            Log.d(TAG, "Iniciando sincronização de novos pacientes...")

            if (!networkManager.checkConnection()) {
                return Result.failure(Exception("Sem conexão com internet"))
            }

            // Buscar pacientes novos (sem serverId e com status PENDING_UPLOAD)
            val novosPacientes = pacienteDao.getNovosPacientes(SyncStatus.PENDING_UPLOAD)

            Log.d(TAG, "Encontrados ${novosPacientes.size} novos pacientes para sincronizar")

            if (novosPacientes.isEmpty()) {
                Log.d(TAG, "Nenhum novo paciente para sincronizar")
                return Result.success(Unit)
            }

            // Processar criações (reutilizar método existente)
            processarCriacaoPacientes(novosPacientes)

            Log.d(TAG, "Sincronização de novos pacientes concluída")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Erro na sincronização de novos pacientes", e)
            Result.failure(e)
        }
    }

    /**
     * Sincroniza todos os pacientes pendentes (novos + atualizados)
     */
    override suspend fun syncAllPendingPacientes(): Result<Unit> {
        return try {
            Log.d(TAG, "Iniciando sincronização de todos os pacientes pendentes...")

            // Usar o método existente que já faz tudo isso
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

    // ==================== MÉTODO AUXILIAR PARA MELHOR LOGGING ====================

    /**
     * Versão otimizada do processamento de atualizações com melhor logging
     */
    private suspend fun processarAtualizacoesPacientesOtimizado(pacientes: List<PacienteEntity>) {
        Log.d(TAG, "=== PROCESSANDO ${pacientes.size} ATUALIZAÇÕES DE PACIENTES ===")

        for ((index, entity) in pacientes.withIndex()) {
            try {
                Log.d(TAG, "[$index/${pacientes.size}] Atualizando: ${entity.nome}")
                Log.d(TAG, "  - LocalId: ${entity.localId}")
                Log.d(TAG, "  - ServerId: ${entity.serverId}")
                Log.d(TAG, "  - Status: ${entity.syncStatus}")
                Log.d(TAG, "  - UpdatedAt: ${entity.updatedAt}")

                if (entity.serverId == null) {
                    Log.w(TAG, "  ⚠️ Paciente marcado para atualização mas sem serverId, pulando...")
                    continue
                }

                val pacienteDto = entity.toPacienteDto()
                Log.d(TAG, "  - DTO preparado: ${pacienteDto.nome}")

                val response = apiService.updatePaciente(entity.serverId!!, pacienteDto)

                Log.d(TAG, "  - Response: ${response.code()} - ${response.message()}")

                if (response.isSuccessful) {
                    val responseBody = response.body()

                    if (responseBody?.success == true) {
                        pacienteDao.updateSyncStatus(entity.localId, SyncStatus.SYNCED)
                        Log.d(TAG, "  ✅ Paciente atualizado com sucesso")
                    } else {
                        val errorMsg = responseBody?.error ?: "API retornou sucesso=false"
                        Log.e(TAG, "  ❌ Falha na API: $errorMsg")
                        pacienteDao.updateSyncStatus(entity.localId, SyncStatus.UPLOAD_FAILED)
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "  ❌ Erro HTTP: ${response.code()}")
                    Log.e(TAG, "  Error body: $errorBody")
                    pacienteDao.updateSyncStatus(entity.localId, SyncStatus.UPLOAD_FAILED)
                }

            } catch (e: Exception) {
                Log.e(TAG, "  💥 Exceção ao atualizar paciente ${entity.localId}", e)
                pacienteDao.incrementSyncAttempts(entity.localId, System.currentTimeMillis(), e.message)
            }
        }

        Log.d(TAG, "=== PROCESSAMENTO DE ATUALIZAÇÕES CONCLUÍDO ===")
    }

    // ==================== MÉTODOS DE DEBUG/MONITORAMENTO ====================

    /**
     * Método para debug - mostra status atual dos pacientes pendentes
     */
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

    /**
     * Força uma tentativa de sincronização imediata com logging detalhado
     */
    suspend fun forceSyncWithDetailedLogging(): Result<Unit> {
        return try {
            Log.d(TAG, "🚀 INICIANDO SINCRONIZAÇÃO FORÇADA COM LOGGING DETALHADO")

            // Debug inicial
            debugPacientesPendentes()

            // Verificar conectividade
            Log.d(TAG, "📡 Verificando conectividade...")
            if (!networkManager.checkConnection()) {
                Log.e(TAG, "❌ Sem conexão com internet")
                return Result.failure(Exception("Sem conexão com internet"))
            }
            Log.d(TAG, "✅ Conexão OK")

            if (!networkManager.testServerConnection()) {
                Log.e(TAG, "❌ Servidor não acessível")
                return Result.failure(Exception("Servidor não acessível"))
            }
            Log.d(TAG, "✅ Servidor acessível")

            // Executar sincronização
            val result = syncAllPendingPacientes()

            if (result.isSuccess) {
                Log.d(TAG, "🎉 SINCRONIZAÇÃO CONCLUÍDA COM SUCESSO")
            } else {
                Log.e(TAG, "💥 SINCRONIZAÇÃO FALHOU: ${result.exceptionOrNull()?.message}")
            }

            // Debug final
            debugPacientesPendentes()

            result

        } catch (e: Exception) {
            Log.e(TAG, "💥 ERRO INESPERADO NA SINCRONIZAÇÃO FORÇADA", e)
            Result.failure(e)
        }
    }

}