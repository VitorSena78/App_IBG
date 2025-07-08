package com.example.projeto_ibg3.data.entity

import com.example.projeto_ibg3.model.SyncStatus
import com.example.projeto_ibg3.model.PacienteEspecialidade
import java.util.Date

fun PacienteEspecialidadeEntity.toPacienteEspecialidade(): PacienteEspecialidade {
    return PacienteEspecialidade(
        pacienteId = this.pacienteId,
        especialidadeId = this.especialidadeId,
        dataAtendimento = Date(this.dataAtendimento)
    )
}

fun PacienteEspecialidade.toEntity(): PacienteEspecialidadeEntity {
    return PacienteEspecialidadeEntity(
        pacienteId = this.pacienteId,
        especialidadeId = this.especialidadeId,
        dataAtendimento = this.dataAtendimento?.time?: 0L,
        syncStatus = SyncStatus.PENDING_UPLOAD,
        lastModified = System.currentTimeMillis(),
        isDeleted = false
    )
}