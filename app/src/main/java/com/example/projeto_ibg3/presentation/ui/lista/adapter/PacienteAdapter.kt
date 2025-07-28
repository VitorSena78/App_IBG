package com.example.projeto_ibg3.presentation.ui.lista.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.projeto_ibg3.R
import com.example.projeto_ibg3.databinding.ItemPacienteBinding
import com.example.projeto_ibg3.domain.model.Paciente
import com.example.projeto_ibg3.domain.model.SyncStatus

class PacienteAdapter(
    private val callback: PacienteAdapterCallback
) : ListAdapter<Paciente, PacienteAdapter.PacienteViewHolder>(PacienteDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PacienteViewHolder {
        val binding = ItemPacienteBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PacienteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PacienteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class PacienteViewHolder(
        private val binding: ItemPacienteBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(paciente: Paciente) {
            binding.apply {
                // Configurar informações básicas
                tvIniciais.text = paciente.iniciais
                tvNome.text = paciente.nome

                // Verificar se a idade não é null e formatar corretamente
                Log.d("PacienteAdapter", "bind: ${paciente.toString()}")

                if (paciente.idade != null) {
                    tvIdade.text = "${paciente.idade} anos"
                    tvIdade.visibility = View.VISIBLE
                } else {
                    tvIdade.text = "Idade não informada"
                    tvIdade.visibility = View.VISIBLE
                }

                tvTelefone.text = paciente.telefoneFormatado

                // Configurar chips
                chipCpf.text = "CPF: ${paciente.cpfFormatado}"
                chipSus.visibility = if (!paciente.sus.isNullOrEmpty()) View.VISIBLE else View.GONE

                // Configurar indicadores de sincronização
                setupSyncIndicators(paciente)

                // Click no item principal
                root.setOnClickListener {
                    callback.onPacienteClick(paciente)
                }

                // Menu de opções
                btnMenu.setOnClickListener { view ->
                    showPopupMenu(view, paciente)
                }

                // Click nos chips para ações rápidas
                chipCpf.setOnClickListener {
                    callback.onViewDetails(paciente)
                }

                chipSus.setOnClickListener {
                    callback.onViewDetails(paciente)
                }
            }
        }

        private fun setupSyncIndicators(paciente: Paciente) {
            binding.apply {
                when (paciente.syncStatus) {
                    SyncStatus.SYNCED -> {
                        // Paciente sincronizado - sem indicadores especiais
                        syncStatusIndicator.visibility = View.GONE
                        ivSyncIndicator.visibility = View.GONE
                        chipSyncStatus.visibility = View.GONE
                    }

                    SyncStatus.PENDING_UPLOAD -> {
                        // Pendente para upload
                        syncStatusIndicator.visibility = View.VISIBLE
                        syncStatusIndicator.setBackgroundColor(
                            ContextCompat.getColor(root.context, R.color.sync_pending)
                        )

                        ivSyncIndicator.visibility = View.VISIBLE
                        ivSyncIndicator.setImageResource(R.drawable.ic_cloud_upload)
                        ivSyncIndicator.setColorFilter(
                            ContextCompat.getColor(root.context, R.color.sync_pending)
                        )

                        chipSyncStatus.visibility = View.VISIBLE
                        chipSyncStatus.text = root.context.getString(R.string.pendente)
                        chipSyncStatus.setChipBackgroundColorResource(R.color.sync_pending)
                    }

                    SyncStatus.SYNCING -> {
                        // Em processo de sincronização
                        syncStatusIndicator.visibility = View.VISIBLE
                        syncStatusIndicator.setBackgroundColor(
                            ContextCompat.getColor(root.context, R.color.sync_in_progress)
                        )

                        ivSyncIndicator.visibility = View.VISIBLE
                        ivSyncIndicator.setImageResource(R.drawable.ic_sync)
                        ivSyncIndicator.setColorFilter(
                            ContextCompat.getColor(root.context, R.color.sync_in_progress)
                        )

                        chipSyncStatus.visibility = View.VISIBLE
                        chipSyncStatus.text = root.context.getString(R.string.sincronizando)
                        chipSyncStatus.setChipBackgroundColorResource(R.color.sync_in_progress)
                    }

                    SyncStatus.PENDING_DELETE -> {
                        // Pendente para deleção
                        syncStatusIndicator.visibility = View.VISIBLE
                        syncStatusIndicator.setBackgroundColor(
                            ContextCompat.getColor(root.context, R.color.sync_error)
                        )

                        ivSyncIndicator.visibility = View.VISIBLE
                        ivSyncIndicator.setImageResource(R.drawable.ic_delete)
                        ivSyncIndicator.setColorFilter(
                            ContextCompat.getColor(root.context, R.color.sync_error)
                        )

                        chipSyncStatus.visibility = View.VISIBLE
                        chipSyncStatus.text = root.context.getString(R.string.deletar)
                        chipSyncStatus.setChipBackgroundColorResource(R.color.sync_error)

                        // Aplicar efeito visual de item marcado para deleção
                        root.alpha = 0.7f
                    }

                    SyncStatus.UPLOAD_FAILED -> {
                        // Falha no upload
                        syncStatusIndicator.visibility = View.VISIBLE
                        syncStatusIndicator.setBackgroundColor(
                            ContextCompat.getColor(root.context, R.color.sync_error)
                        )

                        ivSyncIndicator.visibility = View.VISIBLE
                        ivSyncIndicator.setImageResource(R.drawable.ic_error)
                        ivSyncIndicator.setColorFilter(
                            ContextCompat.getColor(root.context, R.color.sync_error)
                        )

                        chipSyncStatus.visibility = View.VISIBLE
                        chipSyncStatus.text = root.context.getString(R.string.erro)
                        chipSyncStatus.setChipBackgroundColorResource(R.color.sync_error)
                    }

                    SyncStatus.DELETE_FAILED -> {
                        // Falha na deleção
                        syncStatusIndicator.visibility = View.VISIBLE
                        syncStatusIndicator.setBackgroundColor(
                            ContextCompat.getColor(root.context, R.color.sync_error)
                        )

                        ivSyncIndicator.visibility = View.VISIBLE
                        ivSyncIndicator.setImageResource(R.drawable.ic_error)
                        ivSyncIndicator.setColorFilter(
                            ContextCompat.getColor(root.context, R.color.sync_error)
                        )

                        chipSyncStatus.visibility = View.VISIBLE
                        chipSyncStatus.text = root.context.getString(R.string.erro)
                        chipSyncStatus.setChipBackgroundColorResource(R.color.sync_error)
                    }

                    SyncStatus.CONFLICT -> {
                        // Conflito de sincronização
                        syncStatusIndicator.visibility = View.VISIBLE
                        syncStatusIndicator.setBackgroundColor(
                            ContextCompat.getColor(root.context, R.color.sync_conflict)
                        )

                        ivSyncIndicator.visibility = View.VISIBLE
                        ivSyncIndicator.setImageResource(R.drawable.ic_warning)
                        ivSyncIndicator.setColorFilter(
                            ContextCompat.getColor(root.context, R.color.sync_conflict)
                        )

                        chipSyncStatus.visibility = View.VISIBLE
                        chipSyncStatus.text = root.context.getString(R.string.conflito)
                        chipSyncStatus.setChipBackgroundColorResource(R.color.sync_conflict)
                    }

                    // Adicionar casos que estavam faltando
                    null -> {
                        // Status null - tratar como não sincronizado
                        syncStatusIndicator.visibility = View.GONE
                        ivSyncIndicator.visibility = View.GONE
                        chipSyncStatus.visibility = View.GONE
                    }
                }

                // Resetar alpha se não for para deleção
                if (paciente.syncStatus != SyncStatus.PENDING_DELETE) {
                    root.alpha = 1.0f
                }
            }
        }

        private fun showPopupMenu(view: View, paciente: Paciente) {
            val popup = PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.paciente_options_menu, popup.menu)

            // Mostrar/ocultar opções baseado no status de sincronização
            when (paciente.syncStatus) {
                SyncStatus.CONFLICT -> {
                    // Mostrar opções para resolver conflito
                    popup.menu.findItem(R.id.action_resolve_conflict_local).isVisible = true
                    popup.menu.findItem(R.id.action_resolve_conflict_server).isVisible = true
                    popup.menu.findItem(R.id.action_retry_sync).isVisible = false
                    popup.menu.findItem(R.id.action_restore).isVisible = false
                    popup.menu.findItem(R.id.action_force_sync).isVisible = false
                }

                SyncStatus.UPLOAD_FAILED, SyncStatus.DELETE_FAILED -> {
                    // Mostrar opção para tentar novamente
                    popup.menu.findItem(R.id.action_resolve_conflict_local).isVisible = false
                    popup.menu.findItem(R.id.action_resolve_conflict_server).isVisible = false
                    popup.menu.findItem(R.id.action_retry_sync).isVisible = true
                    popup.menu.findItem(R.id.action_restore).isVisible = false
                    popup.menu.findItem(R.id.action_force_sync).isVisible = false
                }

                SyncStatus.PENDING_DELETE -> {
                    // Mostrar opção para restaurar
                    popup.menu.findItem(R.id.action_resolve_conflict_local).isVisible = false
                    popup.menu.findItem(R.id.action_resolve_conflict_server).isVisible = false
                    popup.menu.findItem(R.id.action_retry_sync).isVisible = false
                    popup.menu.findItem(R.id.action_restore).isVisible = true
                    popup.menu.findItem(R.id.action_force_sync).isVisible = false
                }

                SyncStatus.PENDING_UPLOAD -> {
                    // Mostrar opção para forçar sincronização
                    popup.menu.findItem(R.id.action_resolve_conflict_local).isVisible = false
                    popup.menu.findItem(R.id.action_resolve_conflict_server).isVisible = false
                    popup.menu.findItem(R.id.action_retry_sync).isVisible = false
                    popup.menu.findItem(R.id.action_restore).isVisible = false
                    popup.menu.findItem(R.id.action_force_sync).isVisible = true
                }

                else -> {
                    // Ocultar todas as opções de sincronização
                    popup.menu.findItem(R.id.action_resolve_conflict_local).isVisible = false
                    popup.menu.findItem(R.id.action_resolve_conflict_server).isVisible = false
                    popup.menu.findItem(R.id.action_retry_sync).isVisible = false
                    popup.menu.findItem(R.id.action_restore).isVisible = false
                    popup.menu.findItem(R.id.action_force_sync).isVisible = false
                }
            }

            popup.setOnMenuItemClickListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.action_view_details -> {
                        callback.onViewDetails(paciente)
                        true
                    }
                    R.id.action_edit -> {
                        callback.onEditPaciente(paciente)
                        true
                    }
                    R.id.action_call -> {
                        callback.onCallPaciente(paciente)
                        true
                    }
                    R.id.action_resolve_conflict_local -> {
                        if (callback is ExtendedPacienteAdapterCallback) {
                            callback.onResolveConflictKeepLocal(paciente)
                        }
                        true
                    }
                    R.id.action_resolve_conflict_server -> {
                        if (callback is ExtendedPacienteAdapterCallback) {
                            callback.onResolveConflictKeepServer(paciente)
                        }
                        true
                    }
                    R.id.action_retry_sync -> {
                        if (callback is ExtendedPacienteAdapterCallback) {
                            callback.onRetrySync(paciente)
                        }
                        true
                    }
                    R.id.action_restore -> {
                        if (callback is ExtendedPacienteAdapterCallback) {
                            callback.onRestorePaciente(paciente)
                        }
                        true
                    }
                    R.id.action_force_sync -> {
                        if (callback is ExtendedPacienteAdapterCallback) {
                            callback.onForceSyncPaciente(paciente)
                        }
                        true
                    }
                    else -> false
                }
            }
            popup.show()
        }
    }
}