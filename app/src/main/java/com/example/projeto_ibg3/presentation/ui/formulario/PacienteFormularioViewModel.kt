package com.example.projeto_ibg3.presentation.ui.formulario

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projeto_ibg3.data.local.database.dao.EspecialidadeDao
import com.example.projeto_ibg3.data.local.database.entities.EspecialidadeEntity
import com.example.projeto_ibg3.domain.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PacienteFormularioViewModel @Inject constructor(
    private val especialidadeDao: EspecialidadeDao,
    private val syncRepository: SyncRepository
) : ViewModel() {

    companion object {
        private const val TAG = "PacienteFormularioVM"
    }

    // Estados observáveis
    private val _especialidades = MutableStateFlow<List<EspecialidadeEntity>>(emptyList())
    val especialidades: StateFlow<List<EspecialidadeEntity>> = _especialidades.asStateFlow()

    private val _isLoadingEspecialidades = MutableStateFlow(false)
    val isLoadingEspecialidades: StateFlow<Boolean> = _isLoadingEspecialidades.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadEspecialidades()
    }

    /**
     * Carrega especialidades do banco local
     * Se não houver especialidades, faz sincronização com a API
     */
    fun loadEspecialidades() {
        viewModelScope.launch {
            try {
                _isLoadingEspecialidades.value = true
                _errorMessage.value = null

                Log.d(TAG, "Carregando especialidades do banco local...")

                // Primeiro, tenta carregar do banco local
                val localEspecialidades = especialidadeDao.getAllEspecialidadesList()
                Log.d(TAG, "Especialidades encontradas no banco local: ${localEspecialidades.size}")

                if (localEspecialidades.isNotEmpty()) {
                    // Tem especialidades no banco, usar elas
                    _especialidades.value = localEspecialidades
                    Log.d(TAG, "Especialidades carregadas do banco local:")
                    localEspecialidades.forEach { esp ->
                        Log.d(TAG, "  - ${esp.nome} (localId: ${esp.localId}, serverId: ${esp.serverId})")
                    }
                } else {
                    // Não tem especialidades, sincronizar com API
                    Log.d(TAG, "Nenhuma especialidade encontrada no banco local. Iniciando sincronização...")
                    syncEspecialidadesFromApi()
                }

            } catch (e: Exception) {
                Log.e(TAG, "Erro ao carregar especialidades", e)
                _errorMessage.value = "Erro ao carregar especialidades: ${e.message}"
            } finally {
                _isLoadingEspecialidades.value = false
            }
        }
    }

    //Força a sincronização das especialidades com a API
    fun refreshEspecialidades() {
        viewModelScope.launch {
            try {
                _isLoadingEspecialidades.value = true
                _errorMessage.value = null

                Log.d(TAG, "Forçando sincronização de especialidades...")
                syncEspecialidadesFromApi()

            } catch (e: Exception) {
                Log.e(TAG, "Erro ao atualizar especialidades", e)
                _errorMessage.value = "Erro ao atualizar especialidades: ${e.message}"
            } finally {
                _isLoadingEspecialidades.value = false
            }
        }
    }

    //Sincroniza especialidades com a API
    private suspend fun syncEspecialidadesFromApi() {
        Log.d(TAG, "Iniciando sincronização com API...")

        try {
            // Usar o método de sincronização do repository
            val result = syncRepository.syncEspecialidades()

            if (result.isSuccess) {
                Log.d(TAG, "Sincronização com API bem-sucedida")

                // Recarregar especialidades do banco após sincronização
                val updatedEspecialidades = especialidadeDao.getAllEspecialidadesList()
                _especialidades.value = updatedEspecialidades

                Log.d(TAG, "Especialidades após sincronização: ${updatedEspecialidades.size}")
                updatedEspecialidades.forEach { esp ->
                    Log.d(TAG, "  - ${esp.nome} (localId: ${esp.localId}, serverId: ${esp.serverId})")
                }

            } else {
                val error = result.exceptionOrNull()?.message ?: "Erro desconhecido na sincronização"
                Log.e(TAG, "Falha na sincronização: $error")
                _errorMessage.value = "Falha na sincronização: $error"

                // Como fallback, tentar carregar especialidades que já existem no banco
                val fallbackEspecialidades = especialidadeDao.getAllEspecialidadesList()
                if (fallbackEspecialidades.isNotEmpty()) {
                    _especialidades.value = fallbackEspecialidades
                    Log.d(TAG, "Usando especialidades existentes como fallback: ${fallbackEspecialidades.size}")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Erro durante sincronização com API", e)
            _errorMessage.value = "Erro de conexão: ${e.message}"

            // Como fallback, tentar carregar especialidades que já existem no banco
            try {
                val fallbackEspecialidades = especialidadeDao.getAllEspecialidadesList()
                if (fallbackEspecialidades.isNotEmpty()) {
                    _especialidades.value = fallbackEspecialidades
                    Log.d(TAG, "Usando especialidades existentes como fallback: ${fallbackEspecialidades.size}")
                }
            } catch (fallbackError: Exception) {
                Log.e(TAG, "Erro no fallback", fallbackError)
            }
        }
    }

    //Sincroniza paciente atualizado E seus relacionamentos de forma sequencial
    fun syncPacienteAtualizadoCompleto(pacienteLocalId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🎯 Iniciando sincronização COMPLETA para paciente atualizado: $pacienteLocalId")

                // 1. Primeiro sincronizar o paciente
                Log.d(TAG, "📋 Etapa 1: Sincronizando dados básicos...")
                val pacienteResult = syncRepository.syncPacientesUpdated()

                if (pacienteResult.isFailure) {
                    Log.e(TAG, "❌ Falha na sincronização do paciente: ${pacienteResult.exceptionOrNull()?.message}")
                    _errorMessage.value = "Erro ao sincronizar paciente: ${pacienteResult.exceptionOrNull()?.message}"
                    return@launch
                }

                Log.d(TAG, "✅ Dados básicos sincronizados com sucesso")

                // Aguardar um pouco para garantir que o servidor processou
                delay(2000)

                // 2. Depois sincronizar os relacionamentos
                Log.d(TAG, "🔗 Etapa 2: Sincronizando relacionamentos...")
                val relacionamentosResult = syncRepository.syncPacienteRelationshipsOnly(pacienteLocalId)

                if (relacionamentosResult.isFailure) {
                    Log.w(TAG, "⚠️ Falha parcial nos relacionamentos: ${relacionamentosResult.exceptionOrNull()?.message}")
                } else {
                    Log.d(TAG, "✅ Relacionamentos sincronizados com sucesso")
                }

                // 3. Atualizar especialidades para refletir fichas atualizadas
                Log.d(TAG, "🔄 Etapa 3: Atualizando especialidades...")
                refreshEspecialidades()

                Log.d(TAG, "🎉 Sincronização completa do paciente atualizado finalizada")

            } catch (e: Exception) {
                Log.e(TAG, "💥 Erro na sincronização completa", e)
                _errorMessage.value = "Erro na sincronização completa: ${e.message}"
            }
        }
    }

    //Limpa mensagens de erro
    fun clearError() {
        _errorMessage.value = null
    }

    //Verifica se uma especialidade está habilitada nas configurações
    fun isEspecialidadeEnabled(especialidade: String, sharedPreferences: android.content.SharedPreferences): Boolean {
        return sharedPreferences.getBoolean(
            "especialidade_${especialidade.lowercase().replace(" ", "_")}",
            true
        )
    }

    // Filtra especialidades baseado nas configurações do usuário
    fun getFilteredEspecialidades(sharedPreferences: android.content.SharedPreferences): List<EspecialidadeEntity> {
        return _especialidades.value.filter { especialidade ->
            // Primeiro, verificar se está habilitada nas configurações
            val isEnabled = isEspecialidadeEnabled(especialidade.nome, sharedPreferences)

            // Log para debug
            Log.d(TAG, "Especialidade ${especialidade.nome}: enabled=$isEnabled, fichas=${especialidade.fichas}, available=${especialidade.isAvailable()}")

            // Retornar apenas se estiver habilitada nas configurações
            // (independente se tem fichas ou não - isso será tratado na UI)
            isEnabled
        }
    }

    // método para obter apenas especialidades disponíveis (com fichas)
    fun getEspecialidadesDisponiveis(sharedPreferences: android.content.SharedPreferences): List<EspecialidadeEntity> {
        return getFilteredEspecialidades(sharedPreferences).filter { it.isAvailable() }
    }

    // método para verificar se há especialidades disponíveis
    fun hasEspecialidadesDisponiveis(sharedPreferences: android.content.SharedPreferences): Boolean {
        return getEspecialidadesDisponiveis(sharedPreferences).isNotEmpty()
    }

    //Sincroniza apenas os relacionamentos de um paciente específico
    fun syncPacienteRelationships(pacienteLocalId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔗 Iniciando sincronização de relacionamentos para: $pacienteLocalId")

                val result = syncRepository.syncPacienteRelationshipsOnly(pacienteLocalId)

                if (result.isSuccess) {
                    Log.d(TAG, "✅ Sincronização de relacionamentos concluída com sucesso")
                } else {
                    Log.w(TAG, "⚠️ Falha na sincronização de relacionamentos: ${result.exceptionOrNull()?.message}")
                }

            } catch (e: Exception) {
                Log.e(TAG, "💥 Erro na sincronização de relacionamentos", e)
            }
        }
    }

    /**
     * Sincroniza novo paciente
     */
    fun syncNovoPaciente() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🆕 Iniciando sincronização de novo paciente...")

                // Chamar o método específico para novos pacientes
                val result = syncRepository.syncNovosPacientes()

                if (result.isSuccess) {
                    Log.d(TAG, "✅ Sincronização de novo paciente concluída com sucesso")
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Erro desconhecido"
                    Log.e(TAG, "❌ Erro na sincronização de novo paciente: $error")
                    _errorMessage.value = "Erro na sincronização: $error"
                }

            } catch (e: Exception) {
                Log.e(TAG, "💥 Exceção ao sincronizar novo paciente", e)
                _errorMessage.value = "Erro na sincronização: ${e.message}"
            }
        }
    }

}