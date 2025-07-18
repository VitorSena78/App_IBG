package com.example.projeto_ibg3.presentation.ui.detalhe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projeto_ibg3.data.repository.impl.EspecialidadeRepositoryImpl
import com.example.projeto_ibg3.data.repository.impl.PacienteEspecialidadeRepositoryImpl
import com.example.projeto_ibg3.domain.model.Paciente
import com.example.projeto_ibg3.data.repository.impl.PacienteRepositoryImpl
import com.example.projeto_ibg3.domain.model.Especialidade
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PacienteDetalheViewModel @Inject constructor(
    private val pacienteRepositoryImpl: PacienteRepositoryImpl,
    private val especialidadeRepositoryImpl: EspecialidadeRepositoryImpl,
    private val pacienteEspecialidadeRepositoryImpl: PacienteEspecialidadeRepositoryImpl
) : ViewModel() {

    private val _paciente = MutableStateFlow<Paciente?>(null)
    val paciente: StateFlow<Paciente?> = _paciente.asStateFlow()

    private val _pacienteEspecialidades = MutableStateFlow<List<Especialidade>>(emptyList())
    val pacienteEspecialidades: StateFlow<List<Especialidade>> = _pacienteEspecialidades.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // Método principal para carregar paciente por localId
    fun loadPaciente(pacienteLocalId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                val paciente = pacienteRepositoryImpl.getPacienteById(pacienteLocalId)
                _paciente.value = paciente

                if (paciente == null) {
                    _error.value = "Paciente não encontrado"
                } else {
                    // Carregar especialidades automaticamente
                    loadPacienteEspecialidades(pacienteLocalId)
                }
            } catch (e: Exception) {
                _error.value = "Erro ao carregar paciente com ID $pacienteLocalId: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Método para carregar especialidades do paciente
    fun loadPacienteEspecialidades(pacienteLocalId: String) {
        viewModelScope.launch {
            try {
                _error.value = null

                // Buscar as associações paciente-especialidade usando o localId do paciente
                val pacienteEspecialidadeEntities = pacienteEspecialidadeRepositoryImpl
                    .getEspecialidadesByPacienteId(pacienteLocalId)

                // Buscar os dados completos das especialidades
                val especialidades = mutableListOf<Especialidade>()

                pacienteEspecialidadeEntities.forEach { associacao ->
                    // CORREÇÃO: Usar as propriedades corretas da PacienteEspecialidadeEntity
                    val especialidadeEntity = when {
                        // Se a associação tem especialidadeLocalId (String), use-o
                        associacao.localId.isNotEmpty() -> {
                            especialidadeRepositoryImpl.getEspecialidadeById(associacao.localId)
                        }
                        // Se tem especialidadeServerId (Long), use-o
                        associacao.serverId != null -> {
                            especialidadeRepositoryImpl.getEspecialidadeByServerId(associacao.serverId)
                        }
                        else -> null
                    }

                    // Converter Entity para Domain Model
                    especialidadeEntity?.let { entity ->
                        val especialidade = Especialidade(
                            localId = entity.localId,
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

    // Método para adicionar especialidade ao paciente
    fun addEspecialidadeToPaciente(pacienteLocalId: String, especialidadeLocalId: String) {
        viewModelScope.launch {
            try {
                _error.value = null

                // Criar a associação
                pacienteEspecialidadeRepositoryImpl.addEspecialidadeToPaciente(
                    pacienteLocalId = pacienteLocalId,
                    especialidadeLocalId = especialidadeLocalId
                )

                // Recarregar as especialidades
                loadPacienteEspecialidades(pacienteLocalId)

            } catch (e: Exception) {
                _error.value = "Erro ao adicionar especialidade: ${e.message}"
            }
        }
    }

    // Método para remover especialidade do paciente
    fun removeEspecialidadeFromPaciente(pacienteLocalId: String, especialidadeLocalId: String) {
        viewModelScope.launch {
            try {
                _error.value = null

                // Remover a associação
                pacienteEspecialidadeRepositoryImpl.removeEspecialidadeFromPaciente(
                    pacienteLocalId = pacienteLocalId,
                    especialidadeLocalId = especialidadeLocalId
                )

                // Recarregar as especialidades
                loadPacienteEspecialidades(pacienteLocalId)

            } catch (e: Exception) {
                _error.value = "Erro ao remover especialidade: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}