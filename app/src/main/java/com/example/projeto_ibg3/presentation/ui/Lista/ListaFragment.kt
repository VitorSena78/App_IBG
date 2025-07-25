package com.example.projeto_ibg3.presentation.ui.Lista

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.projeto_ibg3.R
import com.example.projeto_ibg3.presentation.ui.Lista.adapter.PacienteAdapter
import com.example.projeto_ibg3.presentation.ui.Lista.adapter.PacienteAdapterCallback
import com.example.projeto_ibg3.domain.model.Paciente
import com.example.projeto_ibg3.domain.model.SyncState
import com.example.projeto_ibg3.domain.model.SyncStatus
import com.example.projeto_ibg3.presentation.ui.Lista.adapter.ExtendedPacienteAdapterCallback
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AndroidEntryPoint
class ListaFragment : Fragment(), ExtendedPacienteAdapterCallback {

    private lateinit var recyclerPacientes: RecyclerView
    private lateinit var btnAddPaciente: MaterialButton
    private lateinit var etSearch: TextInputEditText
    private lateinit var tvTotalPacientes: TextView
    private lateinit var layoutEmptyState: LinearLayout

    // Componentes de sincronização
    private lateinit var cardSyncStatus: MaterialCardView
    private lateinit var progressSync: CircularProgressIndicator
    private lateinit var tvSyncStatus: TextView
    private lateinit var tvSyncDetails: TextView
    private lateinit var btnRetrySync: MaterialButton

    private lateinit var pacienteAdapter: PacienteAdapter
    private val viewModel: ListaViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_paciente_lista, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews(view)
        setupRecyclerView()
        setupSearch()
        setupSyncComponents()
        observeViewModel()

        // Carregar dados iniciais (a sincronização já será automática)
        viewModel.loadPacientes()
    }

    private fun setupViews(view: View) {
        recyclerPacientes = view.findViewById(R.id.recycler_pacientes)
        btnAddPaciente = view.findViewById(R.id.btn_add_paciente)
        etSearch = view.findViewById(R.id.et_search)
        tvTotalPacientes = view.findViewById(R.id.tv_total_pacientes)
        layoutEmptyState = view.findViewById(R.id.layout_empty_state)

        // Componentes de sincronização
        cardSyncStatus = view.findViewById(R.id.card_sync_status)
        progressSync = view.findViewById(R.id.progress_sync)
        tvSyncStatus = view.findViewById(R.id.tv_sync_status)
        tvSyncDetails = view.findViewById(R.id.tv_sync_details)
        btnRetrySync = view.findViewById(R.id.btn_retry_sync)

        btnAddPaciente.setOnClickListener {
            navigateToAddPaciente()
        }
    }

    private fun setupRecyclerView() {
        pacienteAdapter = PacienteAdapter(this)

        recyclerPacientes.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = pacienteAdapter

            // Adicionar animações suaves
            itemAnimator = DefaultItemAnimator().apply {
                addDuration = 300
                removeDuration = 300
                moveDuration = 300
                changeDuration = 300
            }
        }
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener { text ->
            val query = text?.toString()?.trim() ?: ""
            viewModel.updateSearchQuery(query)
        }
    }

    private fun setupSyncComponents() {
        btnRetrySync.setOnClickListener {
            viewModel.retryFailedSync()
        }
    }

    private fun observeViewModel() {
        // Observar lista de pacientes
        viewModel.pacientes.onEach { pacientes ->
            pacienteAdapter.submitList(pacientes)
            updateEmptyState(pacientes.isEmpty())
            updateTotalCount(pacientes.size)
        }.launchIn(lifecycleScope)

        // Observar estados de loading e erro
        viewModel.isLoading.onEach { isLoading ->
            // Mostrar/ocultar indicador de loading se necessário
        }.launchIn(lifecycleScope)

        viewModel.error.onEach { error ->
            error?.let {
                showError(it)
                viewModel.clearError()
            }
        }.launchIn(lifecycleScope)

        // Observar estado de sincronização
        viewModel.syncState.onEach { syncState ->
            updateSyncStatus(syncState)
        }.launchIn(lifecycleScope)

        // Observar estatísticas de sincronização
        viewModel.syncStats.onEach { stats ->
            updateSyncStats(stats)
        }.launchIn(lifecycleScope)

        // Observar estatísticas da lista
        viewModel.getListStats().onEach { stats ->
            updateListStats(stats)
        }.launchIn(lifecycleScope)
    }

    private fun updateSyncStatus(syncState: SyncState) {
        when {
            syncState.isSyncing -> {
                showSyncStatus(
                    message = "Sincronizando...",
                    showProgress = true,
                    showRetry = false
                )
            }

            syncState.error != null -> {
                showSyncStatus(
                    message = "Erro na sincronização",
                    details = syncState.error,
                    showProgress = false,
                    showRetry = true
                )
            }

            syncState.isComplete && syncState.hasChanges -> {
                showSyncStatus(
                    message = "Sincronização concluída",
                    showProgress = false,
                    showRetry = false
                )

                // Ocultar após 3 segundos
                view?.postDelayed({
                    hideSyncStatus()
                }, 3000)
            }

            else -> {
                hideSyncStatus()
            }
        }
    }

    private fun updateSyncStats(stats: ListaViewModel.SyncStats) {
        when {
            stats.conflicts > 0 -> {
                showSyncStatus(
                    message = "Conflitos encontrados",
                    details = "${stats.conflicts} conflito(s) precisam ser resolvidos",
                    showProgress = false,
                    showRetry = false,
                    isWarning = true
                )
            }

            stats.pendingSync > 0 && !stats.isOnline -> {
                showSyncStatus(
                    message = "Offline",
                    details = "${stats.pendingSync} item(ns) pendente(s)",
                    showProgress = false,
                    showRetry = false,
                    isWarning = true
                )
            }

            stats.pendingSync > 0 && stats.isOnline -> {
                // Não mostrar para poucos itens pendentes, deixar a sincronização automática trabalhar
                if (stats.pendingSync > 5) {
                    showSyncStatus(
                        message = "Sincronizando em background",
                        details = "${stats.pendingSync} item(ns) pendente(s)",
                        showProgress = true,
                        showRetry = false
                    )
                }
            }
        }
    }

    private fun updateListStats(stats: ListaViewModel.ListStats) {
        // Atualizar indicadores visuais na lista se necessário
        // Por exemplo, mostrar badges nos itens com problemas de sincronização
    }

    private fun showSyncStatus(
        message: String,
        details: String? = null,
        showProgress: Boolean = false,
        showRetry: Boolean = false,
        isWarning: Boolean = false
    ) {
        cardSyncStatus.visibility = View.VISIBLE
        tvSyncStatus.text = message

        if (details != null) {
            tvSyncDetails.text = details
            tvSyncDetails.visibility = View.VISIBLE
        } else {
            tvSyncDetails.visibility = View.GONE
        }

        progressSync.visibility = if (showProgress) View.VISIBLE else View.GONE
        btnRetrySync.visibility = if (showRetry) View.VISIBLE else View.GONE

        // Mudar cores baseado no tipo de status
        val backgroundColor = if (isWarning) {
            R.color.warning_background
        } else {
            R.color.sync_status_background
        }

        cardSyncStatus.setCardBackgroundColor(
            resources.getColor(backgroundColor, null)
        )
    }

    private fun hideSyncStatus() {
        cardSyncStatus.visibility = View.GONE
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        if (isEmpty) {
            recyclerPacientes.visibility = View.GONE
            layoutEmptyState.visibility = View.VISIBLE
        } else {
            recyclerPacientes.visibility = View.VISIBLE
            layoutEmptyState.visibility = View.GONE
        }
    }

    private fun updateTotalCount(count: Int) {
        val text = when (count) {
            0 -> "Nenhum paciente"
            1 -> "1 paciente"
            else -> "$count pacientes"
        }
        tvTotalPacientes.text = "Total: $text"
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun navigateToAddPaciente() {
        findNavController().navigate(R.id.action_lista_to_addPaciente)
    }

    // Implementação das callbacks do adapter
    override fun onPacienteClick(paciente: Paciente) {
        val bundle = Bundle().apply {
            putString("paciente_id", paciente.localId.toString())
        }
        findNavController().navigate(R.id.action_lista_to_pacienteDetalhe, bundle)
    }

    override fun onEditPaciente(paciente: Paciente) {
        val bundle = Bundle().apply {
            putString("pacienteLocalId", paciente.localId.toString())
        }
        findNavController().navigate(R.id.action_lista_to_editPaciente, bundle)
    }

    override fun onCallPaciente(paciente: Paciente) {
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:${paciente.telefone}")
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(),
                "Não foi possível realizar a ligação",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    override fun onViewDetails(paciente: Paciente) {
        onPacienteClick(paciente)
    }

    // Implementação dos callbacks estendidos para sincronização
    override fun onResolveConflictKeepLocal(paciente: Paciente) {
        viewModel.resolveConflictKeepLocal(paciente.localId)
        Toast.makeText(requireContext(), "Mantendo versão local", Toast.LENGTH_SHORT).show()
    }

    override fun onResolveConflictKeepServer(paciente: Paciente) {
        viewModel.resolveConflictKeepServer(paciente.localId)
        Toast.makeText(requireContext(), "Usando versão do servidor", Toast.LENGTH_SHORT).show()
    }

    override fun onRetrySync(paciente: Paciente) {
        viewModel.retryFailedSync()
        Toast.makeText(requireContext(), "Tentando sincronizar novamente", Toast.LENGTH_SHORT)
            .show()
    }

    override fun onRestorePaciente(paciente: Paciente) {
        viewModel.restorePaciente(paciente.localId)
        Toast.makeText(requireContext(), "Paciente restaurado", Toast.LENGTH_SHORT).show()
    }

    override fun onForceSyncPaciente(paciente: Paciente) {
        viewModel.syncPacientes()
        Toast.makeText(requireContext(), "Forçando sincronização", Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        // Quando o fragmento volta ao foco, verificar se há mudanças para sincronizar
        // A sincronização automática já cuidará disso, mas podemos forçar uma verificação
        viewModel.hasPendingChanges().onEach { hasPending ->
            if (hasPending) {
                android.util.Log.d("ListaFragment", "Pending changes detected on resume")
            }
        }.launchIn(lifecycleScope)
    }

    companion object {
        fun newInstance() = ListaFragment()
    }
}