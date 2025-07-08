package com.example.projeto_ibg3.model

// Classe para dados completos do paciente com especialidades
data class PacienteCompleto(
    val paciente: Paciente,
    val especialidades: List<Especialidade>
)