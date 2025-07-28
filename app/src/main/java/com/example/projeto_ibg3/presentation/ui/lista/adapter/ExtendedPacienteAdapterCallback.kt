package com.example.projeto_ibg3.presentation.ui.lista.adapter

import com.example.projeto_ibg3.domain.model.Paciente

interface ExtendedPacienteAdapterCallback : PacienteAdapterCallback {
    fun onResolveConflictKeepLocal(paciente: Paciente)
    fun onResolveConflictKeepServer(paciente: Paciente)
    fun onRetrySync(paciente: Paciente)
    fun onRestorePaciente(paciente: Paciente)
    fun onForceSyncPaciente(paciente: Paciente)
}