package com.example.projeto_ibg3.presentation.common.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.example.projeto_ibg3.data.remote.api.ApiResult
//import com.example.projeto_ibg3.data.repository.impl.SyncRepositoryImpl
//import com.example.projeto_ibg3.data.local.repository.PacienteRepository
import com.example.projeto_ibg3.domain.repository.SyncRepository
import com.example.projeto_ibg3.domain.usecase.GetAllEspecialidadesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val syncRepository: SyncRepository,
    private val getAllEspecialidadesUseCase: GetAllEspecialidadesUseCase
) : ViewModel() {

    // Expor os estados do repository
    val syncState = syncRepository.syncState
    val syncProgress = syncRepository.syncProgress

    fun loadEspecialidades(forceSync: Boolean = false) = liveData(Dispatchers.IO) {
        try {
            emit(ApiResult.Loading())
            val result = getAllEspecialidadesUseCase.loadEspecialidades(forceSync)

            if (result.isSuccess) {
                emit(ApiResult.Success(result.getOrNull()))
            } else {
                emit(ApiResult.Error(result.exceptionOrNull()?.message ?: "Erro ao carregar especialidades"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emit(ApiResult.Error(e.message ?: "Erro desconhecido"))
        }
    }

    fun startFullSync() {
        viewModelScope.launch {
            syncRepository.startSync().collect {
                // O estado já é observado via syncPhase
            }
        }
    }

    fun startSyncPacientes() {
        viewModelScope.launch {
            syncRepository.startSyncPacientes().collect {
                // O estado já é observado via syncPhase
            }
        }
    }

    fun startSyncEspecialidades() {
        viewModelScope.launch {
            syncRepository.startSyncEspecialidades().collect {
                // O estado já é observado via syncPhase
            }
        }
    }

    fun clearError() {
        syncRepository.clearError()
    }
}