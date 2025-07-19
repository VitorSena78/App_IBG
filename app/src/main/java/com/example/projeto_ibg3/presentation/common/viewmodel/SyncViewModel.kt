package com.example.projeto_ibg3.presentation.common.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projeto_ibg3.data.remote.api.ApiResult
import com.example.projeto_ibg3.data.remote.dto.PacienteDto
//import com.example.projeto_ibg3.data.repository.impl.SyncRepositoryImpl
//import com.example.projeto_ibg3.data.local.repository.PacienteRepository
import com.example.projeto_ibg3.data.repository.impl.SyncRepositoryImpl
import com.example.projeto_ibg3.domain.model.SyncState
import com.example.projeto_ibg3.domain.repository.PacienteRepository
import com.example.projeto_ibg3.domain.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val syncRepository: SyncRepository
) : ViewModel() {

    // Expor os estados do repository
    val syncState = syncRepository.syncState
    val syncProgress = syncRepository.syncProgress

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