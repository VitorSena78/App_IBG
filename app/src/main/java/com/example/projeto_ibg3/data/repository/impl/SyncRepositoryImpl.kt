package com.example.projeto_ibg3.data.repository.impl

import android.util.Log
import com.example.projeto_ibg3.data.remote.api.ApiConfig
import com.example.projeto_ibg3.data.remote.api.ApiResult
import com.example.projeto_ibg3.data.remote.api.ApiService
import com.example.projeto_ibg3.data.remote.api.NetworkManager
import com.example.projeto_ibg3.data.remote.dto.PacienteDto
import com.example.projeto_ibg3.data.remote.dto.SyncType
import com.example.projeto_ibg3.domain.model.SyncProgress
import com.example.projeto_ibg3.domain.model.SyncState

import com.example.projeto_ibg3.domain.repository.SyncRepository
import com.example.projeto_ibg3.domain.repository.PacienteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val networkManager: NetworkManager,
    private val pacienteRepository: PacienteRepository, // Injetar se precisar
): SyncRepository {

    private val apiService = ApiConfig.getApiService()

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

            // Etapa 1: Sincronizar Pacientes
            _syncState.value = _syncState.value.copy(
                message = "Sincronizando pacientes...",
                totalItems = 2,
                processedItems = 0
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

            // Etapa 2: Sincronizar Especialidades
            _syncState.value = _syncState.value.copy(
                message = "Sincronizando especialidades...",
                processedItems = 1
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

            // Atualizar timestamp da última sincronização
            updateLastSyncTimestamp(System.currentTimeMillis())

        } catch (e: Exception) {
            val errorState = SyncState(
                isLoading = false,
                error = e.message,
                message = "Erro inesperado durante a sincronização"
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

    // Método existente para compatibilidade
    fun syncPacientes(lastSyncTimestamp: Long): Flow<ApiResult<List<PacienteDto>>> = flow {
        emit(ApiResult.Loading())

        try {
            if (!networkManager.checkConnection()) {
                emit(ApiResult.Error(Exception("Sem conexão com a internet")))
                return@flow
            }

            val response = apiService.getUpdatedPacientes(lastSyncTimestamp)

            if (response.isSuccessful) {
                val apiResponse = response.body()
                if (apiResponse?.success == true) {
                    emit(ApiResult.Success(apiResponse.data ?: emptyList()))
                } else {
                    emit(ApiResult.Error(Exception(apiResponse?.error ?: "Erro desconhecido")))
                }
            } else {
                emit(ApiResult.Error(Exception("Erro HTTP: ${response.code()}")))
            }
        } catch (e: Exception) {
            emit(ApiResult.Error(e))
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
                    ApiResult.Error(Exception(apiResponse?.error ?: "Erro ao criar paciente"))
                }
            } else {
                ApiResult.Error(Exception("Erro HTTP: ${response.code()}"))
            }
        } catch (e: Exception) {
            ApiResult.Error(e)
        }
    }

    override suspend fun syncAll(): Result<Unit> {
        return try {
            val pacientesResult = syncPacientes()
            if (pacientesResult.isFailure) return pacientesResult

            val especialidadesResult = syncEspecialidades()
            if (especialidadesResult.isFailure) return especialidadesResult

            updateLastSyncTimestamp(System.currentTimeMillis())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun syncPacientes(): Result<Unit> {
        return try {
            Log.d("SyncRepository", "Iniciando syncPacientes - FAZENDO CONEXÃO REAL")

            // VERIFICAR CONEXÃO
            if (!networkManager.checkConnection()) {
                Log.e("SyncRepository", "Sem conexão com internet")
                return Result.failure(Exception("Sem conexão com internet"))
            }

            // TESTAR SERVIDOR
            Log.d("SyncRepository", "Testando conexão com servidor...")
            val serverOk = networkManager.testServerConnection()
            if (!serverOk) {
                Log.e("SyncRepository", "Servidor não acessível")
                return Result.failure(Exception("Servidor não acessível"))
            }

            Log.d("SyncRepository", "Servidor OK! Buscando pacientes...")

            // FAZER CONEXÃO REAL - usando o método que já existe
            val lastSync = getLastSyncTimestamp()

            // Usar o método syncPacientes que já funciona
            syncPacientes(lastSync).collect { result ->
                when (result) {
                    is ApiResult.Success -> {
                        Log.d("SyncRepository", "SUCESSO! Recebidos ${result.data.size} pacientes")
                        // Aqui você salvaria no banco local:
                        // pacienteRepository.syncFromServer(result.data)
                    }
                    is ApiResult.Error -> {
                        Log.e("SyncRepository", "Erro na API: ${result.exception.message}")
                        throw result.exception
                    }
                    is ApiResult.Loading -> {
                        Log.d("SyncRepository", "Carregando...")
                    }
                }
            }

            Log.d("SyncRepository", "syncPacientes concluído com sucesso")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e("SyncRepository", "Erro em syncPacientes", e)
            Result.failure(e)
        }
    }

    override suspend fun syncEspecialidades(): Result<Unit> {
        return try {
            Log.d("SyncRepository", "Iniciando syncEspecialidades")

            // Por enquanto, só testar se o servidor responde
            if (!networkManager.testServerConnection()) {
                return Result.failure(Exception("Servidor não acessível"))
            }

            // TODO: Implementar busca de especialidades
            // val response = apiService.getEspecialidades()

            Log.d("SyncRepository", "syncEspecialidades concluído")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e("SyncRepository", "Erro em syncEspecialidades", e)
            Result.failure(e)
        }
    }

    override suspend fun hasPendingChanges(): Boolean {
        // Implementar verificação se há mudanças pendentes
        // Verificar no banco local se há registros marcados como "pending sync"
        return false
    }

    override suspend fun getLastSyncTimestamp(): Long {
        // Implementar busca do timestamp da última sincronização
        // Pode ser armazenado em SharedPreferences ou na base de dados
        return 0L
    }

    override suspend fun updateLastSyncTimestamp(timestamp: Long) {
        // Implementar atualização do timestamp
        // Salvar em SharedPreferences ou na base de dados
    }

    override fun clearError() {
        _syncState.value = _syncState.value.copy(error = null)
    }
}