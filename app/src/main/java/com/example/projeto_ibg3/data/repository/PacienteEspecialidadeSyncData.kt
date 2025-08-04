package com.example.projeto_ibg3.data.repository

data class PacienteEspecialidadeSyncData(
    val localId: String,
    val serverId: Long?,
    val pacienteId: Long,
    val especialidadeId: Long,
    val dataAtendimento: Long?,
    val lastModified: Long,
    val isDeleted: Boolean
)
