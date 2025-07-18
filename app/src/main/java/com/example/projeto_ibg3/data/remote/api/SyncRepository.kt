package com.example.projeto_ibg3.data.remote.api

import com.example.projeto_ibg3.data.remote.api.ApiConfig
import com.example.projeto_ibg3.data.remote.api.ApiResult
import com.example.projeto_ibg3.data.remote.api.NetworkManager
import com.example.projeto_ibg3.data.remote.dto.PacienteDto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SyncRepository(private val networkManager: NetworkManager) {

    private val apiService = ApiConfig.getApiService()

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
}