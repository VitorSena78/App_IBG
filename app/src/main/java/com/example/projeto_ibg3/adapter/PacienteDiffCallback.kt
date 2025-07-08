package com.example.projeto_ibg3.adapter

import androidx.recyclerview.widget.DiffUtil
import com.example.projeto_ibg3.model.Paciente

// DiffUtil para performance otimizada
class PacienteDiffCallback : DiffUtil.ItemCallback<Paciente>() {
    override fun areItemsTheSame(oldItem: Paciente, newItem: Paciente): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Paciente, newItem: Paciente): Boolean {
        return oldItem == newItem
    }
}