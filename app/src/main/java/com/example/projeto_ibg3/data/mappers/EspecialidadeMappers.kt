package com.example.projeto_ibg3.data.mappers

import com.example.projeto_ibg3.data.local.database.entities.EspecialidadeEntity
import com.example.projeto_ibg3.data.remote.dto.EspecialidadeDto
import com.example.projeto_ibg3.domain.model.SyncStatus
import com.example.projeto_ibg3.domain.model.Especialidade
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
        nome = this.nome
    )
}

// Converter Domain Model para Entity
fun Especialidade.toEntity(
    deviceId: String,
    syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD,
    createdAt: Long = System.currentTimeMillis(),
    updatedAt: Long = System.currentTimeMillis(),
    lastSyncTimestamp: Long = 0,
    isDeleted: Boolean = false
): EspecialidadeEntity {
    return EspecialidadeEntity(
        localId = this.localId,
        serverId = this.serverId,
        nome = this.nome,
        syncStatus = syncStatus,
        deviceId = deviceId,
        createdAt = createdAt,
        updatedAt = updatedAt,
        lastSyncTimestamp = lastSyncTimestamp,
        isDeleted = isDeleted
    )
}

// DTO para Domain Model - CORRIGIDO
fun EspecialidadeDto.toDomain(): Especialidade {
    return Especialidade(
        localId = this.localId,
        serverId = this.serverId, // Agora mapeia corretamente para "id" do JSON
        nome = this.nome
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
    val validLocalId = if (this.localId.isNullOrBlank()) {
        UUID.randomUUID().toString()
    } else {
        this.localId
    }
    return EspecialidadeEntity(
        localId = validLocalId,
        serverId = this.serverId, // Agora mapeia corretamente
        nome = this.nome,
        syncStatus = syncStatus,
        deviceId = deviceId,
        createdAt = this.getCreatedAtTimestamp(), // CORRIGIDO: converter data ISO
        updatedAt = this.getUpdatedAtTimestamp(), // CORRIGIDO: converter data ISO
        lastSyncTimestamp = this.lastSyncTimestamp ?: System.currentTimeMillis(),
        isDeleted = this.isDeleted ?: false
    )
}

// Converter Entity para DTO
fun EspecialidadeEntity.toDto(): EspecialidadeDto {
    return EspecialidadeDto(
        serverId = this.serverId, // Mapeia corretamente
        localId = this.localId,
        nome = this.nome,
        deviceId = this.deviceId,
        lastSyncTimestamp = this.lastSyncTimestamp,
        createdAt = null, // Converter timestamp para ISO se necessário
        updatedAt = null, // Converter timestamp para ISO se necessário
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
        localId = this.localId,
        serverId = this.serverId, // Agora mapeia corretamente
        nome = this.nome
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
        localId = this.localId,
        nome = this.nome,
        deviceId = deviceId,
        lastSyncTimestamp = lastSyncTimestamp,
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