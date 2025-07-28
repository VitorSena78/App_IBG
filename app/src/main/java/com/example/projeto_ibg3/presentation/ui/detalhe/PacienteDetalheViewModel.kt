package com.example.projeto_ibg3.presentation.ui.detalhe

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projeto_ibg3.data.repository.impl.EspecialidadeRepositoryImpl
import com.example.projeto_ibg3.data.repository.impl.PacienteEspecialidadeRepositoryImpl
import com.example.projeto_ibg3.domain.model.Paciente
import com.example.projeto_ibg3.data.repository.impl.PacienteRepositoryImpl
import com.example.projeto_ibg3.domain.model.Especialidade
import com.example.projeto_ibg3.domain.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.util.Log
import com.example.projeto_ibg3.R

@HiltViewModel
class PacienteDetalheViewModel @Inject constructor(
    private val pacienteRepositoryImpl: PacienteRepositoryImpl,
    private val especialidadeRepositoryImpl: EspecialidadeRepositoryImpl,
    private val pacienteEspecialidadeRepositoryImpl: PacienteEspecialidadeRepositoryImpl,
    private val syncRepository: SyncRepository
) : ViewModel() {

    companion object {
        private const val TAG = "PacienteDetalheVM"
    }

    private val _paciente = MutableStateFlow<Paciente?>(null)
    val paciente: StateFlow<Paciente?> = _paciente.asStateFlow()

    private val _pacienteEspecialidades = MutableStateFlow<List<Especialidade>>(emptyList())
    val pacienteEspecialidades: StateFlow<List<Especialidade>> = _pacienteEspecialidades.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _syncStatusMessage = MutableStateFlow<String>("Sincronizado")
    val syncStatusMessage: StateFlow<String> = _syncStatusMessage.asStateFlow()

    private val _syncStatusIcon = MutableStateFlow(R.drawable.ic_sync)
    val syncStatusIcon: StateFlow<Int> = _syncStatusIcon.asStateFlow()

    private val _syncStatusColor = MutableStateFlow(R.color.success)
    val syncStatusColor: StateFlow<Int> = _syncStatusColor.asStateFlow()

    // Método principal para carregar paciente por localId com sincronização
    fun loadPaciente(pacienteLocalId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                Log.d(TAG, "Carregando paciente: $pacienteLocalId")

                // 1. Carregar dados locais primeiro
                val paciente = pacienteRepositoryImpl.getPacienteById(pacienteLocalId)
                _paciente.value = paciente

                if (paciente == null) {
                    _error.value = "Paciente não encontrado"
                    return@launch
                }

                // 2. Carregar especialidades locais
                loadPacienteEspecialidadesLocal(pacienteLocalId)

                // 3. Iniciar sincronização em background
                syncPacienteData(pacienteLocalId)

            } catch (e: Exception) {
                Log.e(TAG, "Erro ao carregar paciente", e)
                _error.value = "Erro ao carregar paciente: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    // Método para sincronizar dados do paciente
    private fun syncPacienteData(pacienteLocalId: String) {
        viewModelScope.launch {
            try {
                _isSyncing.value = true
                updateSyncStatus("Sincronizando especialidades...", R.drawable.ic_sync, R.color.primary)

                Log.d(TAG, "Iniciando sincronização para paciente: $pacienteLocalId")

                // 1. Sincronizar especialidades primeiro
                val especialidadesResult = syncRepository.syncEspecialidades()
                if (especialidadesResult.isFailure) {
                    Log.w(TAG, "Falha na sincronização de especialidades: ${especialidadesResult.exceptionOrNull()?.message}")
                    updateSyncStatus("Falha ao sincronizar especialidades", R.drawable.ic_error, R.color.error)
                    return@launch
                }

                updateSyncStatus("Enviando relacionamentos...", R.drawable.ic_sync, R.color.primary)

                // 2. Sincronizar relacionamentos (upload pendentes)
                val uploadResult = syncUploadPendingRelationships()
                if (uploadResult.isFailure) {
                    Log.w(TAG, "Falha no upload de relacionamentos: ${uploadResult.exceptionOrNull()?.message}")
                    updateSyncStatus("Falha no upload", R.drawable.ic_sync_problem, R.color.warning)
                } else {
                    updateSyncStatus("Baixando atualizações...", R.drawable.ic_sync, R.color.primary)
                }

                // 3. Sincronizar relacionamentos (download atualizados)
                val downloadResult = syncDownloadUpdatedRelationships()
                if (downloadResult.isFailure) {
                    Log.w(TAG, "Falha no download de relacionamentos: ${downloadResult.exceptionOrNull()?.message}")
                    updateSyncStatus("Falha no download", R.drawable.ic_sync_problem, R.color.warning)
                } else {
                    updateSyncStatus("Sincronizado", R.drawable.ic_sync, R.color.success)
                }

                // 4. Recarregar especialidades após sincronização
                loadPacienteEspecialidadesLocal(pacienteLocalId)

                Log.d(TAG, "Sincronização concluída para paciente: $pacienteLocalId")

            } catch (e: Exception) {
                Log.e(TAG, "Erro na sincronização", e)
                updateSyncStatus("Erro na sincronização", R.drawable.ic_error, R.color.error)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    private fun updateSyncStatus(message: String, icon: Int, color: Int) {
        _syncStatusMessage.value = message
        _syncStatusIcon.value = icon
        _syncStatusColor.value = color
    }

    // Método privado para usar os métodos do SyncRepository
    private suspend fun syncUploadPendingRelationships(): Result<Unit> {
        return try {
            // Usar o método específico do SyncRepository
            syncRepository.uploadPacienteEspecialidadesPending()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun syncDownloadUpdatedRelationships(): Result<Unit> {
        return try {
            // Usar o método específico do SyncRepository
            syncRepository.downloadPacienteEspecialidadesUpdated()
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Método para carregar especialidades localmente (sem sync)
    private fun loadPacienteEspecialidadesLocal(pacienteLocalId: String) {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Carregando especialidades locais para paciente: $pacienteLocalId")

                // Buscar as associações paciente-especialidade usando o localId do paciente
                val pacienteEspecialidadeEntities = pacienteEspecialidadeRepositoryImpl
                    .getEspecialidadesByPacienteId(pacienteLocalId)

                Log.d(TAG, "Encontradas ${pacienteEspecialidadeEntities.size} associações")

                // Buscar os dados completos das especialidades
                val especialidades = mutableListOf<Especialidade>()

                pacienteEspecialidadeEntities.forEach { associacao ->
                    Log.d(TAG, "Processando associação: localId=${associacao.localId}, serverId=${associacao.serverId}")

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
                        Log.d(TAG, "Especialidade adicionada: ${especialidade.nome}")
                    }
                }

                _pacienteEspecialidades.value = especialidades
                Log.d(TAG, "Total de especialidades carregadas: ${especialidades.size}")

            } catch (e: Exception) {
                Log.e(TAG, "Erro ao carregar especialidades locais", e)
                _error.value = "Erro ao carregar especialidades: ${e.message}"
            }
        }
    }

    // Método público para recarregar especialidades (com sincronização)
    fun loadPacienteEspecialidades(pacienteLocalId: String) {
        loadPacienteEspecialidadesLocal(pacienteLocalId)
        // Opcionalmente, também sincronizar
        syncPacienteData(pacienteLocalId)
    }

    // Método para forçar sincronização manual
    fun forceSyncEspecialidades(pacienteLocalId: String) {
        syncPacienteData(pacienteLocalId)
    }

    // Método para adicionar especialidade ao paciente
    fun addEspecialidadeToPaciente(pacienteLocalId: String, especialidadeLocalId: String) {
        viewModelScope.launch {
            try {
                _error.value = null

                Log.d(TAG, "Adicionando especialidade $especialidadeLocalId ao paciente $pacienteLocalId")

                // Criar a associação
                pacienteEspecialidadeRepositoryImpl.addEspecialidadeToPaciente(
                    pacienteLocalId = pacienteLocalId,
                    especialidadeLocalId = especialidadeLocalId
                )

                // Recarregar as especialidades
                loadPacienteEspecialidadesLocal(pacienteLocalId)

                Log.d(TAG, "Especialidade adicionada com sucesso")

            } catch (e: Exception) {
                Log.e(TAG, "Erro ao adicionar especialidade", e)
                _error.value = "Erro ao adicionar especialidade: ${e.message}"
            }
        }
    }

    // Método para remover especialidade do paciente
    fun removeEspecialidadeFromPaciente(pacienteLocalId: String, especialidadeLocalId: String) {
        viewModelScope.launch {
            try {
                _error.value = null

                Log.d(TAG, "Removendo especialidade $especialidadeLocalId do paciente $pacienteLocalId")

                // Remover a associação
                pacienteEspecialidadeRepositoryImpl.removeEspecialidadeFromPaciente(
                    pacienteLocalId = pacienteLocalId,
                    especialidadeLocalId = especialidadeLocalId
                )

                // Recarregar as especialidades
                loadPacienteEspecialidadesLocal(pacienteLocalId)

                Log.d(TAG, "Especialidade removida com sucesso")

            } catch (e: Exception) {
                Log.e(TAG, "Erro ao remover especialidade", e)
                _error.value = "Erro ao remover especialidade: ${e.message}"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSyncMessage() {
        // Método removido - não precisamos mais limpar mensagem
    }
}