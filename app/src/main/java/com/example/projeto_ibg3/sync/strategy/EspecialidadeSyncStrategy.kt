package com.example.projeto_ibg3.sync.strategy

import com.example.projeto_ibg3.data.local.database.dao.EspecialidadeDao
import com.example.projeto_ibg3.data.local.database.entities.EspecialidadeEntity
import com.example.projeto_ibg3.data.mappers.toDto
import com.example.projeto_ibg3.data.mappers.toEntity
import com.example.projeto_ibg3.data.remote.api.ApiService
import com.example.projeto_ibg3.domain.model.SyncStatus
import com.example.projeto_ibg3.sync.core.ConflictResolver
import com.example.projeto_ibg3.sync.model.SyncResult
import com.example.projeto_ibg3.sync.model.SyncError
import javax.inject.Inject

class EspecialidadeSyncStrategy @Inject constructor(
    private val dao: EspecialidadeDao,
    private val api: ApiService,
    private val conflictResolver: ConflictResolver
) : SyncStrategy {

    // Método para obter deviceId
    private fun getCurrentDeviceId(): String {
        return "device_${System.currentTimeMillis()}"
    }

    override suspend fun sync(): SyncResult {
        return try {
            // 1. Upload mudanças locais
            val uploadResult = uploadLocalChanges()
            if (uploadResult is SyncResult.ERROR && !uploadResult.partialSuccess) {
                return uploadResult
            }

            // 2. Download mudanças do servidor
            val downloadResult = downloadServerChanges()
            if (downloadResult is SyncResult.ERROR && !downloadResult.partialSuccess) {
                return downloadResult
            }

            // 3. Resolver conflitos
            val conflictResult = resolveConflicts()

            // 4. Consolidar resultados
            val totalSynced = listOf(uploadResult, downloadResult, conflictResult)
                .sumOf { (it as? SyncResult.SUCCESS)?.syncedCount ?: 0 }

            val totalFailed = listOf(uploadResult, downloadResult, conflictResult)
                .sumOf { (it as? SyncResult.SUCCESS)?.failedCount ?: 0 }

            SyncResult.SUCCESS(
                syncedCount = totalSynced,
                failedCount = totalFailed,
                message = "Especialidades sincronizadas"
            )

        } catch (e: Exception) {
            SyncResult.ERROR(SyncError.UnknownError("Erro na sincronização de especialidades", e))
        }
    }

    override suspend fun syncQuick(): SyncResult {
        return try {
            val pendingCount = dao.countPendingSync()
            if (pendingCount == 0) {
                return SyncResult.SUCCESS(
                    syncedCount = 0,
                    failedCount = 0,
                    message = "Nenhuma especialidade pendente"
                )
            }

            val uploadResult = uploadLocalChanges()
            val lastSync = getLastSyncTimestamp()
            val downloadResult = downloadServerChanges(since = lastSync)

            val totalSynced = ((uploadResult as? SyncResult.SUCCESS)?.syncedCount ?: 0) +
                    ((downloadResult as? SyncResult.SUCCESS)?.syncedCount ?: 0)

            val totalFailed = ((uploadResult as? SyncResult.SUCCESS)?.failedCount ?: 0) +
                    ((downloadResult as? SyncResult.SUCCESS)?.failedCount ?: 0)

            SyncResult.SUCCESS(
                syncedCount = totalSynced,
                failedCount = totalFailed,
                message = "Sync rápido de especialidades"
            )

        } catch (e: Exception) {
            SyncResult.ERROR(SyncError.UnknownError("Erro no sync rápido", e))
        }
    }

    override suspend fun uploadLocalChanges(): SyncResult {
        return try {
            var syncedCount = 0
            var failedCount = 0

            // Upload criações - apenas entidades sem serverId
            val pendingCreations = dao.getEspecialidadesByStatus(SyncStatus.PENDING_UPLOAD)
                .filter { it.serverId == null }

            for (entity in pendingCreations) {
                try {
                    val dto = entity.toDto()
                    val response = api.createEspecialidade(dto)

                    if (response.isSuccessful) {
                        response.body()?.let { apiResponse ->
                            apiResponse.data?.let { responseDto ->
                                responseDto.serverId?.let { serverId ->
                                    dao.markAsSynced(entity.localId, serverId)
                                    syncedCount++
                                } ?: run {
                                    dao.updateSyncStatus(entity.localId, SyncStatus.UPLOAD_FAILED)
                                    failedCount++
                                }
                            }
                        } ?: run {
                            dao.updateSyncStatus(entity.localId, SyncStatus.UPLOAD_FAILED)
                            failedCount++
                        }
                    } else {
                        dao.updateSyncStatus(entity.localId, SyncStatus.UPLOAD_FAILED)
                        failedCount++
                    }
                } catch (e: Exception) {
                    dao.updateSyncStatus(entity.localId, SyncStatus.UPLOAD_FAILED)
                    failedCount++
                }
            }

            // Upload atualizações - entidades que já têm serverId
            val pendingUpdates = dao.getEspecialidadesByStatus(SyncStatus.PENDING_UPLOAD)
                .filter { it.serverId != null }

            for (entity in pendingUpdates) {
                try {
                    entity.serverId?.let { serverId ->
                        val dto = entity.toDto()
                        val response = api.updateEspecialidade(serverId, dto)

                        if (response.isSuccessful) {
                            dao.updateSyncStatus(entity.localId, SyncStatus.SYNCED)
                            syncedCount++
                        } else {
                            dao.updateSyncStatus(entity.localId, SyncStatus.UPLOAD_FAILED)
                            failedCount++
                        }
                    }
                } catch (e: Exception) {
                    dao.updateSyncStatus(entity.localId, SyncStatus.UPLOAD_FAILED)
                    failedCount++
                }
            }

            // Upload deleções
            val pendingDeletions = dao.getEspecialidadesByStatus(SyncStatus.PENDING_DELETE)
            for (entity in pendingDeletions) {
                try {
                    entity.serverId?.let { serverId ->
                        val response = api.deleteEspecialidade(serverId)

                        if (response.isSuccessful) {
                            dao.deleteEspecialidadePermanently(entity.localId)
                            syncedCount++
                        } else {
                            dao.updateSyncStatus(entity.localId, SyncStatus.DELETE_FAILED)
                            failedCount++
                        }
                    }
                } catch (e: Exception) {
                    dao.updateSyncStatus(entity.localId, SyncStatus.DELETE_FAILED)
                    failedCount++
                }
            }

            SyncResult.SUCCESS(
                syncedCount = syncedCount,
                failedCount = failedCount,
                message = "Upload de especialidades concluído"
            )

        } catch (e: Exception) {
            SyncResult.ERROR(SyncError.UnknownError("Erro no upload", e))
        }
    }

    override suspend fun downloadServerChanges(): SyncResult {
        return downloadServerChanges(since = null)
    }

    private suspend fun downloadServerChanges(since: Long? = null): SyncResult {
        return try {
            val currentDeviceId = getCurrentDeviceId()
            val response = api.getAllEspecialidades()

            if (response.isSuccessful) {
                response.body()?.let { apiResponse ->
                    apiResponse.data?.let { dtosList ->
                        var syncedCount = 0
                        var conflictCount = 0

                        for (dto in dtosList) {
                            val serverEntity = dto.toEntity(
                                deviceId = currentDeviceId,
                                syncStatus = SyncStatus.SYNCED
                            )

                            val localEntity = dto.serverId?.let { serverId ->
                                dao.getEspecialidadeByServerId(serverId)
                            }

                            when {
                                localEntity == null -> {
                                    dao.insertEspecialidade(serverEntity)
                                    syncedCount++
                                }
                                needsConflictResolution(localEntity, serverEntity) -> {
                                    dao.updateSyncStatus(localEntity.localId, SyncStatus.CONFLICT)
                                    conflictCount++
                                }
                                else -> {
                                    val updatedEntity = serverEntity.copy(
                                        localId = localEntity.localId,
                                        deviceId = localEntity.deviceId,
                                        syncStatus = SyncStatus.SYNCED
                                    )
                                    dao.updateEspecialidade(updatedEntity)
                                    syncedCount++
                                }
                            }
                        }

                        SyncResult.SUCCESS(
                            syncedCount = syncedCount,
                            failedCount = 0,
                            conflictCount = conflictCount,
                            message = "Download concluído"
                        )
                    } ?: SyncResult.SUCCESS(
                        syncedCount = 0,
                        failedCount = 0,
                        message = "Nenhuma especialidade para download"
                    )
                } ?: SyncResult.SUCCESS(
                    syncedCount = 0,
                    failedCount = 0,
                    message = "Resposta vazia do servidor"
                )
            } else {
                SyncResult.ERROR(
                    SyncError.ServerError("Erro no download", response.code())
                )
            }

        } catch (e: Exception) {
            SyncResult.ERROR(SyncError.UnknownError("Erro no download", e))
        }
    }

    override suspend fun resolveConflicts(): SyncResult {
        return try {
            val conflicts = dao.getEspecialidadesByStatus(SyncStatus.CONFLICT)
            var resolvedCount = 0
            var failedCount = 0

            for (conflictEntity in conflicts) {
                try {
                    conflictEntity.serverId?.let { serverId ->
                        val serverResponse = api.getEspecialidadeById(serverId)

                        if (serverResponse.isSuccessful) {
                            serverResponse.body()?.let { apiResponse ->
                                apiResponse.data?.let { serverDto ->
                                    val serverEntity = serverDto.toEntity(
                                        deviceId = conflictEntity.deviceId,
                                        syncStatus = SyncStatus.SYNCED
                                    )

                                    val resolvedEntity = conflictResolver.resolveConflict(
                                        conflictEntity,
                                        serverEntity
                                    )

                                    dao.updateEspecialidade(
                                        resolvedEntity.copy(syncStatus = SyncStatus.SYNCED)
                                    )
                                    resolvedCount++
                                }
                            }
                        } else {
                            failedCount++
                        }
                    } ?: run {
                        failedCount++
                    }
                } catch (e: Exception) {
                    failedCount++
                }
            }

            SyncResult.SUCCESS(
                syncedCount = resolvedCount,
                failedCount = failedCount,
                message = "Conflitos resolvidos"
            )

        } catch (e: Exception) {
            SyncResult.ERROR(SyncError.UnknownError("Erro ao resolver conflitos", e))
        }
    }

    override suspend fun hasPendingChanges(): Boolean {
        return try {
            dao.countPendingSync() > 0
        } catch (e: Exception) {
            false
        }
    }

    override suspend fun getStats(): Map<String, Int> {
        return try {
            mapOf(
                "synced" to getCountByStatus(SyncStatus.SYNCED),
                "pending_upload" to getCountByStatus(SyncStatus.PENDING_UPLOAD),
                "pending_delete" to getCountByStatus(SyncStatus.PENDING_DELETE),
                "failed" to getCountByStatus(SyncStatus.UPLOAD_FAILED) + getCountByStatus(SyncStatus.DELETE_FAILED),
                "conflicts" to getCountByStatus(SyncStatus.CONFLICT)
            )
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private suspend fun getCountByStatus(status: SyncStatus): Int {
        return try {
            dao.countByStatus(status)
        } catch (e: Exception) {
            0
        }
    }

    private fun needsConflictResolution(local: EspecialidadeEntity, server: EspecialidadeEntity): Boolean {
        return local.syncStatus != SyncStatus.SYNCED &&
                local.updatedAt > server.updatedAt
    }

    private suspend fun getLastSyncTimestamp(): Long {
        return try {
            dao.getLastSyncTimestamp() ?: 0L
        } catch (e: Exception) {
            dao.getLastUpdatedTimestamp() ?: 0L
        }
    }
}