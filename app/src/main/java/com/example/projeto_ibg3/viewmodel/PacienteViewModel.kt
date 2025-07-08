package com.example.projeto_ibg3.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projeto_ibg3.model.Paciente
import com.example.projeto_ibg3.data.repository.PacienteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PacienteViewModel @Inject constructor(
    private val pacienteRepository: PacienteRepository
) : ViewModel() {

    private val _allPacientes = MutableStateFlow<List<Paciente>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _selectedPaciente = MutableStateFlow<Paciente?>(null)

    val pacientes: StateFlow<List<Paciente>> = combine(
        _allPacientes,
        _searchQuery
    ) { pacientes, query ->
        if (query.isBlank()) {
            pacientes
        } else {
            pacientes.filter { paciente ->
                paciente.nome.contains(query, ignoreCase = true) ||
                        paciente.cpf.contains(query) ||
                        paciente.telefone.contains(query) ||
                        paciente.nomeDaMae.contains(query, ignoreCase = true)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val isLoading = _isLoading.asStateFlow()
    val error = _error.asStateFlow()
    val selectedPaciente = _selectedPaciente.asStateFlow()

    fun loadPacientes() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                // Coletar o Flow
                pacienteRepository.getAllPacientes().collect { pacientes ->
                    _allPacientes.value = pacientes
                }
            } catch (e: Exception) {
                _error.value = "Erro ao carregar pacientes: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun searchPacientes(query: String) {
        _searchQuery.value = query
    }

    fun insertPaciente(paciente: Paciente) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                pacienteRepository.insertPaciente(paciente)
                loadPacientes() // Recarregar lista
            } catch (e: Exception) {
                _error.value = "Erro ao adicionar paciente: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updatePaciente(paciente: Paciente) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                pacienteRepository.updatePaciente(paciente)
                loadPacientes() // Recarregar lista
                _selectedPaciente.value = paciente // Atualizar paciente selecionado
            } catch (e: Exception) {
                _error.value = "Erro ao atualizar paciente: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun deletePaciente(pacienteId: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                pacienteRepository.deletePaciente(pacienteId)
                loadPacientes() // Recarregar lista
                // Se o paciente deletado era o selecionado, limpar seleção
                if (_selectedPaciente.value?.id == pacienteId) {
                    _selectedPaciente.value = null
                }
            } catch (e: Exception) {
                _error.value = "Erro ao excluir paciente: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun getPacienteById(id: Long) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                val paciente = pacienteRepository.getPacienteById(id)
                _selectedPaciente.value = paciente
            } catch (e: Exception) {
                _error.value = "Erro ao buscar paciente: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectPaciente(paciente: Paciente) {
        _selectedPaciente.value = paciente
    }

    fun clearSelectedPaciente() {
        _selectedPaciente.value = null
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSearch() {
        _searchQuery.value = ""
    }

    // Função para validar CPF (opcional, mas útil)
    fun validateCpf(cpf: String): Boolean {
        val cleanCpf = cpf.replace(Regex("[^0-9]"), "")
        if (cleanCpf.length != 11) return false
        if (cleanCpf.all { it == cleanCpf[0] }) return false

        // Validação dos dígitos verificadores
        val digits = cleanCpf.map { it.toString().toInt() }

        // Primeiro dígito
        val sum1 = (0..8).sumOf { i -> digits[i] * (10 - i) }
        val digit1 = 11 - (sum1 % 11)
        val firstDigit = if (digit1 >= 10) 0 else digit1

        // Segundo dígito
        val sum2 = (0..9).sumOf { i -> digits[i] * (11 - i) }
        val digit2 = 11 - (sum2 % 11)
        val secondDigit = if (digit2 >= 10) 0 else digit2

        return digits[9] == firstDigit && digits[10] == secondDigit
    }

    // Função para validar campos obrigatórios
    fun validatePaciente(paciente: Paciente): String? {
        return when {
            paciente.nome.isBlank() -> "Nome é obrigatório"
            paciente.cpf.isBlank() -> "CPF é obrigatório"
            !validateCpf(paciente.cpf) -> "CPF inválido"
            paciente.telefone.isBlank() -> "Telefone é obrigatório"
            paciente.nomeDaMae.isBlank() -> "Nome da mãe é obrigatório"
            paciente.sus.isBlank() -> "Cartão SUS é obrigatório"
            paciente.endereco.isBlank() -> "Endereço é obrigatório"
            else -> null
        }
    }

    // Função para verificar se CPF já existe (evitar duplicatas)
    fun checkCpfExists(cpf: String, excludeId: Long? = null): Boolean {
        return _allPacientes.value.any { paciente ->
            paciente.cpf == cpf && paciente.id != excludeId
        }
    }

    // Função para obter estatísticas dos pacientes
    fun getPacienteStats(): Map<String, Any> {
        val pacientes = _allPacientes.value
        val totalPacientes = pacientes.size
        val averageAge = if (pacientes.isNotEmpty()) {
            val idades = pacientes.mapNotNull { it.idade }
            if (idades.isNotEmpty()) idades.average() else 0.0
        } else 0.0

        val pacientesWithVitalSigns = pacientes.count { paciente ->
            paciente.peso != null || paciente.altura != null ||
                    paciente.pressaoArterial != null || paciente.temperatura != null
        }

        return mapOf(
            "total" to totalPacientes,
            "averageAge" to averageAge,
            "withVitalSigns" to pacientesWithVitalSigns,
            "percentageWithVitalSigns" to if (totalPacientes > 0) {
                (pacientesWithVitalSigns.toDouble() / totalPacientes) * 100
            } else 0.0
        )
    }
}