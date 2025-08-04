package com.example.projeto_ibg3.data.mappers

import com.example.projeto_ibg3.data.local.database.entities.EspecialidadeEntity
import com.example.projeto_ibg3.data.local.database.entities.PacienteEspecialidadeEntity
import com.example.projeto_ibg3.domain.model.Especialidade
import com.example.projeto_ibg3.domain.model.SyncStatus
import com.example.projeto_ibg3.domain.model.PacienteEspecialidade
import java.util.Date

fun PacienteEspecialidadeEntity.toPacienteEspecialidade(): PacienteEspecialidade {
    return PacienteEspecialidade(
        pacienteLocalId = this.pacienteLocalId,
        especialidadeLocalId = this.especialidadeLocalId,
        dataAtendimento = this.dataAtendimento?.let { Date(it) },
        pacienteServerId = this.pacienteServerId,
        especialidadeServerId = this.especialidadeServerId,
        syncStatus = this.syncStatus
    )
}

fun PacienteEspecialidade.toEntity(): PacienteEspecialidadeEntity {
    return PacienteEspecialidadeEntity(
        pacienteLocalId = this.pacienteLocalId,
        especialidadeLocalId = this.especialidadeLocalId,
        dataAtendimento = this.dataAtendimento?.time?: 0L,
        syncStatus = SyncStatus.PENDING_UPLOAD,
        updatedAt = System.currentTimeMillis(),
        isDeleted = false
    )
}

// Função para converter PacienteEspecialidadeEntity para PacienteEspecialidade (model)
fun PacienteEspecialidadeEntity.toModel(): PacienteEspecialidade {
    return PacienteEspecialidade(
        pacienteLocalId = this.pacienteLocalId,
        especialidadeLocalId = this.especialidadeLocalId,
        dataAtendimento = this.dataAtendimento?.let { Date(it) }, // Converte Long para Date
        pacienteServerId = this.pacienteServerId,
        especialidadeServerId = this.especialidadeServerId,
        syncStatus = this.syncStatus
    )
}

// Função para converter lista de models para lista de entities
fun List<PacienteEspecialidade>.toEntityList(deviceId: String = ""): List<PacienteEspecialidadeEntity> {
    return this.map { it.toEntity(deviceId) }
}

// Função para converter lista de entities para lista de models
fun List<PacienteEspecialidadeEntity>.toModelList(): List<PacienteEspecialidade> {
    return this.map { it.toModel() }
}

// Função para converter PacienteEspecialidade (model) para PacienteEspecialidadeEntity
fun PacienteEspecialidade.toEntity(deviceId: String = "default_device"): PacienteEspecialidadeEntity {
    return PacienteEspecialidadeEntity(
        pacienteLocalId = this.pacienteLocalId,
        especialidadeLocalId = this.especialidadeLocalId,
        pacienteServerId = this.pacienteServerId,
        especialidadeServerId = this.especialidadeServerId,
        syncStatus = this.syncStatus,
        deviceId = deviceId,
        dataAtendimento = this.dataAtendimento?.time, // Converte Date para Long (timestamp)
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
        lastSyncTimestamp = 0L,
        version = 1,
        isDeleted = false,
        syncAttempts = 0,
        lastSyncAttempt = 0L,
        syncError = null
    )
}

// Conversão de Especialidade para EspecialidadeEntity
fun Especialidade.toEspecialidadeEntity(deviceId: String): EspecialidadeEntity {
    return EspecialidadeEntity(
        localId = this.localId,
        serverId = this.serverId,
        nome = this.nome,
        deviceId = deviceId
    )
}

// Conversão de PacienteEspecialidade para PacienteEspecialidadeEntity
fun PacienteEspecialidade.toPacienteEspecialidadeEntity(deviceId: String): PacienteEspecialidadeEntity {
    return PacienteEspecialidadeEntity(
        pacienteLocalId = this.pacienteLocalId,
        especialidadeLocalId = this.especialidadeLocalId,
        dataAtendimento = this.dataAtendimento?.time,
        pacienteServerId = this.pacienteServerId,
        especialidadeServerId = this.especialidadeServerId,
        syncStatus = this.syncStatus,
        deviceId = deviceId
    )
}