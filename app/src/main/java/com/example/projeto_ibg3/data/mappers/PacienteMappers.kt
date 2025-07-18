package com.example.projeto_ibg3.data.mappers

import com.example.projeto_ibg3.data.local.database.entities.PacienteEntity
import com.example.projeto_ibg3.data.remote.dto.PacienteDto
import com.example.projeto_ibg3.domain.model.Paciente
import com.example.projeto_ibg3.domain.model.SyncStatus
import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

// ========== CONVERSÕES PRINCIPAIS ==========

//Converte PacienteEntity para Paciente (model)
fun PacienteEntity.toPaciente(): Paciente {
    return Paciente(
        localId = this.localId,
        serverId = this.serverId,
        nome = this.nome,
        dataNascimento = Date(this.dataNascimento),
        idade = this.idade,
        nomeDaMae = this.nomeDaMae,
        cpf = this.cpf,
        sus = this.sus,
        telefone = this.telefone,
        endereco = this.endereco,
        pressaoArterial = this.pressaoArterial,
        frequenciaCardiaca = this.frequenciaCardiaca,
        frequenciaRespiratoria = this.frequenciaRespiratoria,
        temperatura = this.temperatura,
        glicemia = this.glicemia,
        saturacaoOxigenio = this.saturacaoOxigenio,
        peso = this.peso,
        altura = this.altura,
        imc = this.imc,
        createdAt = if (this.createdAt > 0) Date(this.createdAt) else null,
        updatedAt = if (this.updatedAt > 0) Date(this.updatedAt) else null,
        syncStatus = this.syncStatus,
        version = this.version
    )
}

//Converte Paciente (model) para PacienteEntity
fun Paciente.toEntity(
    deviceId: String = "",
    syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD,
    lastSyncTimestamp: Long = 0,
    isDeleted: Boolean = false,
    syncAttempts: Int = 0,
    syncError: String? = null
): PacienteEntity {
    return PacienteEntity(
        localId = this.localId,
        serverId = this.serverId,
        nome = this.nome,
        dataNascimento = this.dataNascimento.time,
        idade = this.idade ?: this.calcularIdade,
        nomeDaMae = this.nomeDaMae,
        cpf = this.cpf,
        sus = this.sus,
        telefone = this.telefone,
        endereco = this.endereco,
        pressaoArterial = this.pressaoArterial,
        frequenciaCardiaca = this.frequenciaCardiaca,
        frequenciaRespiratoria = this.frequenciaRespiratoria,
        temperatura = this.temperatura,
        glicemia = this.glicemia,
        saturacaoOxigenio = this.saturacaoOxigenio,
        peso = this.peso,
        altura = this.altura,
        imc = this.imc ?: this.calcularImc,
        syncStatus = syncStatus,
        deviceId = deviceId,
        createdAt = this.createdAt?.time ?: System.currentTimeMillis(),
        updatedAt = this.updatedAt?.time ?: System.currentTimeMillis(),
        lastSyncTimestamp = lastSyncTimestamp,
        isDeleted = isDeleted,
        version = this.version,
        syncAttempts = syncAttempts,
        syncError = syncError
    )
}

//Converte PacienteDto para Paciente (model)
fun PacienteDto.toPaciente(): Paciente {
    return Paciente(
        localId = this.localId ?: UUID.randomUUID().toString(),
        serverId = this.serverId,
        nome = this.nome?.takeIf { it.isNotBlank() } ?: "Nome não informado",
        dataNascimento = parseIsoDate(this.dataNascimento),
        idade = this.idade,
        nomeDaMae = this.nomeDaMae?.takeIf { it.isNotBlank() } ?: "Não informado",
        cpf = this.cpf?.takeIf { it.isNotBlank() } ?: "",
        sus = this.sus?.takeIf { it.isNotBlank() } ?: "",
        telefone = this.telefone?.takeIf { it.isNotBlank() } ?: "",
        endereco = this.endereco?.takeIf { it.isNotBlank() } ?: "",
        pressaoArterial = this.paXMmhg,
        frequenciaCardiaca = this.fcBpm,
        frequenciaRespiratoria = this.frIbpm,
        temperatura = this.temperaturaC,
        glicemia = this.hgtMgld,
        saturacaoOxigenio = this.spo2,
        peso = this.peso,
        altura = this.altura,
        imc = this.imc,
        createdAt = parseIsoDate(this.createdAt),
        updatedAt = parseIsoDate(this.updatedAt),
        syncStatus = SyncStatus.SYNCED,
        version = this.version
    )
}

//Converte Paciente (model) para PacienteDto
fun Paciente.toDto(): PacienteDto {
    return PacienteDto(
        serverId = this.serverId,
        localId = this.localId,
        nome = this.nome,
        dataNascimento = formatDateToIso(this.dataNascimento),
        idade = this.idade,
        nomeDaMae = this.nomeDaMae,
        cpf = this.cpf,
        sus = this.sus,
        telefone = this.telefone,
        endereco = this.endereco,
        paXMmhg = this.pressaoArterial,
        fcBpm = this.frequenciaCardiaca,
        frIbpm = this.frequenciaRespiratoria,
        temperaturaC = this.temperatura,
        hgtMgld = this.glicemia,
        spo2 = this.saturacaoOxigenio,
        peso = this.peso,
        altura = this.altura,
        imc = this.imc,
        createdAt = this.createdAt?.let { formatDateToIso(it) },
        updatedAt = this.updatedAt?.let { formatDateToIso(it) },
        deviceId = "",
        version = this.version,
        lastSyncTimestamp = 0,
        isDeleted = false
    )
}

//Converte PacienteEntity para PacienteDto
fun PacienteEntity.toDto(): PacienteDto {
    return PacienteDto(
        serverId = this.serverId,
        localId = this.localId,
        nome = this.nome,
        dataNascimento = formatTimestampToIso(this.dataNascimento),
        idade = this.idade,
        nomeDaMae = this.nomeDaMae,
        cpf = this.cpf,
        sus = this.sus,
        telefone = this.telefone,
        endereco = this.endereco,
        paXMmhg = this.pressaoArterial,
        fcBpm = this.frequenciaCardiaca,
        frIbpm = this.frequenciaRespiratoria,
        temperaturaC = this.temperatura,
        hgtMgld = this.glicemia,
        spo2 = this.saturacaoOxigenio,
        peso = this.peso,
        altura = this.altura,
        imc = this.imc,
        createdAt = formatTimestampToIso(this.createdAt),
        updatedAt = formatTimestampToIso(this.updatedAt),
        deviceId = this.deviceId,
        version = this.version,
        lastSyncTimestamp = this.lastSyncTimestamp,
        isDeleted = this.isDeleted
    )
}

//Converte PacienteDto para PacienteEntity
fun PacienteDto.toEntity(
    localId: String = this.localId,
    syncStatus: SyncStatus = SyncStatus.SYNCED,
    deviceId: String = this.deviceId ?: ""
): PacienteEntity {
    return PacienteEntity(
        localId = localId,
        serverId = this.serverId,
        nome = this.nome ?: "",
        dataNascimento = parseIsoToTimestamp(this.dataNascimento) ?: System.currentTimeMillis(),
        idade = this.idade,
        nomeDaMae = this.nomeDaMae ?: "",
        cpf = this.cpf ?: "",
        sus = this.sus ?: "",
        telefone = this.telefone ?: "",
        endereco = this.endereco ?: "",
        pressaoArterial = this.paXMmhg,
        frequenciaCardiaca = this.fcBpm,
        frequenciaRespiratoria = this.frIbpm,
        temperatura = this.temperaturaC,
        glicemia = this.hgtMgld,
        saturacaoOxigenio = this.spo2,
        peso = this.peso,
        altura = this.altura,
        imc = this.imc,
        syncStatus = syncStatus,
        deviceId = deviceId,
        createdAt = parseIsoToTimestamp(this.createdAt) ?: System.currentTimeMillis(),
        updatedAt = parseIsoToTimestamp(this.updatedAt) ?: System.currentTimeMillis(),
        lastSyncTimestamp = this.lastSyncTimestamp ?: 0L,
        version = this.version,
        syncAttempts = 0,
        syncError = null,
        isDeleted = this.isDeleted
    )
}

// ========== CONVERSÕES DE ATUALIZAÇÃO ==========

//Atualiza PacienteEntity com dados do PacienteDto (servidor)
fun PacienteEntity.updateFrom(dto: PacienteDto): PacienteEntity {
    return this.copy(
        nome = dto.nome ?: this.nome,
        dataNascimento = parseIsoToTimestamp(dto.dataNascimento) ?: this.dataNascimento,
        idade = dto.idade ?: this.idade,
        nomeDaMae = dto.nomeDaMae ?: this.nomeDaMae,
        cpf = dto.cpf ?: this.cpf,
        sus = dto.sus ?: this.sus,
        telefone = dto.telefone ?: this.telefone,
        endereco = dto.endereco ?: this.endereco,
        pressaoArterial = dto.paXMmhg ?: this.pressaoArterial,
        frequenciaCardiaca = dto.fcBpm ?: this.frequenciaCardiaca,
        frequenciaRespiratoria = dto.frIbpm ?: this.frequenciaRespiratoria,
        temperatura = dto.temperaturaC ?: this.temperatura,
        glicemia = dto.hgtMgld ?: this.glicemia,
        saturacaoOxigenio = dto.spo2 ?: this.saturacaoOxigenio,
        peso = dto.peso ?: this.peso,
        altura = dto.altura ?: this.altura,
        imc = dto.imc ?: this.imc,
        updatedAt = parseIsoToTimestamp(dto.updatedAt) ?: System.currentTimeMillis(),
        version = dto.version ?: this.version,
        lastSyncTimestamp = dto.lastSyncTimestamp ?: this.lastSyncTimestamp,
        isDeleted = dto.isDeleted ?: this.isDeleted
    )
}

// ========== FUNÇÕES UTILITÁRIAS PARA CONVERSÃO DE DATAS ==========

//Converte Date para formato ISO string
private fun formatDateToIso(date: Date): String {
    return try {
        val instant = Instant.ofEpochMilli(date.time)
        instant.toString()
    } catch (e: Exception) {
        Instant.now().toString()
    }
}

//Converte timestamp (Long) para formato ISO string
private fun formatTimestampToIso(timestamp: Long): String {
    return try {
        val instant = Instant.ofEpochMilli(timestamp)
        instant.toString()
    } catch (e: Exception) {
        Instant.now().toString()
    }
}

//Converte string ISO para Date
private fun parseIsoDate(isoString: String?): Date {
    return try {
        if (isoString.isNullOrBlank()) {
            Date()
        } else {
            // Tenta primeiro formato completo ISO
            val instant = Instant.parse(isoString)
            Date.from(instant)
        }
    } catch (e: Exception) {
        // Tenta formato alternativo (apenas data)
        try {
            if (isoString != null && isoString.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                sdf.parse(isoString) ?: Date()
            } else {
                Date()
            }
        } catch (e2: Exception) {
            Date()
        }
    }
}

//Converte string ISO para timestamp (Long)
private fun parseIsoToTimestamp(isoString: String?): Long {
    return try {
        parseIsoDate(isoString).time
    } catch (e: Exception) {
        System.currentTimeMillis() // Retorna timestamp atual como padrão
    }
}

private fun parseIsoToTimestamp(isoString: String?, defaultValue: Long): Long {
    return try {
        parseIsoDate(isoString).time
    } catch (e: Exception) {
        defaultValue
    }
}

// ========== FUNÇÕES PARA LISTAS (BATCH OPERATIONS) - CORRIGIDAS ==========

// Converte lista de PacienteEntity para lista de Paciente
@JvmName("entityListToPacientes")
fun List<PacienteEntity>.toPacientes(): List<Paciente> = map { it.toPaciente() }

// Converte lista de PacienteDto para lista de Paciente
@JvmName("dtoListToPacientes")
fun List<PacienteDto>.toPacientes(): List<Paciente> = map { it.toPaciente() }

// Converte lista de Paciente para lista de PacienteEntity
@JvmName("pacienteListToEntities")
fun List<Paciente>.toEntities(deviceId: String = ""): List<PacienteEntity> =
    map { it.toEntity(deviceId = deviceId) }

// Converte lista de PacienteDto para lista de PacienteEntity
@JvmName("dtoListToEntities")
fun List<PacienteDto>.toEntities(deviceId: String = ""): List<PacienteEntity> =
    map { it.toEntity(deviceId = deviceId) }

// Converte lista de PacienteEntity para lista de PacienteDto
@JvmName("entityListToDtos")
fun List<PacienteEntity>.toDtos(): List<PacienteDto> = map { it.toDto() }

// Converte lista de Paciente para lista de PacienteDto
@JvmName("pacienteListToDtos")
fun List<Paciente>.toDtos(): List<PacienteDto> = map { it.toDto() }