package com.example.projeto_ibg3.presentation.ui.config

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projeto_ibg3.data.mappers.toDomainModelList
import com.example.projeto_ibg3.domain.model.Especialidade
import com.example.projeto_ibg3.domain.repository.EspecialidadeRepository
import com.example.projeto_ibg3.domain.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConfigViewModel @Inject constructor(
    private val especialidadeRepository: EspecialidadeRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ConfigViewModel"
    }

    private val _especialidades = MutableStateFlow<List<Especialidade>>(emptyList())
    val especialidades: StateFlow<List<Especialidade>> = _especialidades.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    // Estado combinado para mostrar loading quando necessário
    val shouldShowLoading: StateFlow<Boolean> = combine(_isLoading, _isSyncing) { loading, syncing ->
        loading || syncing
    }.let { flow ->
        val stateFlow = MutableStateFlow(false)
        viewModelScope.launch {
            flow.collect { stateFlow.value = it }
        }
        stateFlow.asStateFlow()
    }

    init {
        observeEspecialidades()
        // Carregar dados locais imediatamente, depois tentar sincronizar
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                // Verificar se há especialidades no banco local
                val count = especialidadeRepository.getEspecialidadeCount()
                Log.d(TAG, "Especialidades no banco local: $count")

                if (count == 0) {
                    // Se não há dados locais, forçar sincronização
                    Log.d(TAG, "Nenhuma especialidade local encontrada, iniciando sincronização...")
                    syncEspecialidades()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao verificar dados locais", e)
                _error.value = "Erro ao carregar dados: ${e.message}"
            }
        }
    }

    private fun observeEspecialidades() {
        viewModelScope.launch {
            especialidadeRepository.getAllEspecialidades()
                .catch { e ->
                    Log.e(TAG, "Erro ao observar especialidades", e)
                    _error.value = "Erro ao carregar especialidades: ${e.message}"
                }
                .collect { especialidadesEntity ->
                    val especialidadesList = especialidadesEntity.toDomainModelList()
                    Log.d(TAG, "Especialidades atualizadas: ${especialidadesList.size} itens")
                    _especialidades.value = especialidadesList
                }
        }
    }

    private fun syncEspecialidades() {
        viewModelScope.launch {
            _isSyncing.value = true
            _error.value = null

            try {
                Log.d(TAG, "Iniciando sincronização de especialidades...")

                syncRepository.startSyncEspecialidades()
                    .catch { e ->
                        Log.e(TAG, "Erro durante sincronização", e)
                        _error.value = "Erro na sincronização: ${e.message}"
                    }
                    .collect { syncState ->
                        Log.d(TAG, "Estado da sincronização: ${syncState.message}")

                        if (syncState.error != null) {
                            _error.value = syncState.error
                        }

                        if (!syncState.isLoading) {
                            _isSyncing.value = false

                            if (syncState.error == null) {
                                Log.d(TAG, "Sincronização concluída com sucesso")
                            }
                        }
                    }

            } catch (e: Exception) {
                Log.e(TAG, "Erro inesperado durante sincronização", e)
                _error.value = "Erro inesperado: ${e.message}"
                _isSyncing.value = false
            }
        }
    }

    fun refreshEspecialidades() {
        Log.d(TAG, "Refresh solicitado pelo usuário")
        syncEspecialidades()
    }

    fun clearError() {
        _error.value = null
        syncRepository.clearError()
    }

    // Método para inserir especialidades padrão caso não existam no servidor
    fun insertDefaultEspecialidades() {
        viewModelScope.launch {
            try {
                val defaultEspecialidades = getDefaultEspecialidades()

                for (nome in defaultEspecialidades) {
                    // Verificar se já existe
                    val existing = especialidadeRepository.getEspecialidadeByName(nome)
                    if (existing == null) {
                        val especialidade = Especialidade(
                            localId = "", // Será gerado automaticamente
                            serverId = null,
                            nome = nome
                        )
                        especialidadeRepository.insertEspecialidade(especialidade)
                        Log.d(TAG, "Inserida especialidade padrão: $nome")
                    }
                }

                Log.d(TAG, "Especialidades padrão inseridas")
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao inserir especialidades padrão", e)
                _error.value = "Erro ao criar especialidades padrão: ${e.message}"
            }
        }
    }

    private fun getDefaultEspecialidades(): List<String> {
        return listOf(
            "Cardiologia",
            "Pediatria",
            "Clínico Geral",
            "Neurologia",
            "Ginecologia",
            "Dermatologia",
            "Ortopedia",
            "Endocrinologia",
            "Oftalmologia",
            "Psiquiatria"
        )
    }

    // Métodos para debugging
    fun getDebugInfo(): String {
        return buildString {
            appendLine("=== DEBUG INFO ===")
            appendLine("Especialidades carregadas: ${_especialidades.value.size}")
            appendLine("Está sincronizando: ${_isSyncing.value}")
            appendLine("Está carregando: ${_isLoading.value}")
            appendLine("Erro atual: ${_error.value}")

            _especialidades.value.forEach { esp ->
                appendLine("- ${esp.nome} (localId: ${esp.localId}, serverId: ${esp.serverId})")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "ViewModel sendo destruído")
    }
}