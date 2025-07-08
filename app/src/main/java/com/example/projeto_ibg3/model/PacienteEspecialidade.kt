package com.example.projeto_ibg3.model

import java.util.Date

data class PacienteEspecialidade(
    val pacienteId: Long,
    val especialidadeId: Long,
    val dataAtendimento: Date? = null
)

