package com.example.projeto_ibg3.presentation.ui.lista.adapter

import androidx.recyclerview.widget.DiffUtil
import com.example.projeto_ibg3.domain.model.Paciente

// DiffUtil para performance otimizada
class PacienteDiffCallback : DiffUtil.ItemCallback<Paciente>() {
    override fun areItemsTheSame(oldItem: Paciente, newItem: Paciente): Boolean {
        return oldItem.localId == newItem.localId
    }

    override fun areContentsTheSame(oldItem: Paciente, newItem: Paciente): Boolean {
        return oldItem == newItem
    }
}