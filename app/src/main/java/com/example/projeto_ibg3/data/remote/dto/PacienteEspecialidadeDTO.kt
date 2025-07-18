package com.example.projeto_ibg3.data.remote.dto

data class PacienteEspecialidadeDTO(
    val pacienteId: Long,
    val especialidadeId: Long,
    val dataAtendimento: Long? = null,
    val serverId: Long? = null, // ID no servidor (se já foi sincronizado)
    val localId: String? = null, // ID local único para tracking
    val lastModified: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)
