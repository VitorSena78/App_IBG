package com.example.projeto_ibg3.data.mappers

import com.example.projeto_ibg3.data.local.database.dao.EspecialidadeDao
import com.example.projeto_ibg3.data.local.database.dao.PacienteDao
import com.example.projeto_ibg3.data.local.database.entities.PacienteEspecialidadeEntity
import com.example.projeto_ibg3.data.remote.dto.PacienteEspecialidadeDTO
import com.example.projeto_ibg3.domain.model.SyncStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PacienteEspecialidadeMapper @Inject constructor(
    private val pacienteDao: PacienteDao,
    private val especialidadeDao: EspecialidadeDao
) {

    /**
     * Converte PacienteEspecialidadeDTO para PacienteEspecialidadeEntity
     * Busca automaticamente os localIds usando os serverIds
     */
    suspend fun dtoToEntity(
        dto: PacienteEspecialidadeDTO,
        deviceId: String = "",
        syncStatus: SyncStatus = SyncStatus.SYNCED
    ): PacienteEspecialidadeEntity {

        // Buscar pacienteLocalId usando o serverId
        val pacienteLocalId = dto.pacienteServerId?.let { serverId ->
            pacienteDao.getPacienteByServerId(serverId)?.localId
        } ?: throw IllegalStateException("Paciente com serverId ${dto.pacienteServerId} não encontrado")

        // Buscar especialidadeLocalId usando o serverId
        val especialidadeLocalId = dto.especialidadeServerId?.let { serverId ->
            especialidadeDao.getEspecialidadeByServerId(serverId)?.localId
        } ?: throw IllegalStateException("Especialidade com serverId ${dto.especialidadeServerId} não encontrada")

        return PacienteEspecialidadeEntity(
            pacienteLocalId = pacienteLocalId,
            especialidadeLocalId = especialidadeLocalId,
            pacienteServerId = dto.pacienteServerId,
            especialidadeServerId = dto.especialidadeServerId,
            dataAtendimento = dto.dataAtendimento.toDateLong(),
            syncStatus = syncStatus,
            deviceId = deviceId,
            createdAt = dto.createdAt.toIsoDateLong(),
            updatedAt = dto.updatedAt.toIsoDateLong(),
            lastSyncTimestamp = dto.lastSyncTimestamp ?: System.currentTimeMillis(),
            isDeleted = dto.isDeleted,
            version = 1,
            conflictData = null,
            syncAttempts = 0,
            lastSyncAttempt = 0,
            syncError = null
        )
    }

    /**
     * Converte PacienteEspecialidadeDTO para PacienteEspecialidadeEntity
     * usando localIds já conhecidos (mais eficiente quando você já tem os IDs)
     */
    fun dtoToEntityWithLocalIds(
        dto: PacienteEspecialidadeDTO,
        pacienteLocalId: String,
        especialidadeLocalId: String,
        deviceId: String = "",
        syncStatus: SyncStatus = SyncStatus.SYNCED
    ): PacienteEspecialidadeEntity {
        return PacienteEspecialidadeEntity(
            pacienteLocalId = pacienteLocalId,
            especialidadeLocalId = especialidadeLocalId,
            pacienteServerId = dto.pacienteServerId,
            especialidadeServerId = dto.especialidadeServerId,
            dataAtendimento = dto.dataAtendimento.toDateLong(),
            syncStatus = syncStatus,
            deviceId = deviceId,
            createdAt = dto.createdAt.toIsoDateLong(),
            updatedAt = dto.updatedAt.toIsoDateLong(),
            lastSyncTimestamp = dto.lastSyncTimestamp ?: System.currentTimeMillis(),
            isDeleted = dto.isDeleted,
            version = 1,
            conflictData = null,
            syncAttempts = 0,
            lastSyncAttempt = 0,
            syncError = null
        )
    }

    /**
     * Converte lista de DTOs para lista de Entities
     */
    suspend fun dtoListToEntityList(
        dtoList: List<PacienteEspecialidadeDTO>,
        deviceId: String = "",
        syncStatus: SyncStatus = SyncStatus.SYNCED
    ): List<PacienteEspecialidadeEntity> {
        return dtoList.mapNotNull { dto ->
            try {
                dtoToEntity(dto, deviceId, syncStatus)
            } catch (e: Exception) {
                // Log do erro mas continua processando outros itens
                println("Erro ao converter DTO para Entity: ${e.message}")
                null
            }
        }
    }

    /**
     * Converte lista de DTOs para lista de Entities de forma segura
     * Retorna apenas os que conseguiram ser convertidos com sucesso
     */
    suspend fun dtoListToEntityListSafe(
        dtoList: List<PacienteEspecialidadeDTO>,
        deviceId: String = "",
        syncStatus: SyncStatus = SyncStatus.SYNCED
    ): Pair<List<PacienteEspecialidadeEntity>, List<String>> {
        val successfulEntities = mutableListOf<PacienteEspecialidadeEntity>()
        val errors = mutableListOf<String>()

        dtoList.forEach { dto ->
            try {
                val entity = dtoToEntity(dto, deviceId, syncStatus)
                successfulEntities.add(entity)
            } catch (e: Exception) {
                val errorMsg = "Erro ao converter DTO (paciente: ${dto.pacienteServerId}, especialidade: ${dto.especialidadeServerId}): ${e.message}"
                errors.add(errorMsg)
                println(errorMsg)
            }
        }

        return Pair(successfulEntities, errors)
    }

    /**
     * Método específico para quando você tem o pacienteLocalId mas precisa buscar especialidadeLocalId
     */
    suspend fun dtoToEntityForPaciente(
        dto: PacienteEspecialidadeDTO,
        pacienteLocalId: String,
        deviceId: String = "",
        syncStatus: SyncStatus = SyncStatus.SYNCED
    ): PacienteEspecialidadeEntity {

        // Buscar apenas especialidadeLocalId usando o serverId
        val especialidadeLocalId = dto.especialidadeServerId?.let { serverId ->
            especialidadeDao.getEspecialidadeByServerId(serverId)?.localId
        } ?: throw IllegalStateException("Especialidade com serverId ${dto.especialidadeServerId} não encontrada")

        return dtoToEntityWithLocalIds(
            dto = dto,
            pacienteLocalId = pacienteLocalId,
            especialidadeLocalId = especialidadeLocalId,
            deviceId = deviceId,
            syncStatus = syncStatus
        )
    }
}