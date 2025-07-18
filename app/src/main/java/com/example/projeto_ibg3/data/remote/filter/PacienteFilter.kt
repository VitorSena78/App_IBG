package com.example.projeto_ibg3.data.remote.filter

import com.example.projeto_ibg3.domain.model.SyncStatus

//Filtros para busca de pacientes
data class PacienteFilter(
    val nome: String? = null,
    val cpf: String? = null,
    val sus: String? = null,
    val nomeDaMae: String? = null,
    val telefone: String? = null,
    val syncStatus: SyncStatus? = null,
    val isDeleted: Boolean? = null,
    val createdAfter: Long? = null,
    val createdBefore: Long? = null,
    val updatedAfter: Long? = null,
    val updatedBefore: Long? = null
)
