package com.example.projeto_ibg3.data.repository.impl

import com.example.projeto_ibg3.data.local.database.dao.EspecialidadeDao
import com.example.projeto_ibg3.data.local.database.entities.EspecialidadeEntity
import com.example.projeto_ibg3.domain.model.Especialidade
import com.example.projeto_ibg3.domain.model.SyncStatus
import com.example.projeto_ibg3.domain.repository.EspecialidadeRepository
import kotlinx.coroutines.flow.Flow
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
        TODO("Not yet implemented")
    }

    override suspend fun updateEspecialidade(especialidade: Especialidade) {
        TODO("Not yet implemented")
    }

    suspend fun searchEspecialidadesByName(nome: String): List<EspecialidadeEntity> {
        return especialidadeDao.searchEspecialidadesByName(nome)
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

    suspend fun insertEspecialidades(especialidades: List<EspecialidadeEntity>) {
        val especialidadesToInsert = especialidades.map { especialidade ->
            especialidade.copy(
                syncStatus = SyncStatus.PENDING_UPLOAD,
                updatedAt = System.currentTimeMillis()
            )
        }
        especialidadeDao.insertEspecialidades(especialidadesToInsert)
    }

    suspend fun updateEspecialidade(especialidade: EspecialidadeEntity) {
        val especialidadeToUpdate = especialidade.copy(
            syncStatus = SyncStatus.PENDING_UPLOAD,
            updatedAt = System.currentTimeMillis()
        )
        especialidadeDao.updateEspecialidade(especialidadeToUpdate)
    }

    suspend fun insertOrUpdateEspecialidade(especialidade: EspecialidadeEntity) {
        val especialidadeToSave = especialidade.copy(
            syncStatus = if (especialidade.serverId == null) SyncStatus.PENDING_UPLOAD else SyncStatus.SYNCED,
            updatedAt = System.currentTimeMillis()
        )
        especialidadeDao.insertOrUpdateEspecialidade(especialidadeToSave)
    }

    // EXCLUSÃO
    override suspend fun deleteEspecialidade(localId: String) {
        especialidadeDao.markAsDeleted(
            localId = localId,
            status = SyncStatus.PENDING_DELETE,
            timestamp = System.currentTimeMillis()
        )
    }

    override suspend fun searchEspecialidades(query: String): List<Especialidade> {
        TODO("Not yet implemented")
    }

    suspend fun deleteEspecialidadePermanently(localId: String) {
        especialidadeDao.deleteEspecialidadePermanently(localId)
    }

    suspend fun restoreDeleted(localId: String) {
        especialidadeDao.restoreDeleted(
            localId = localId,
            status = SyncStatus.PENDING_UPLOAD,
            timestamp = System.currentTimeMillis()
        )
    }

    // SINCRONIZAÇÃO
    override suspend fun getPendingSyncItems(): List<EspecialidadeEntity> {
        return especialidadeDao.getPendingSyncItems()
    }

    suspend fun getEspecialidadesByStatus(status: SyncStatus): List<EspecialidadeEntity> {
        return especialidadeDao.getEspecialidadesByStatus(status)
    }

    suspend fun getEspecialidadesByMultipleStatus(statuses: List<SyncStatus>): List<EspecialidadeEntity> {
        return especialidadeDao.getEspecialidadesByMultipleStatus(statuses)
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
        TODO("Not yet implemented")
    }

    suspend fun markAsSyncFailed(localId: String, isDelete: Boolean = false) {
        val failedStatus = if (isDelete) SyncStatus.DELETE_FAILED else SyncStatus.UPLOAD_FAILED
        especialidadeDao.updateSyncStatus(
            localId = localId,
            newStatus = failedStatus,
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

    suspend fun getDeletedEspecialidades(): List<EspecialidadeEntity> {
        return especialidadeDao.getDeletedEspecialidades()
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