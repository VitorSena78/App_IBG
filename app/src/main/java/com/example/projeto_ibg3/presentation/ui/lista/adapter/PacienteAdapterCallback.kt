package com.example.projeto_ibg3.presentation.ui.lista.adapter

import com.example.projeto_ibg3.domain.model.Paciente

// Interface para callbacks do adapter
interface PacienteAdapterCallback {
    fun onPacienteClick(paciente: Paciente)
    fun onEditPaciente(paciente: Paciente)
    fun onCallPaciente(paciente: Paciente)
    fun onViewDetails(paciente: Paciente)
}