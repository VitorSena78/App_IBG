package com.example.projeto_ibg3.presentation.ui.lista

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projeto_ibg3.domain.model.Paciente
import com.example.projeto_ibg3.domain.model.SyncState
import com.example.projeto_ibg3.domain.model.SyncStatus
import com.example.projeto_ibg3.domain.repository.PacienteRepository
import com.example.projeto_ibg3.domain.repository.SyncRepository
import com.example.projeto_ibg3.sync.model.SyncStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class ListaViewModel @Inject constructor(
    private val pacienteRepository: PacienteRepository,
    private val syncRepository: SyncRepository
) : ViewModel() {

    // ==================== ESTADOS PRIVADOS ====================

    private val _searchQuery = MutableStateFlow("")
    private val _isLoading = MutableStateFlow(false)
    private val _error = MutableStateFlow<String?>(null)
    private val _isRefreshing = MutableStateFlow(false)

    // ==================== ESTADOS PÚBLICOS ====================

    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    val error: StateFlow<String?> = _error.asStateFlow()

    // Estados de sincronização vindos do SyncRepository
    val syncState: Flow<SyncState> = syncRepository.syncState

    // ==================== DADOS PRINCIPAIS ====================

    // lista de pacientes filtrada pela busca
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val pacientes: StateFlow<List<Paciente>> = _searchQuery
        .debounce(300) // Aguarda 300ms após a última digitação
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isEmpty()) {
                pacienteRepository.getAllPacientes()
            } else {
                pacienteRepository.searchPacientes(query)
            }
        }
        .catch { exception ->
            _error.value = exception.message ?: "Erro ao carregar pacientes"
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Estatísticas de sincronização
    val syncStats: StateFlow<SyncStats> = combine(
        pacienteRepository.getPendingSyncCount(),
        pacienteRepository.getConflictCount(),
        syncState
    ) { pendingCount, conflictCount, syncState ->
        SyncStats(
            pendingSync = pendingCount,
            conflicts = conflictCount,
            isOnline = syncState.error == null,
            lastSync = syncState.lastSyncTime
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SyncStats()
    )

    // ==================== INICIALIZAÇÃO ====================

    init {
        // Carregar dados iniciais
        loadPacientes()

        // Iniciar sincronização automática em background
        startAutoSync()

        // Observar mudanças de conectividade
        observeConnectivityChanges()

        // Observar mudanças nos dados para sincronização
        observeDataChangesForSync()
    }

    // ==================== MÉTODOS PÚBLICOS ====================

    //Carrega a lista de pacientes
    fun loadPacientes() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null

                // Os dados são carregados automaticamente através do Flow
                // Este método serve principalmente para controlar o estado de loading

            } catch (e: Exception) {
                _error.value = e.message ?: "Erro ao carregar pacientes"
            } finally {
                _isLoading.value = false
            }
        }
    }

    //Atualiza a query de busca
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    //Atualiza a lista (pull-to-refresh)
    fun refresh() {
        viewModelScope.launch {
            try {
                _isRefreshing.value = true
                _error.value = null

                // Forçar sincronização imediata
                forceSyncAll()

            } catch (e: Exception) {
                _error.value = e.message ?: "Erro ao atualizar dados"
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    //Força uma sincronização completa
    fun forceSyncAll() {
        viewModelScope.launch {
            syncRepository.startSync().collect {
                // Estado observado automaticamente
            }
        }
    }

    //Sincroniza apenas pacientes
    fun syncPacientes() {
        viewModelScope.launch {
            syncRepository.startSyncPacientes().collect {
                // Estado observado automaticamente
            }
        }
    }

    //Restaura um paciente deletado
    fun restorePaciente(pacienteLocalId: String) {
        viewModelScope.launch {
            try {
                pacienteRepository.restorePaciente(pacienteLocalId)
                clearError()
            } catch (e: Exception) {
                _error.value = e.message ?: "Erro ao restaurar paciente"
            }
        }
    }

    //Resolve conflito de sincronização escolhendo a versão local
    fun resolveConflictKeepLocal(pacienteLocalId: String) {
        viewModelScope.launch {
            try {
                pacienteRepository.resolveConflictKeepLocal(pacienteLocalId)
                clearError()
            } catch (e: Exception) {
                _error.value = e.message ?: "Erro ao resolver conflito"
            }
        }
    }

    //Resolve conflito de sincronização escolhendo a versão do servidor
    fun resolveConflictKeepServer(pacienteLocalId: String) {
        viewModelScope.launch {
            try {
                pacienteRepository.resolveConflictKeepServer(pacienteLocalId)
                clearError()
            } catch (e: Exception) {
                _error.value = e.message ?: "Erro ao resolver conflito"
            }
        }
    }

    //Tenta novamente a sincronização de itens com falha
    fun retryFailedSync() {
        viewModelScope.launch {
            try {
                pacienteRepository.retryFailedSync()
                // Após retry, sincronizar novamente
                syncRepository.startSyncPacientes().collect {
                    // Estado observado automaticamente
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Erro ao tentar novamente sincronização"
            }
        }
    }

    //Limpa mensagens de erro
    fun clearError() {
        _error.value = null
        syncRepository.clearError()
    }

    //Verifica se há mudanças pendentes
    fun hasPendingChanges(): Flow<Boolean> {
        return flow {
            emit(syncRepository.hasPendingChanges())
        }
    }

    // ==================== MÉTODOS PRIVADOS PARA SINCRONIZAÇÃO AUTOMÁTICA ====================

    //Inicia sincronização automática em background
    private fun startAutoSync() {
        viewModelScope.launch {
            try {
                // Sincronização inicial após 2 segundos
                delay(2000)
                performBackgroundSync()

                // Sincronização periódica a cada 5 minutos
                while (true) {
                    delay(300_000) // 5 minutos
                    performBackgroundSync()
                }
            } catch (e: Exception) {
                android.util.Log.e("ListaViewModel", "Auto sync error", e)
            }
        }
    }

    //Executa sincronização em background
    private suspend fun performBackgroundSync() {
        try {
            // Verifica se há dados pendentes para sincronizar
            val hasPending = syncRepository.hasPendingChanges()

            if (hasPending) {
                android.util.Log.d("ListaViewModel", "Starting background sync - pending changes detected")

                syncRepository.startSync().collect { syncState ->
                    when {
                        syncState.error != null -> {
                            android.util.Log.w("ListaViewModel", "Background sync error: ${syncState.error}")
                        }
                        syncState.isComplete -> {
                            android.util.Log.d("ListaViewModel", "Background sync completed successfully")
                        }
                    }
                }
            } else {
                // Mesmo sem mudanças pendentes, fazer uma sincronização leve para buscar atualizações do servidor
                android.util.Log.d("ListaViewModel", "Starting light background sync - checking for server updates")

                syncRepository.startSyncPacientes().collect { syncState ->
                    // Log apenas, sem interferir na UI
                    if (syncState.isComplete) {
                        android.util.Log.d("ListaViewModel", "Light background sync completed")
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("ListaViewModel", "Background sync failed", e)
        }
    }

    //Observa mudanças nos dados para acionar sincronização
    private fun observeDataChangesForSync() {
        viewModelScope.launch {
            // Observa mudanças no número de itens pendentes
            pacienteRepository.getPendingSyncCount()
                .distinctUntilChanged()
                .collect { pendingCount ->
                    if (pendingCount > 0) {
                        android.util.Log.d("ListaViewModel", "Data changes detected, scheduling sync")

                        // Aguarda um pouco para agrupar múltiplas mudanças
                        delay(5000) // 5 segundos

                        // Verifica se ainda há mudanças pendentes
                        if (syncRepository.hasPendingChanges()) {
                            performBackgroundSync()
                        }
                    }
                }
        }
    }

    //Observa mudanças de conectividade para sincronização automática
    private fun observeConnectivityChanges() {
        viewModelScope.launch {
            // Observa o estado de sincronização para detectar mudanças de conectividade
            syncState
                .map { it.error == null } // isOnline
                .distinctUntilChanged()
                .collect { isOnline ->
                    if (isOnline) {
                        android.util.Log.d("ListaViewModel", "Connectivity restored, starting sync")

                        // Aguarda um pouco para estabilizar a conexão
                        delay(3000)

                        // Sincronizar quando a conectividade for restaurada
                        performBackgroundSync()
                    } else {
                        android.util.Log.d("ListaViewModel", "Connectivity lost")
                    }
                }
        }
    }

    // ==================== MÉTODOS DE UTILIDADE ====================

    //Obtém estatísticas rápidas da lista atual
    fun getListStats(): Flow<ListStats> {
        return pacientes.map { list ->
            ListStats(
                totalPacientes = list.size,
                pacientesComConflito = list.count { it.syncStatus == SyncStatus.CONFLICT },
                pacientesPendentes = list.count {
                    it.syncStatus in listOf(
                        SyncStatus.PENDING_UPLOAD,
                        SyncStatus.PENDING_DELETE,
                        SyncStatus.UPLOAD_FAILED
                    )
                },
                pacientesSincronizados = list.count { it.syncStatus == SyncStatus.SYNCED }
            )
        }
    }

    // ==================== CLEANUP ====================

    override fun onCleared() {
        super.onCleared()
        android.util.Log.d("ListaViewModel", "ViewModel cleared, stopping background sync")
    }
}