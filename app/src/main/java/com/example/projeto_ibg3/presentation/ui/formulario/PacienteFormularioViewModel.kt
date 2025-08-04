package com.example.projeto_ibg3.presentation.ui.formulario

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projeto_ibg3.data.local.database.dao.EspecialidadeDao
import com.example.projeto_ibg3.data.local.database.entities.EspecialidadeEntity
import com.example.projeto_ibg3.data.remote.validation.ValidationResult
import com.example.projeto_ibg3.data.repository.impl.SyncRepositoryImpl
import com.example.projeto_ibg3.domain.model.EspecialidadeStats
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

    // método para obter especialidades esgotadas
    fun getEspecialidadesEsgotadas(sharedPreferences: android.content.SharedPreferences): List<EspecialidadeEntity> {
        return getFilteredEspecialidades(sharedPreferences).filter { it.isEsgotada() && !it.isDeleted }
    }

    // método para verificar se há especialidades disponíveis
    fun hasEspecialidadesDisponiveis(sharedPreferences: android.content.SharedPreferences): Boolean {
        return getEspecialidadesDisponiveis(sharedPreferences).isNotEmpty()
    }

    // Método para obter estatísticas das especialidades
    fun getEspecialidadesStats(sharedPreferences: android.content.SharedPreferences): EspecialidadeStats {
        val filtered = getFilteredEspecialidades(sharedPreferences)
        return EspecialidadeStats(
            total = filtered.size,
            disponiveis = filtered.count { it.isAvailable() },
            esgotadas = filtered.count { it.isEsgotada() },
            totalFichas = filtered.sumOf { it.fichas },
            comPoucasFichas = filtered.count { it.fichas in 1..5 }
        )
    }

    // NOVO: Método para obter especialidades com poucas fichas (aviso)
    fun getEspecialidadesComPoucasFichas(sharedPreferences: android.content.SharedPreferences, limite: Int = 5): List<EspecialidadeEntity> {
        return getFilteredEspecialidades(sharedPreferences).filter {
            it.fichas in 1..limite && !it.isDeleted
        }
    }

    // NOVO: Método para verificar se uma especialidade específica está disponível
    fun isEspecialidadeDisponivel(nomeEspecialidade: String, sharedPreferences: android.content.SharedPreferences): Boolean {
        val especialidade = _especialidades.value.find { it.nome == nomeEspecialidade }
        return especialidade?.let {
            isEspecialidadeEnabled(it.nome, sharedPreferences) && it.isAvailable()
        } ?: false
    }

    // NOVO: Método para obter informações detalhadas de uma especialidade
    fun getEspecialidadeInfo(nomeEspecialidade: String): EspecialidadeEntity? {
        return _especialidades.value.find { it.nome == nomeEspecialidade }
    }

    // NOVO: Método para validar se as especialidades selecionadas ainda estão disponíveis
    fun validateSelectedEspecialidades(
        selectedEspecialidades: List<String>,
        sharedPreferences: android.content.SharedPreferences
    ): ValidationResult {
        val unavailableEspecialidades = mutableListOf<String>()
        val warningEspecialidades = mutableListOf<String>()

        selectedEspecialidades.forEach { nome ->
            val especialidade = getEspecialidadeInfo(nome)
            when {
                especialidade == null -> unavailableEspecialidades.add(nome)
                !especialidade.isAvailable() -> unavailableEspecialidades.add(nome)
                especialidade.fichas <= 5 -> warningEspecialidades.add("$nome (${especialidade.fichas} fichas restantes)")
            }
        }

        return ValidationResult(
            isValid = unavailableEspecialidades.isEmpty(),
            unavailableEspecialidades = unavailableEspecialidades,
            warningEspecialidades = warningEspecialidades
        )
    }

    // NOVO: Método para decrementar fichas localmente (para feedback imediato)
    fun decrementarFichasLocalmente(nomeEspecialidade: String) {
        viewModelScope.launch {
            try {
                val especialidade = especialidadeDao.getEspecialidadeByName(nomeEspecialidade)
                if (especialidade != null && especialidade.fichas > 0) {
                    especialidadeDao.updateFichas(
                        especialidade.localId,
                        especialidade.fichas - 1
                    )
                    Log.d(TAG, "Fichas decrementadas localmente para $nomeEspecialidade: ${especialidade.fichas - 1}")

                    // Recarregar especialidades para atualizar a UI
                    loadEspecialidades()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao decrementar fichas localmente", e)
            }
        }
    }

    // NOVO: Método para incrementar fichas localmente
    fun incrementarFichasLocalmente(nomeEspecialidade: String) {
        viewModelScope.launch {
            try {
                val especialidade = especialidadeDao.getEspecialidadeByName(nomeEspecialidade)
                if (especialidade != null) {
                    especialidadeDao.updateFichas(
                        especialidade.localId,
                        especialidade.fichas + 1
                    )
                    Log.d(TAG, "Fichas incrementadas localmente para $nomeEspecialidade: ${especialidade.fichas + 1}")

                    // Recarregar especialidades para atualizar a UI
                    loadEspecialidades()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao incrementar fichas localmente", e)
            }
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
     * Sincroniza apenas os relacionamentos de um paciente específico
     */
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
}