package com.example.projeto_ibg3.data.mappers

import android.util.Log
import com.example.projeto_ibg3.data.local.database.entities.PacienteEntity
import com.example.projeto_ibg3.data.local.database.entities.PacienteEspecialidadeEntity
import com.example.projeto_ibg3.data.local.database.entities.EspecialidadeEntity
import com.example.projeto_ibg3.data.remote.dto.PacienteDto
import com.example.projeto_ibg3.data.remote.dto.PacienteEspecialidadeDTO
import com.example.projeto_ibg3.domain.model.Paciente
import com.example.projeto_ibg3.domain.model.PacienteEspecialidade
import com.example.projeto_ibg3.domain.model.Especialidade
import com.example.projeto_ibg3.domain.model.SyncStatus
import java.text.SimpleDateFormat
import java.util.*

// ==================== PACIENTE MAPPERS ====================

/**
 * Converte PacienteEntity para PacienteDto
 */
// Formato padrão usado
val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

// Converte Long (timestamp) para String no formato yyyy-MM-dd
fun Long.toDateString(): String {
    return dateFormat.format(Date(this))
}

// Converte String no formato yyyy-MM-dd para Long (timestamp)
// Retorna System.currentTimeMillis() se o formato estiver incorreto
fun String.toDateLong(): Long {
    return try {
        val date = dateFormat.parse(this)
        date.time
    } catch (e: Exception) {
        Log.e("SincMapper", "Erro ao converter data", e)
        System.currentTimeMillis()
    }
}

fun PacienteEntity.toPacienteDto(): PacienteDto {
    return PacienteDto(
        serverId = this.serverId,
        localId = this.localId,
        nome = this.nome,
        nomeDaMae = this.nomeDaMae,
        dataNascimento = this.dataNascimento!!.toDateString(),
        cpf = this.cpf,
        sus = this.sus,
        telefone = this.telefone,
        endereco = this.endereco,
        updatedAt = this.updatedAt.toDateString(),
        createdAt = this.createdAt.toDateString(),
        isDeleted = this.isDeleted
    )
}

/**
 * Converte PacienteDto para PacienteEntity
 */
fun PacienteDto.toPacienteEntity(
    deviceId: String = "default_device",
    syncStatus: SyncStatus = SyncStatus.SYNCED
): PacienteEntity {
    val dataNascimentoTimestamp = this.dataNascimento?.let {
        try {
            dateFormat.parse(it)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    } ?: 0L

    return PacienteEntity(
        localId = this.localId ?: UUID.randomUUID().toString(),
        serverId = this.serverId,
        nome = this.nome,
        nomeDaMae = this.nomeDaMae,
        dataNascimento = dataNascimentoTimestamp,
        cpf = this.cpf,
        sus = this.sus,
        telefone = this.telefone,
        endereco = this.endereco,
        syncStatus = syncStatus,
        deviceId = deviceId,
        createdAt = this.createdAt.toLong(),
        updatedAt = this.updatedAt.toLong(),
        lastSyncTimestamp = System.currentTimeMillis(),
        isDeleted = this.isDeleted
    )
}


/**
 * Converte domain model Paciente para PacienteEntity
 */
fun Paciente.toEntity(
    deviceId: String = "default_device"
): PacienteEntity {
    return PacienteEntity(
        localId = this.localId,
        serverId = this.serverId,
        nome = this.nome,
        nomeDaMae = this.nomeDaMae,
        dataNascimento = this.dataNascimento.time,
        cpf = this.cpf,
        sus = this.sus,
        telefone = this.telefone,
        endereco = this.endereco,
        syncStatus = this.syncStatus,
        deviceId = deviceId,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}

/**
 * Converte lista de PacienteEntity para lista de Paciente
 */
fun List<PacienteEntity>.toPacienteList(): List<Paciente> {
    return this.map { it.toPaciente() }
}

/**
 * Converte lista de PacienteDto para lista de PacienteEntity
 */
fun List<PacienteDto>.toPacienteEntityList(
    deviceId: String = "default_device",
    syncStatus: SyncStatus = SyncStatus.SYNCED
): List<PacienteEntity> {
    return this.map { it.toPacienteEntity(deviceId, syncStatus) }
}

// ==================== ESPECIALIDADE MAPPERS ====================


/**
 * Converte domain model Especialidade para EspecialidadeEntity
 */
fun Especialidade.toEntity(
    deviceId: String = "default_device"
): EspecialidadeEntity {
    return EspecialidadeEntity(
        localId = this.localId,
        serverId = this.serverId,
        nome = this.nome,
        deviceId = deviceId,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}

/**
 * Converte lista de EspecialidadeEntity para lista de Especialidade
 */
fun List<EspecialidadeEntity>.toEspecialidadeList(): List<Especialidade> {
    return this.map { it.toEspecialidade() }
}


// ==================== PACIENTE ESPECIALIDADE MAPPERS ====================


/**
 * Converte PacienteEspecialidadeEntity para PacienteEspecialidadeDTO
 */
fun PacienteEspecialidadeEntity.toDto(): PacienteEspecialidadeDTO? {
    // Só pode converter se tiver os serverIds
    return if (this.pacienteServerId != null && this.especialidadeServerId != null) {
        PacienteEspecialidadeDTO(
            pacienteId = this.pacienteServerId!!,
            especialidadeId = this.especialidadeServerId!!,
            dataAtendimento = this.dataAtendimento,
            serverId = null, // Relacionamentos não têm serverId próprio
            localId = this.relationId,
            lastModified = this.updatedAt,
            isDeleted = this.isDeleted
        )
    } else {
        null
    }
}

/**
 * Converte PacienteEspecialidadeDTO para PacienteEspecialidadeEntity
 * Requer os localIds dos paciente e especialidade correspondentes
 */
fun PacienteEspecialidadeDTO.toEntity(
    pacienteLocalId: String,
    especialidadeLocalId: String,
    deviceId: String = "default_device",
    syncStatus: SyncStatus = SyncStatus.SYNCED
): PacienteEspecialidadeEntity {
    return PacienteEspecialidadeEntity(
        pacienteLocalId = pacienteLocalId,
        especialidadeLocalId = especialidadeLocalId,
        dataAtendimento = this.dataAtendimento,
        pacienteServerId = this.pacienteId,
        especialidadeServerId = this.especialidadeId,
        syncStatus = syncStatus,
        deviceId = deviceId,
        createdAt = this.lastModified,
        updatedAt = this.lastModified,
        lastSyncTimestamp = System.currentTimeMillis(),
        isDeleted = this.isDeleted
    )
}

/**
 * Converte lista de PacienteEspecialidadeEntity para lista de PacienteEspecialidade
 */
fun List<PacienteEspecialidadeEntity>.toPacienteEspecialidadeList(): List<PacienteEspecialidade> {
    return this.map { it.toPacienteEspecialidade() }
}

// ==================== MAPPERS AUXILIARES ====================

/**
 * Cria um PacienteEntity com valores padrão para novos registros
 */
fun createNewPacienteEntity(
    nome: String,
    nomeDaMae: String? = null,
    dataNascimento: String,
    cpf: String,
    sus: String? = null,
    telefone: String? = null,
    endereco: String? = null,
    deviceId: String = "default_device"
): PacienteEntity {
    return PacienteEntity(
        localId = UUID.randomUUID().toString(),
        serverId = null,
        nome = nome,
        nomeDaMae = nomeDaMae,
        dataNascimento = dataNascimento.toDateLong(),
        cpf = cpf,
        sus = sus,
        telefone = telefone,
        endereco = endereco,
        syncStatus = SyncStatus.PENDING_UPLOAD,
        deviceId = deviceId,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}

/**
 * Cria um PacienteEspecialidadeEntity com valores padrão
 */
fun createNewPacienteEspecialidadeEntity(
    pacienteLocalId: String,
    especialidadeLocalId: String,
    dataAtendimento: Date? = null,
    deviceId: String = "default_device"
): PacienteEspecialidadeEntity {
    return PacienteEspecialidadeEntity(
        pacienteLocalId = pacienteLocalId,
        especialidadeLocalId = especialidadeLocalId,
        dataAtendimento = dataAtendimento?.time,
        syncStatus = SyncStatus.PENDING_UPLOAD,
        deviceId = deviceId,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}

/**
 * Atualiza um PacienteEntity existente com novos dados
 */
fun PacienteEntity.updateWith(
    nome: String? = null,
    nomeDaMae: String? = null,
    cpf: String? = null,
    sus: String? = null,
    telefone: String? = null,
    endereco: String? = null,
): PacienteEntity {
    return this.copy(
        nome = nome ?: this.nome,
        nomeDaMae = nomeDaMae ?: this.nomeDaMae,
        dataNascimento = this.dataNascimento,
        cpf = cpf ?: this.cpf,
        sus = sus ?: this.sus,
        telefone = telefone ?: this.telefone,
        endereco = endereco ?: this.endereco,
        updatedAt = System.currentTimeMillis(),
        syncStatus = if (this.syncStatus == SyncStatus.SYNCED) SyncStatus.PENDING_UPLOAD else this.syncStatus
    )
}

/**
 * Marca uma entidade como deletada (soft delete)
 */
fun PacienteEntity.markAsDeleted(): PacienteEntity {
    return this.copy(
        isDeleted = true,
        syncStatus = SyncStatus.PENDING_DELETE,
        updatedAt = System.currentTimeMillis()
    )
}

/**
 * Marca um relacionamento como deletado (soft delete)
 */
fun PacienteEspecialidadeEntity.markAsDeleted(): PacienteEspecialidadeEntity {
    return this.copy(
        isDeleted = true,
        syncStatus = SyncStatus.PENDING_DELETE,
        updatedAt = System.currentTimeMillis()
    )
}

/**
 * Restaura uma entidade deletada
 */
fun PacienteEntity.restore(): PacienteEntity {
    return this.copy(
        isDeleted = false,
        syncStatus = SyncStatus.PENDING_UPLOAD,
        updatedAt = System.currentTimeMillis()
    )
}

/**
 * Marca uma entidade como sincronizada
 */
fun PacienteEntity.markAsSynced(serverId: Long? = null): PacienteEntity {
    return this.copy(
        serverId = serverId ?: this.serverId,
        syncStatus = SyncStatus.SYNCED,
        lastSyncTimestamp = System.currentTimeMillis(),
        syncAttempts = 0,
        syncError = null
    )
}

/**
 * Marca um relacionamento como sincronizado
 */
fun PacienteEspecialidadeEntity.markAsSynced(): PacienteEspecialidadeEntity {
    return this.copy(
        syncStatus = SyncStatus.SYNCED,
        lastSyncTimestamp = System.currentTimeMillis(),
        syncAttempts = 0,
        syncError = null
    )
}

/**
 * Incrementa tentativas de sincronização
 */
fun PacienteEntity.incrementSyncAttempts(error: String? = null): PacienteEntity {
    return this.copy(
        syncAttempts = this.syncAttempts + 1,
        lastSyncAttempt = System.currentTimeMillis(),
        syncError = error,
        syncStatus = when {
            this.syncAttempts >= 2 -> SyncStatus.UPLOAD_FAILED
            else -> this.syncStatus
        }
    )
}

/**
 * Incrementa tentativas de sincronização para relacionamentos
 */
fun PacienteEspecialidadeEntity.incrementSyncAttempts(error: String? = null): PacienteEspecialidadeEntity {
    return this.copy(
        syncAttempts = this.syncAttempts + 1,
        lastSyncAttempt = System.currentTimeMillis(),
        syncError = error,
        syncStatus = when {
            this.syncAttempts >= 2 -> SyncStatus.UPLOAD_FAILED
            else -> this.syncStatus
        }
    )
}