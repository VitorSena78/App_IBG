package com.example.projeto_ibg3.sync.strategy

import com.example.projeto_ibg3.data.local.database.dao.EspecialidadeDao
import com.example.projeto_ibg3.data.local.database.entities.EspecialidadeEntity
import com.example.projeto_ibg3.data.mappers.toDto
import com.example.projeto_ibg3.data.mappers.toEntity
import com.example.projeto_ibg3.data.remote.api.PacienteApiService
import com.example.projeto_ibg3.domain.model.SyncStatus
import com.example.projeto_ibg3.sync.core.ConflictResolver
import com.example.projeto_ibg3.sync.model.SyncResult
import com.example.projeto_ibg3.sync.model.SyncError
import javax.inject.Inject

class EspecialidadeSyncStrategy @Inject constructor(
    private val dao: EspecialidadeDao,
    private val api: PacienteApiService,
    private val conflictResolver: ConflictResolver
) : SyncStrategy {

    // Método para obter deviceId (você pode implementar conforme sua necessidade)
    private fun getCurrentDeviceId(): String {
        // Implementar lógica para obter deviceId
        // Por exemplo: Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        return "device_${System.currentTimeMillis()}" // Implementação temporária
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
                return SyncResult.SUCCESS(0, message = "Nenhuma especialidade pendente")
            }

            // Upload apenas itens pendentes
            val uploadResult = uploadLocalChanges()

            // Download apenas modificações recentes
            val lastSync = getLastSyncTimestamp()
            val downloadResult = downloadServerChanges(since = lastSync)

            val totalSynced = ((uploadResult as? SyncResult.SUCCESS)?.syncedCount ?: 0) +
                    ((downloadResult as? SyncResult.SUCCESS)?.syncedCount ?: 0)

            val totalFailed = ((uploadResult as? SyncResult.SUCCESS)?.failedCount ?: 0) +
                    ((downloadResult as? SyncResult.SUCCESS)?.failedCount ?: 0)

            SyncResult.SUCCESS(totalSynced, totalFailed, message = "Sync rápido de especialidades")

        } catch (e: Exception) {
            SyncResult.ERROR(SyncError.UnknownError("Erro no sync rápido", e))
        }
    }

    override suspend fun uploadLocalChanges(): SyncResult {
        return try {
            var syncedCount = 0
            var failedCount = 0
            val currentDeviceId = getCurrentDeviceId()

            // Upload criações
            val pendingCreations = dao.getEspecialidadesByStatus(SyncStatus.PENDING_UPLOAD)
            for (entity in pendingCreations) {
                try {
                    val dto = entity.toDto()
                    val response = api.createEspecialidade(dto)
                    if (response.isSuccessful) {
                        response.body()?.let { dto ->
                            dao.markAsSynced(entity.localId, dto.serverId!!)
                            syncedCount++
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

            // Upload atualizações
            val pendingUpdates = dao.getEspecialidadesByStatus(SyncStatus.PENDING_UPLOAD)
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

            SyncResult.SUCCESS(syncedCount, failedCount, message = "Upload de especialidades concluído")

        } catch (e: Exception) {
            SyncResult.ERROR(SyncError.UnknownError("Erro no upload", e))
        }
    }

    // Método sem parâmetros que chama o método com parâmetros
    override suspend fun downloadServerChanges(): SyncResult {
        return downloadServerChanges(since = null)
    }

    // Método com parâmetros (implementação principal)
    private suspend fun downloadServerChanges(since: Long? = null): SyncResult {
        return try {
            val currentDeviceId = getCurrentDeviceId()

            // Usar o método disponível na API
            val response = api.getAllEspecialidades()

            if (response.isSuccessful) {
                response.body()?.let { dtos ->
                    var syncedCount = 0
                    var conflictCount = 0

                    dtos.forEach { dto ->
                        val serverEntity = dto.toEntity(deviceId = currentDeviceId)
                        val localEntity = dao.getEspecialidadeByServerId(serverEntity.serverId!!)

                        when {
                            localEntity == null -> {
                                // Novo item do servidor
                                val newEntity = serverEntity.copy(
                                    deviceId = currentDeviceId,
                                    syncStatus = SyncStatus.SYNCED
                                )
                                dao.insertEspecialidade(newEntity)
                                syncedCount++
                            }
                            needsConflictResolution(localEntity, serverEntity) -> {
                                // Marcar como conflito
                                dao.updateSyncStatus(localEntity.localId, SyncStatus.CONFLICT)
                                conflictCount++
                            }
                            else -> {
                                // Atualizar com dados do servidor
                                val updatedEntity = serverEntity.copy(
                                    localId = localEntity.localId,
                                    deviceId = localEntity.deviceId, // Manter o deviceId original
                                    syncStatus = SyncStatus.SYNCED
                                )
                                dao.updateEspecialidade(updatedEntity)
                                syncedCount++
                            }
                        }
                    }

                    SyncResult.SUCCESS(
                        syncedCount = syncedCount,
                        conflictCount = conflictCount,
                        message = "Download concluído"
                    )
                } ?: SyncResult.SUCCESS(0, message = "Nenhuma especialidade para download")
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

            conflicts.forEach { conflictEntity ->
                try {
                    // Buscar versão do servidor
                    val serverResponse = api.getEspecialidadeById(conflictEntity.serverId!!)

                    if (serverResponse.isSuccessful) {
                        serverResponse.body()?.let { serverDto ->
                            val serverEntity = serverDto.toEntity(deviceId = conflictEntity.deviceId)

                            // Usar ConflictResolver para decidir qual versão manter
                            val resolvedEntity = conflictResolver.resolveConflict(conflictEntity, serverEntity)

                            dao.updateEspecialidade(resolvedEntity.copy(syncStatus = SyncStatus.SYNCED))
                            resolvedCount++
                        }
                    } else {
                        failedCount++
                    }
                } catch (e: Exception) {
                    failedCount++
                }
            }

            SyncResult.SUCCESS(resolvedCount, failedCount, message = "Conflitos resolvidos")

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

    // Método helper para contar por status
    private suspend fun getCountByStatus(status: SyncStatus): Int {
        return try {
            dao.getEspecialidadesByStatus(status).size
        } catch (e: Exception) {
            0
        }
    }

    private fun needsConflictResolution(local: EspecialidadeEntity, server: EspecialidadeEntity): Boolean {
        return local.syncStatus != SyncStatus.SYNCED &&
                local.updatedAt > server.updatedAt
    }

    // Método helper para obter o timestamp da última sincronização
    private suspend fun getLastSyncTimestamp(): Long {
        return try {
            // Buscar pelo registro mais recente sincronizado
            dao.getLastSyncTimestamp() ?: 0L
        } catch (e: Exception) {
            // Se não conseguir, usar o timestamp do último update
            dao.getLastUpdatedTimestamp() ?: 0L
        }
    }
}