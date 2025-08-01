package com.example.projeto_ibg3.data.mappers

import com.example.projeto_ibg3.data.local.database.entities.EspecialidadeEntity
import com.example.projeto_ibg3.data.remote.dto.EspecialidadeDto
import com.example.projeto_ibg3.domain.model.SyncStatus
import com.example.projeto_ibg3.domain.model.Especialidade
import java.util.Date
import java.util.UUID

//EspecialidadeMapper.kt

// ============================================================================
// MAPPERS INDIVIDUAIS - ENTITY ↔ DOMAIN MODEL
// ============================================================================

// Converter Entity para Domain Model
fun EspecialidadeEntity.toDomainModel(): Especialidade {
    return Especialidade(
        localId = this.localId,
        serverId = this.serverId,
        nome = this.nome,
        fichas = this.fichas, // NOVO CAMPO
        atendimentosRestantesHoje = this.atendimentosRestantesHoje,
        atendimentosTotaisHoje = this.atendimentosTotaisHoje,
        isDeleted = this.isDeleted,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

// Converter Domain Model para Entity
fun Especialidade.toEntity(
    deviceId: String,
    syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD,
    lastSyncTimestamp: Long = 0
): EspecialidadeEntity {
    return EspecialidadeEntity(
        localId = this.localId,
        serverId = this.serverId,
        nome = this.nome,
        fichas = this.fichas, // NOVO CAMPO
        atendimentosRestantesHoje = this.atendimentosRestantesHoje,
        atendimentosTotaisHoje = this.atendimentosTotaisHoje,
        syncStatus = syncStatus,
        deviceId = deviceId,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
        lastSyncTimestamp = lastSyncTimestamp,
        isDeleted = this.isDeleted
    )
}

// DTO para Domain Model - CORRIGIDO
fun EspecialidadeDto.toDomain(): Especialidade {
    return Especialidade(
        localId = UUID.randomUUID().toString(),
        serverId = this.serverId, // Agora mapeia corretamente para "id" do JSON
        nome = this.nome,
        fichas = this.fichas ?: 0,
        atendimentosRestantesHoje = this.atendimentosRestantesHoje ?: 0,
        atendimentosTotaisHoje = this.atendimentosTotaisHoje ?: 0,
        isDeleted = this.isDeleted ?: false
    )
}

// ============================================================================
// MAPPERS INDIVIDUAIS - DTO ↔ ENTITY
// ============================================================================

// Converter DTO para Entity - CORRIGIDO
fun EspecialidadeDto.toEntity(
    deviceId: String,
    syncStatus: SyncStatus = SyncStatus.SYNCED
): EspecialidadeEntity {

    return EspecialidadeEntity(
        localId = UUID.randomUUID().toString(),
        serverId = this.serverId,
        nome = this.nome,
        fichas = this.fichas ?: 0,
        atendimentosRestantesHoje = this.atendimentosRestantesHoje ?: 0,
        atendimentosTotaisHoje = this.atendimentosTotaisHoje ?: 0,
        syncStatus = syncStatus,
        deviceId = deviceId,
        createdAt = this.getCreatedAtTimestamp(),
        updatedAt = this.getUpdatedAtTimestamp(),
        isDeleted = this.isDeleted ?: false
    )
}

// Converter Entity para DTO
fun EspecialidadeEntity.toDto(): EspecialidadeDto {
    return EspecialidadeDto(
        serverId = this.serverId, // Mapeia corretamente
        nome = this.nome,
        fichas = this.fichas,
        atendimentosRestantesHoje = this.atendimentosRestantesHoje,
        atendimentosTotaisHoje = this.atendimentosTotaisHoje,
        createdAt = dateTimeFormat.format(Date(this.createdAt)),
        updatedAt = dateTimeFormat.format(Date(this.updatedAt)),
        isDeleted = this.isDeleted
    )
}

// Entity para Domain Model
fun EspecialidadeEntity.toDomain(): Especialidade {
    return Especialidade(
        localId = this.localId,
        serverId = this.serverId,
        nome = this.nome
    )
}

// ENTITY → DOMAIN (LIST)
fun List<EspecialidadeEntity>.toDomain(): List<Especialidade> {
    return this.map { it.toDomain() }
}

// ============================================================================
// MAPPERS INDIVIDUAIS - DTO ↔ DOMAIN MODEL
// ============================================================================

// Converter DTO para Domain Model - CORRIGIDO
fun EspecialidadeDto.toDomainModel(): Especialidade {
    return Especialidade(
        localId = UUID.randomUUID().toString(),
        serverId = this.serverId, // Agora mapeia corretamente
        nome = this.nome,
        fichas = this.fichas ?: 0,
        atendimentosRestantesHoje = this.atendimentosRestantesHoje ?: 0,
        atendimentosTotaisHoje = this.atendimentosTotaisHoje ?: 0,
        isDeleted = this.isDeleted ?: false
    )
}

// Converter Domain Model para DTO
fun Especialidade.toDto(
    deviceId: String? = null,
    lastSyncTimestamp: Long = 0,
    createdAt: Long? = null,
    updatedAt: Long? = null,
    isDeleted: Boolean = false
): EspecialidadeDto {
    return EspecialidadeDto(
        serverId = this.serverId, // Mapeia corretamente
        nome = this.nome,
        createdAt = null, // Converter timestamp para ISO se necessário
        updatedAt = null, // Converter timestamp para ISO se necessário
        isDeleted = isDeleted
    )
}

// ============================================================================
// MAPPERS PARA LISTAS - NOMES ESPECÍFICOS PARA EVITAR CONFLITOS
// ============================================================================

// ENTITY → DOMAIN MODEL (LIST)
@JvmName("entityListToDomainModelList")
fun List<EspecialidadeEntity>.toDomainModelList(): List<Especialidade> {
    return this.map { it.toDomainModel() }
}

// DTO → DOMAIN MODEL (LIST)
@JvmName("dtoListToDomainModelList")
fun List<EspecialidadeDto>.toDomainModelList(): List<Especialidade> {
    return this.map { it.toDomainModel() }
}

// DOMAIN MODEL → ENTITY (LIST)
@JvmName("domainModelListToEntityList")
fun List<Especialidade>.toEntityList(
    deviceId: String,
    syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD
): List<EspecialidadeEntity> {
    return this.map { it.toEntity(deviceId = deviceId, syncStatus = syncStatus) }
}

// DTO → ENTITY (LIST)
@JvmName("dtoListToEntityList")
fun List<EspecialidadeDto>.toEntityList(
    deviceId: String,
    syncStatus: SyncStatus = SyncStatus.SYNCED
): List<EspecialidadeEntity> {
    return this.map { it.toEntity(deviceId = deviceId, syncStatus = syncStatus) }
}

// ENTITY → DTO (LIST)
fun List<EspecialidadeEntity>.toDtoList(): List<EspecialidadeDto> {
    return this.map { it.toDto() }
}

// DOMAIN MODEL → DTO (LIST)
fun List<Especialidade>.toDtoList(
    deviceId: String? = null,
    lastSyncTimestamp: Long = 0,
    createdAt: Long? = null,
    updatedAt: Long? = null,
    isDeleted: Boolean = false
): List<EspecialidadeDto> {
    return this.map {
        it.toDto(
            deviceId = deviceId,
            lastSyncTimestamp = lastSyncTimestamp,
            createdAt = createdAt,
            updatedAt = updatedAt,
            isDeleted = isDeleted
        )
    }
}

// ============================================================================
// MAPPERS LEGADOS (para compatibilidade com código existente)
// ============================================================================

// Mapper legado: mantém apenas para compatibilidade
@Deprecated("Use toDomainModel() instead", ReplaceWith("toDomainModel()"))
fun EspecialidadeEntity.toEspecialidade(): Especialidade {
    return this.toDomainModel()
}

// Mapper legado: mantém apenas para compatibilidade
@Deprecated("Use toDomainModel() instead", ReplaceWith("toDomainModel()"))
fun EspecialidadeEntity.toModel(): Especialidade {
    return this.toDomainModel()
}

// Mapper legado: mantém apenas para compatibilidade
@Deprecated("Use toDomainModelList() instead", ReplaceWith("toDomainModelList()"))
fun List<EspecialidadeEntity>.toModelList(): List<Especialidade> {
    return this.toDomainModelList()
}