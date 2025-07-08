package com.example.projeto_ibg3.adapter

import com.example.projeto_ibg3.model.Paciente

// Interface para callbacks do adapter
interface PacienteAdapterCallback {
    fun onPacienteClick(paciente: Paciente)
    fun onEditPaciente(paciente: Paciente)
    fun onDeletePaciente(paciente: Paciente)
    fun onCallPaciente(paciente: Paciente)
    fun onViewDetails(paciente: Paciente)
}