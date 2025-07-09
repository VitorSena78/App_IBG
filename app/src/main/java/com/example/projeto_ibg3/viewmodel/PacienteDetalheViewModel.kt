package com.example.projeto_ibg3.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projeto_ibg3.data.repository.EspecialidadeRepository
import com.example.projeto_ibg3.data.repository.PacienteEspecialidadeRepository
import com.example.projeto_ibg3.model.Paciente
import com.example.projeto_ibg3.data.repository.PacienteRepository
import com.example.projeto_ibg3.model.Especialidade
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PacienteDetalheViewModel @Inject constructor(
    private val pacienteRepository: PacienteRepository,
    private val especialidadeRepository: EspecialidadeRepository,
    private val pacienteEspecialidadeRepository: PacienteEspecialidadeRepository
) : ViewModel() {

    private val _paciente = MutableStateFlow<Paciente?>(null)
    val paciente: StateFlow<Paciente?> = _paciente.asStateFlow()

    private val _pacienteEspecialidades = MutableStateFlow<List<Especialidade>>(emptyList())
    val pacienteEspecialidades: StateFlow<List<Especialidade>> = _pacienteEspecialidades.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadPaciente(pacienteId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // CORRIGIDO: Usar pacienteId em vez de id
                val paciente = pacienteRepository.getPacienteById(pacienteId)
                _paciente.value = paciente

                if (paciente == null) {
                    _error.value = "Paciente não encontrado"
                }
            } catch (e: Exception) {
                _error.value = "Erro ao carregar paciente: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadPacienteEspecialidades(pacienteId: Long) {
        viewModelScope.launch {
            try {
                _error.value = null

                // Buscar as associações paciente-especialidade
                val pacienteEspecialidades = pacienteEspecialidadeRepository.getEspecialidadesByPacienteId(pacienteId)

                // Buscar os dados completos das especialidades
                val especialidades = mutableListOf<Especialidade>()
                pacienteEspecialidades.forEach { associacao ->
                    val especialidadeEntity = especialidadeRepository.getEspecialidadeById(associacao.especialidadeId)

                    // CORRIGIDO: Converter Entity para Model
                    especialidadeEntity?.let { entity ->
                        val especialidade = Especialidade(
                            id = entity.id,
                            serverId = entity.serverId,
                            nome = entity.nome
                        )
                        especialidades.add(especialidade)
                    }
                }

                _pacienteEspecialidades.value = especialidades

            } catch (e: Exception) {
                _error.value = "Erro ao carregar especialidades: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}