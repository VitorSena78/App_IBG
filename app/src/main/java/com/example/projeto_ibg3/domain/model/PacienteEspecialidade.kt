package com.example.projeto_ibg3.domain.model

import java.util.Date

data class PacienteEspecialidade(
    val pacienteLocalId: String,
    val especialidadeLocalId: String,
    val dataAtendimento: Date? = null,  //mudar para Long futuramente
    val pacienteServerId: Long? = null,
    val especialidadeServerId: Long? = null,
    val syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD
){
    // ID único para esta relação
    val relationId: String
        get() = "${pacienteLocalId}_${especialidadeLocalId}"
}

