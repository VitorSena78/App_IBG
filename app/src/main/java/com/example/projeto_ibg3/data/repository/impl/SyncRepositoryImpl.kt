package com.example.projeto_ibg3.data.repository.impl

import android.util.Log
import com.example.projeto_ibg3.data.local.database.dao.EspecialidadeDao
import com.example.projeto_ibg3.data.mappers.toDomain
import com.example.projeto_ibg3.data.remote.api.ApiResult
import com.example.projeto_ibg3.data.remote.api.ApiService
import com.example.projeto_ibg3.data.remote.api.NetworkManager
import com.example.projeto_ibg3.data.remote.dto.PacienteDto
import com.example.projeto_ibg3.data.remote.dto.EspecialidadeDto
import com.example.projeto_ibg3.data.mappers.toEntityList
import com.example.projeto_ibg3.domain.model.SyncProgress
import com.example.projeto_ibg3.domain.model.SyncState
import com.example.projeto_ibg3.domain.model.SyncStatus
import com.example.projeto_ibg3.domain.repository.SyncRepository
import com.example.projeto_ibg3.domain.repository.PacienteRepository
import com.example.projeto_ibg3.domain.repository.EspecialidadeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val networkManager: NetworkManager,
    private val especialidadeDao: EspecialidadeDao, // <- Injete o DAO
    private val pacienteRepository: PacienteRepository,
    private val especialidadeRepository: EspecialidadeRepository,
    private val apiService: ApiService
): SyncRepository {

    companion object {
        private const val TAG = "SyncRepository"
    }

    // Estados observáveis
    private val _syncState = MutableStateFlow(SyncState())
    override val syncState: Flow<SyncState> = _syncState.asStateFlow()

    private val _syncProgress = MutableStateFlow(SyncProgress())
    override val syncProgress: Flow<SyncProgress> = _syncProgress.asStateFlow()

    override suspend fun startSync(): Flow<SyncState> = flow {
        _syncState.value = SyncState(
            isLoading = true,
            message = "Iniciando sincronização completa...",
            error = null
        )
        emit(_syncState.value)

        try {
            if (!networkManager.checkConnection()) {
                val errorState = SyncState(
                    isLoading = false,
                    error = "Sem conexão com a internet",
                    message = "Verifique sua conexão"
                )
                _syncState.value = errorState
                emit(errorState)
                return@flow
            }

            // Etapa 1: Sincronizar Especialidades primeiro
            _syncState.value = _syncState.value.copy(
                message = "Sincronizando especialidades...",
                totalItems = 2,
                processedItems = 0
            )
            emit(_syncState.value)

            val especialidadesResult = syncEspecialidades()
            if (especialidadesResult.isFailure) {
                val errorState = SyncState(
                    isLoading = false,
                    error = especialidadesResult.exceptionOrNull()?.message,
                    message = "Erro ao sincronizar especialidades"
                )
                _syncState.value = errorState
                emit(errorState)
                return@flow
            }

            // Etapa 2: Sincronizar Pacientes
            _syncState.value = _syncState.value.copy(
                message = "Sincronizando pacientes...",
                processedItems = 1
            )
            emit(_syncState.value)

            val pacientesResult = syncPacientes()
            if (pacientesResult.isFailure) {
                val errorState = SyncState(
                    isLoading = false,
                    error = pacientesResult.exceptionOrNull()?.message,
                    message = "Erro ao sincronizar pacientes"
                )
                _syncState.value = errorState
                emit(errorState)
                return@flow
            }

            // Sucesso
            val successState = SyncState(
                isLoading = false,
                message = "Sincronização concluída com sucesso!",
                lastSyncTime = System.currentTimeMillis(),
                totalItems = 2,
                processedItems = 2
            )
            _syncState.value = successState
            emit(successState)

            updateLastSyncTimestamp(System.currentTimeMillis())

        } catch (e: Exception) {
            Log.e(TAG, "Erro inesperado durante sincronização", e)
            val errorState = SyncState(
                isLoading = false,
                error = e.message,
                message = "Erro inesperado durante a sincronização"
            )
            _syncState.value = errorState
            emit(errorState)
        }
    }

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

    override suspend fun startSyncPacientes(): Flow<SyncState> = flow {
        _syncState.value = SyncState(
            isLoading = true,
            message = "Sincronizando pacientes...",
            error = null
        )
        emit(_syncState.value)

        try {
            val result = syncPacientes()
            if (result.isSuccess) {
                val successState = SyncState(
                    isLoading = false,
                    message = "Pacientes sincronizados com sucesso!",
                    lastSyncTime = System.currentTimeMillis()
                )
                _syncState.value = successState
                emit(successState)
            } else {
                val errorState = SyncState(
                    isLoading = false,
                    error = result.exceptionOrNull()?.message,
                    message = "Erro ao sincronizar pacientes"
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

                        // DEBUG: Mostrar dados recebidos do servidor
                        Log.d(TAG, "=== DADOS DO SERVIDOR ===")
                        especialidadesDto.forEachIndexed { index, dto ->
                            Log.d(TAG, "[$index] Nome: ${dto.nome}, ServerId: ${dto.serverId}, LocalId: '${dto.localId}'")
                        }

                        Log.d(TAG, "=== ENTIDADES CONVERTIDAS ===")
                        especialidadesEntity.forEachIndexed { index, entity ->
                            Log.d(TAG, "[$index] Nome: ${entity.nome}, ServerId: ${entity.serverId}, LocalId: '${entity.localId}'")
                        }

                        // Processar cada especialidade
                        especialidadesEntity.forEach { entity ->
                            // CORREÇÃO: Verificar se serverId não é null E se localId não está vazio
                            if (entity.serverId != null && entity.localId.isNotBlank()) {
                                // Verifica se já existe uma especialidade com o mesmo serverId
                                val existingByServerId = especialidadeDao.getEspecialidadeByServerId(entity.serverId)

                                Log.d(TAG, "Processando: ${entity.nome}")
                                Log.d(TAG, "  - LocalId: '${entity.localId}'")
                                Log.d(TAG, "  - ServerId: ${entity.serverId}")
                                Log.d(TAG, "  - Existing by ServerId: $existingByServerId")

                                if (existingByServerId == null) {
                                    // ADICIONAL: Verificar se já existe uma especialidade com o mesmo nome
                                    // Isso previne duplicação quando uma especialidade local vira sincronizada
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

            val lastSync = getLastSyncTimestamp()

            // Usar o método existente que funciona com Flow
            syncPacientes(lastSync).collect { result ->
                when (result) {
                    is ApiResult.Success -> {
                        Log.d(TAG, "SUCESSO! Recebidos ${result.data.size} pacientes")
                        // Aqui você implementaria a lógica para salvar no banco local
                        // pacienteRepository.syncFromServer(result.data)
                    }
                    is ApiResult.Error -> {
                        Log.e(TAG, "Erro na API: ${result.exception}")
                        throw Exception(result.exception)
                    }
                    is ApiResult.Loading -> {
                        Log.d(TAG, "Carregando pacientes...")
                    }
                }
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

            updateLastSyncTimestamp(System.currentTimeMillis())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun hasPendingChanges(): Boolean {
        // Verificar se há mudanças pendentes tanto em especialidades quanto em pacientes
        return especialidadeRepository.hasPendingChanges()
        // || pacienteRepository.hasPendingChanges() // implementar quando necessário
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
}