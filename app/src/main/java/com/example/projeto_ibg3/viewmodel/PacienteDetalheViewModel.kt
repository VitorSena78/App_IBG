package com.example.projeto_ibg3.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projeto_ibg3.model.Paciente
import com.example.projeto_ibg3.data.repository.PacienteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PacienteDetalheViewModel @Inject constructor(
    private val pacienteRepository: PacienteRepository
) : ViewModel() {

    private val _paciente = MutableStateFlow<Paciente?>(null)
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)

    val paciente: StateFlow<Paciente?> = _paciente.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadPaciente(pacienteId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val paciente = pacienteRepository.getPacienteById(pacienteId)
                _paciente.value = paciente
            } catch (e: Exception) {
                _error.value = "Erro ao carregar dados do paciente: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}