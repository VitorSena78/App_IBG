package com.example.projeto_ibg3.presentation.ui.Lista.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.projeto_ibg3.R
import com.example.projeto_ibg3.databinding.ItemPacienteBinding
import com.example.projeto_ibg3.domain.model.Paciente

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
            // Usar binding ao invés de findViewById
            binding.apply {
                // Configurar informações básicas
                tvInitials.text = paciente.iniciais
                tvName.text = paciente.nome
                tvAge.text = "${paciente.idade} anos"
                tvPhone.text = paciente.telefoneFormatado

                // Configurar chips
                chipCpf.text = "CPF: ${paciente.cpfFormatado}"
                chipSus.visibility = if ((paciente.sus ?: "").isNotEmpty()) View.VISIBLE else View.GONE

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

        private fun showPopupMenu(view: View, paciente: Paciente) {
            val popup = PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.paciente_options_menu, popup.menu)

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
                    else -> false
                }
            }
            popup.show()
        }
    }
}