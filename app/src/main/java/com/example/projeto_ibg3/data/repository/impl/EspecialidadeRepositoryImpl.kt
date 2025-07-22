package com.example.projeto_ibg3.data.repository.impl

import com.example.projeto_ibg3.data.local.database.dao.EspecialidadeDao
import com.example.projeto_ibg3.data.local.database.entities.EspecialidadeEntity
import com.example.projeto_ibg3.data.mappers.toDomainModelList
import com.example.projeto_ibg3.data.mappers.toEntity
import com.example.projeto_ibg3.domain.model.Especialidade
import com.example.projeto_ibg3.domain.model.SyncStatus
import com.example.projeto_ibg3.domain.repository.EspecialidadeRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EspecialidadeRepositoryImpl @Inject constructor(
    private val especialidadeDao: EspecialidadeDao
): EspecialidadeRepository {

    // OPERAÇÕES BÁSICAS
    override fun getAllEspecialidades(): Flow<List<EspecialidadeEntity>> {
        return especialidadeDao.getAllEspecialidades()
    }

    override suspend fun getEspecialidadeById(localId: String): EspecialidadeEntity? {
        return especialidadeDao.getEspecialidadeById(localId)
    }

    override suspend fun getEspecialidadeByServerId(serverId: Long?): EspecialidadeEntity? {
        return especialidadeDao.getEspecialidadeByServerId(serverId)
    }

    override suspend fun getEspecialidadeByName(nome: String): EspecialidadeEntity? {
        return especialidadeDao.getEspecialidadeByName(nome)
    }

    override suspend fun insertEspecialidade(especialidade: Especialidade): String {
        val localId = if (especialidade.localId.isEmpty()) UUID.randomUUID().toString() else especialidade.localId
        val entity = especialidade.copy(localId = localId).toEntity(
            deviceId = "default_device",
            syncStatus = SyncStatus.PENDING_UPLOAD
        )
        especialidadeDao.insertEspecialidade(entity)
        return localId
    }

    override suspend fun updateEspecialidade(especialidade: Especialidade) {
        val entity = especialidade.toEntity(
            deviceId = "default_device",
            syncStatus = SyncStatus.PENDING_UPLOAD
        )
        especialidadeDao.updateEspecialidade(entity)
    }

    override suspend fun getEspecialidadeCount(): Int {
        return especialidadeDao.getEspecialidadesCount()
    }

    // INSERÇÃO E ATUALIZAÇÃO
    suspend fun insertEspecialidade(especialidade: EspecialidadeEntity): Long {
        val especialidadeToInsert = especialidade.copy(
            syncStatus = SyncStatus.PENDING_UPLOAD,
            updatedAt = System.currentTimeMillis()
        )
        return especialidadeDao.insertEspecialidade(especialidadeToInsert)
    }

    override suspend fun searchEspecialidades(query: String): List<Especialidade> {
        return especialidadeDao.searchEspecialidadesByName(query).toDomainModelList()
    }

    // SINCRONIZAÇÃO
    override suspend fun getPendingSyncItems(): List<EspecialidadeEntity> {
        return especialidadeDao.getPendingSyncItems()
    }

    override suspend fun updateSyncStatus(localId: String, newStatus: SyncStatus) {
        especialidadeDao.updateSyncStatus(
            localId = localId,
            newStatus = newStatus,
            timestamp = System.currentTimeMillis()
        )
    }

    override suspend fun markAsSynced(localId: String, serverId: Long) {
        especialidadeDao.markAsSynced(
            localId = localId,
            serverId = serverId,
            status = SyncStatus.SYNCED,
            timestamp = System.currentTimeMillis()
        )
    }

    override suspend fun markAsSyncFailed(localId: String, error: String) {
        especialidadeDao.updateSyncStatus(
            localId = localId,
            newStatus = SyncStatus.UPLOAD_FAILED,
            timestamp = System.currentTimeMillis()
        )
    }

    override suspend fun getModifiedSince(timestamp: Long): List<EspecialidadeEntity> {
        return especialidadeDao.getModifiedSince(timestamp)
    }

    suspend fun resetSyncStatus(oldStatus: SyncStatus, newStatus: SyncStatus) {
        especialidadeDao.resetSyncStatus(oldStatus, newStatus)
    }

    // MÉTODOS AUXILIARES
    override suspend fun especialidadeExists(nome: String, excludeId: String): Boolean {
        return especialidadeDao.countEspecialidadesByName(nome, excludeId) > 0
    }

    suspend fun countPendingSync(): Int {
        return especialidadeDao.countPendingSync()
    }

    override suspend fun cleanupOldDeletedRecords(cutoffTimestamp: Long) {
        especialidadeDao.cleanupOldDeletedRecords(cutoffTimestamp)
    }

    // MÉTODOS CONVENIENTES PARA SINCRONIZAÇÃO
    override suspend fun getPendingUploads(): List<EspecialidadeEntity> {
        return especialidadeDao.getEspecialidadesByMultipleStatus(
            listOf(SyncStatus.PENDING_UPLOAD, SyncStatus.UPLOAD_FAILED)
        )
    }

    override suspend fun getPendingDeletions(): List<EspecialidadeEntity> {
        return especialidadeDao.getEspecialidadesByMultipleStatus(
            listOf(SyncStatus.PENDING_DELETE, SyncStatus.DELETE_FAILED)
        )
    }

    override suspend fun hasPendingChanges(): Boolean {
        return countPendingSync() > 0
    }

    override suspend fun retryFailedSync() {
        // Redefine status de upload com falha
        resetSyncStatus(SyncStatus.UPLOAD_FAILED, SyncStatus.PENDING_UPLOAD)
        // Redefine status de delete com falha
        resetSyncStatus(SyncStatus.DELETE_FAILED, SyncStatus.PENDING_DELETE)
    }
}