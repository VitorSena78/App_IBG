package com.example.projeto_ibg3.data.mappers

import android.util.Log
import com.example.projeto_ibg3.data.local.database.entities.PacienteEntity
import com.example.projeto_ibg3.data.local.database.entities.PacienteEspecialidadeEntity
import com.example.projeto_ibg3.data.local.database.entities.EspecialidadeEntity
import com.example.projeto_ibg3.data.mappers.toDomainModel
import com.example.projeto_ibg3.data.remote.dto.PacienteDto
import com.example.projeto_ibg3.data.remote.dto.PacienteEspecialidadeDTO
import com.example.projeto_ibg3.domain.model.Paciente
import com.example.projeto_ibg3.domain.model.PacienteEspecialidade
import com.example.projeto_ibg3.domain.model.Especialidade
import com.example.projeto_ibg3.domain.model.SyncStatus
import java.text.SimpleDateFormat
import java.util.*

val isoDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

// Formato de data ISO para conversão
private val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

// ==================== EXTENSÕES PARA CONVERSÃO DE DATAS ====================

fun String?.toDateLong(): Long? {
    if (this.isNullOrBlank()) return null
    return try {
        // Primeiro tenta formato de data simples (yyyy-MM-dd)
        if (this.length == 10) {
            simpleDateFormat.parse(this)?.time
        } else {
            // Tenta formato ISO completo
            isoDateFormat.parse(this)?.time
        }
    } catch (e: Exception) {
        null
    }
}

fun Long.toIsoString(): String {
    return isoDateFormat.format(Date(this))
}

// ==================== MAPPERS ENTITY -> DTO ====================

fun PacienteEspecialidadeEntity.toPacienteEspecialidadeDto(): PacienteEspecialidadeDTO {
    return PacienteEspecialidadeDTO(
        pacienteServerId = this.pacienteServerId,
        especialidadeServerId = this.especialidadeServerId,
        pacienteLocalId = this.pacienteLocalId,
        especialidadeLocalId = this.especialidadeLocalId,
        dataAtendimento = this.dataAtendimento?.toDateString(),
        createdAt = this.createdAt.toIsoString(),
        updatedAt = this.updatedAt.toIsoString(),
        lastSyncTimestamp = this.lastSyncTimestamp,
        action = when (this.syncStatus) {
            SyncStatus.PENDING_DELETE -> "DELETE"
            else -> "PENDING"
        }
    )
}

// ==================== MAPPERS DTO -> ENTITY ====================

// Versão alternativa que cria com os IDs do DTO (caso você queira manter)
fun PacienteEspecialidadeDTO.toPacienteEspecialidadeEntity(
    deviceId: String = "",
    syncStatus: SyncStatus = SyncStatus.SYNCED
): PacienteEspecialidadeEntity {
    return PacienteEspecialidadeEntity(
        pacienteLocalId = this.pacienteLocalId,
        especialidadeLocalId = this.especialidadeLocalId,
        pacienteServerId = this.pacienteServerId,
        especialidadeServerId = this.especialidadeServerId,
        dataAtendimento = this.dataAtendimento.toDateLong(),
        syncStatus = syncStatus,
        deviceId = deviceId,
        createdAt = this.createdAt.toIsoDateLong(),
        updatedAt = this.updatedAt.toIsoDateLong(),
        lastSyncTimestamp = this.lastSyncTimestamp ?: System.currentTimeMillis(),
        isDeleted = this.isDeleted,
        version = 1,
        conflictData = null,
        syncAttempts = 0,
        lastSyncAttempt = 0,
        syncError = null
    )
}

// ==================== MAPPERS PARA LISTAS ====================

fun List<PacienteEspecialidadeEntity>.toDtoList(): List<PacienteEspecialidadeDTO> {
    return this.map { it.toPacienteEspecialidadeDto() }
}

fun List<PacienteEspecialidadeDTO>.toEntityList(
    deviceId: String = "",
    syncStatus: SyncStatus = SyncStatus.SYNCED
): List<PacienteEspecialidadeEntity> {
    return this.map { it.toPacienteEspecialidadeEntity(deviceId, syncStatus) }
}

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
            Log.e("SincMapper", "Erro ao converter dataNascimento: $it", e)
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
        createdAt = this.createdAt.toLongSafe(),
        updatedAt = this.updatedAt.toLongSafe(),
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
    return this.map { it.toDomainModel() }
}


// ==================== PACIENTE ESPECIALIDADE MAPPERS ====================


//Converte PacienteEspecialidadeEntity para PacienteEspecialidadeDTO
fun PacienteEspecialidadeEntity.toDto(): PacienteEspecialidadeDTO? {
    // Só pode converter se tiver os serverIds
    return if (this.pacienteServerId != null && this.especialidadeServerId != null) {
        PacienteEspecialidadeDTO(
            pacienteServerId = this.pacienteServerId!!,
            especialidadeServerId = this.especialidadeServerId!!,
            pacienteLocalId = this.pacienteLocalId,
            especialidadeLocalId = this.especialidadeLocalId,
            dataAtendimento = this.dataAtendimento?.let { dateFormat.format(Date(it)) }, // Converter timestamp para string de data
            createdAt = dateFormat.format(Date(this.createdAt)), // Converter timestamp para string
            updatedAt = dateFormat.format(Date(this.updatedAt)), // Converter timestamp para string
            lastSyncTimestamp = this.lastSyncTimestamp,
            action = if (this.isDeleted) "DELETE" else null
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
        dataAtendimento = this.dataAtendimento?.toDateLong(), // Converter string de data para timestamp
        pacienteServerId = this.pacienteServerId,
        especialidadeServerId = this.especialidadeServerId,
        syncStatus = syncStatus,
        deviceId = deviceId,
        createdAt = this.createdAt.toIsoDateLong(), // Converter string ISO para timestamp
        updatedAt = this.updatedAt.toIsoDateLong(), // Converter string ISO para timestamp
        lastSyncTimestamp = this.lastSyncTimestamp ?: System.currentTimeMillis(),
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

//Cria um PacienteEntity com valores padrão para novos registros
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

// Função para converter data ISO para timestamp Long
fun String?.toIsoDateLong(): Long {
    return try {
        if (this.isNullOrBlank()) {
            System.currentTimeMillis()
        } else {
            // Tenta primeiro o formato ISO completo
            val date = isoDateFormat.parse(this)
            date?.time ?: System.currentTimeMillis()
        }
    } catch (e: Exception) {
        Log.e("SincMapper", "Erro ao converter data ISO: $this", e)
        System.currentTimeMillis()
    }
}

// Função para converter string para Long de forma segura
fun String?.toLongSafe(): Long {
    return try {
        if (this.isNullOrBlank()) {
            System.currentTimeMillis()
        } else {
            // Se for um número puro, converte direto
            if (this.matches(Regex("\\d+"))) {
                this.toLong()
            } else {
                // Se não for número, tenta como data ISO
                this.toIsoDateLong()
            }
        }
    } catch (e: Exception) {
        Log.e("SincMapper", "Erro ao converter para Long: $this", e)
        System.currentTimeMillis()
    }
}