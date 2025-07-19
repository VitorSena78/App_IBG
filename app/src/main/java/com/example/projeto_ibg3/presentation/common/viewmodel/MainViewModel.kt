package com.example.projeto_ibg3.presentation.common.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projeto_ibg3.domain.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val syncRepository: SyncRepository // Agora injeta o repository diretamente
) : ViewModel() {

    private val _showSyncButton = MutableStateFlow(true)
    val showSyncButton: StateFlow<Boolean> = _showSyncButton.asStateFlow()

    // Expor os estados do sync
    val syncState = syncRepository.syncState
    val syncProgress = syncRepository.syncProgress

    fun triggerSync() {
        viewModelScope.launch {
            syncRepository.startSync().collect {
                // O estado já é observado via syncPhase
            }
        }
    }

    fun triggerSyncPacientes() {
        viewModelScope.launch {
            syncRepository.startSyncPacientes().collect {
                // O estado já é observado via syncPhase
            }
        }
    }

    fun triggerSyncEspecialidades() {
        viewModelScope.launch {
            syncRepository.startSyncEspecialidades().collect {
                // O estado já é observado via syncPhase
            }
        }
    }

    fun hideSyncButton() {
        _showSyncButton.value = false
    }

    fun showSyncButton() {
        _showSyncButton.value = true
    }

    fun clearSyncError() {
        syncRepository.clearError()
    }
}