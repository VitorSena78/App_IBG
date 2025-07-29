package com.example.projeto_ibg3.presentation.ui.formulario

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projeto_ibg3.data.local.database.dao.EspecialidadeDao
import com.example.projeto_ibg3.data.local.database.entities.EspecialidadeEntity
import com.example.projeto_ibg3.data.repository.impl.SyncRepositoryImpl
import com.example.projeto_ibg3.domain.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
            isEspecialidadeEnabled(especialidade.nome, sharedPreferences)
        }
    }

    /**
     * Sincroniza paciente atualizado
     */
    fun syncPacienteUpdated() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🔄 Iniciando sincronização de paciente atualizado...")

                // Chamar o método específico para pacientes atualizados
                val result = syncRepository.syncPacientesUpdated()

                if (result.isSuccess) {
                    Log.d(TAG, "✅ Sincronização de paciente atualizado concluída com sucesso")
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Erro desconhecido"
                    Log.e(TAG, "❌ Erro na sincronização de paciente atualizado: $error")
                    _errorMessage.value = "Erro na sincronização: $error"
                }

            } catch (e: Exception) {
                Log.e(TAG, "💥 Exceção ao sincronizar paciente atualizado", e)
                _errorMessage.value = "Erro na sincronização: ${e.message}"
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

    /**
     * Força sincronização de todos os pacientes pendentes
     */
    fun forceSyncAllPacientes() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🚀 Iniciando sincronização forçada de todos os pacientes...")

                // Chamar sincronização de todos os pacientes pendentes
                val result = syncRepository.syncAllPendingPacientes()

                if (result.isSuccess) {
                    Log.d(TAG, "✅ Sincronização forçada concluída com sucesso")
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Erro desconhecido"
                    Log.e(TAG, "❌ Erro na sincronização forçada: $error")
                    _errorMessage.value = "Erro na sincronização: $error"
                }

            } catch (e: Exception) {
                Log.e(TAG, "💥 Exceção na sincronização forçada", e)
                _errorMessage.value = "Erro na sincronização: ${e.message}"
            }
        }
    }

    /**
     * Método de debug para verificar pacientes pendentes
     */
    fun debugPacientesPendentes() {
        viewModelScope.launch {
            try {
                // Se seu SyncRepositoryImpl tiver o método debugPacientesPendentes
                if (syncRepository is SyncRepositoryImpl) {
                    syncRepository.debugPacientesPendentes()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro no debug", e)
            }
        }
    }

    /**
     * Método para teste de sincronização com logging detalhado
     */
    fun testSyncWithDetailedLogging() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "🧪 Iniciando teste de sincronização com logging detalhado...")

                // Se seu SyncRepositoryImpl tiver o método forceSyncWithDetailedLogging
                if (syncRepository is SyncRepositoryImpl) {
                    val result = syncRepository.forceSyncWithDetailedLogging()

                    if (result.isSuccess) {
                        Log.d(TAG, "✅ Teste de sincronização concluído com sucesso")
                        _errorMessage.value = null // Limpar erro anterior
                    } else {
                        val error = result.exceptionOrNull()?.message ?: "Erro desconhecido"
                        Log.e(TAG, "❌ Teste de sincronização falhou: $error")
                        _errorMessage.value = "Teste falhou: $error"
                    }
                } else {
                    Log.w(TAG, "⚠️ Método de teste não disponível para este tipo de repository")
                }

            } catch (e: Exception) {
                Log.e(TAG, "💥 Exceção no teste de sincronização", e)
                _errorMessage.value = "Erro do teste: ${e.message}"
            }
        }
    }
}