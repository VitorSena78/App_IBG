package com.example.projeto_ibg3.data.entity

import com.example.projeto_ibg3.model.SyncStatus
import com.example.projeto_ibg3.model.Especialidade

fun EspecialidadeEntity.toEspecialidade(): Especialidade {
    return Especialidade(
        id = this.id,
        serverId = this.serverId,
        nome = this.nome
    )
}

fun Especialidade.toEntity(): EspecialidadeEntity {
    return EspecialidadeEntity(
        id = 0,
        serverId = this.serverId,
        nome = this.nome,
        syncStatus = SyncStatus.PENDING_UPLOAD,
        lastModified = System.currentTimeMillis(),
        isDeleted = false
    )
}