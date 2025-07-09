package com.example.projeto_ibg3.data.repository

import com.example.projeto_ibg3.data.dao.EspecialidadeDao
import com.example.projeto_ibg3.data.entity.EspecialidadeEntity
import com.example.projeto_ibg3.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EspecialidadeRepository @Inject constructor(
    private val especialidadeDao: EspecialidadeDao
) {

    // OPERAÇÕES BÁSICAS
    fun getAllEspecialidades(): Flow<List<EspecialidadeEntity>> {
        return especialidadeDao.getAllEspecialidades()
    }

    suspend fun getEspecialidadeById(id: Long): EspecialidadeEntity? {
        return especialidadeDao.getEspecialidadeById(id)
    }

    suspend fun getEspecialidadeByServerId(serverId: Long): EspecialidadeEntity? {
        return especialidadeDao.getEspecialidadeByServerId(serverId)
    }

    suspend fun getEspecialidadeByName(nome: String): EspecialidadeEntity? {
        return especialidadeDao.getEspecialidadeByName(nome)
    }

    suspend fun searchEspecialidadesByName(nome: String): List<EspecialidadeEntity> {
        return especialidadeDao.searchEspecialidadesByName(nome)
    }

    // INSERÇÃO E ATUALIZAÇÃO
    suspend fun insertEspecialidade(especialidade: EspecialidadeEntity): Long {
        val especialidadeToInsert = especialidade.copy(
            syncStatus = SyncStatus.PENDING_UPLOAD,
            lastModified = System.currentTimeMillis()
        )
        return especialidadeDao.insertEspecialidade(especialidadeToInsert)
    }

    suspend fun insertEspecialidades(especialidades: List<EspecialidadeEntity>) {
        val especialidadesToInsert = especialidades.map { especialidade ->
            especialidade.copy(
                syncStatus = SyncStatus.PENDING_UPLOAD,
                lastModified = System.currentTimeMillis()
            )
        }
        especialidadeDao.insertEspecialidades(especialidadesToInsert)
    }

    suspend fun updateEspecialidade(especialidade: EspecialidadeEntity) {
        val especialidadeToUpdate = especialidade.copy(
            syncStatus = SyncStatus.PENDING_UPLOAD,
            lastModified = System.currentTimeMillis()
        )
        especialidadeDao.updateEspecialidade(especialidadeToUpdate)
    }

    suspend fun insertOrUpdateEspecialidade(especialidade: EspecialidadeEntity) {
        val especialidadeToSave = especialidade.copy(
            syncStatus = if (especialidade.serverId == null) SyncStatus.PENDING_UPLOAD else SyncStatus.SYNCED,
            lastModified = System.currentTimeMillis()
        )
        especialidadeDao.insertOrUpdateEspecialidade(especialidadeToSave)
    }

    // EXCLUSÃO
    suspend fun deleteEspecialidade(id: Long) {
        especialidadeDao.markAsDeleted(
            id = id,
            status = SyncStatus.PENDING_DELETE,
            timestamp = System.currentTimeMillis()
        )
    }

    suspend fun deleteEspecialidadePermanently(id: Long) {
        especialidadeDao.deleteEspecialidadePermanently(id)
    }

    // SINCRONIZAÇÃO
    suspend fun getPendingSync(): List<EspecialidadeEntity> {
        return especialidadeDao.getPendingSync()
    }

    suspend fun getPendingDeletions(): List<EspecialidadeEntity> {
        return especialidadeDao.getPendingDeletions()
    }

    suspend fun markAsSynced(especialidade: EspecialidadeEntity, serverId: Long? = null) {
        val updatedEspecialidade = especialidade.copy(
            serverId = serverId ?: especialidade.serverId,
            syncStatus = SyncStatus.SYNCED,
            lastModified = System.currentTimeMillis()
        )
        especialidadeDao.updateEspecialidade(updatedEspecialidade)
    }

    suspend fun markAsSyncFailed(especialidade: EspecialidadeEntity) {
        val updatedEspecialidade = especialidade.copy(
            syncStatus = SyncStatus.PENDING_UPLOAD, // Use um valor que existe no seu enum
            lastModified = System.currentTimeMillis()
        )
        especialidadeDao.updateEspecialidade(updatedEspecialidade)
    }

    // MÉTODOS AUXILIARES
    suspend fun especialidadeExists(nome: String): Boolean {
        return getEspecialidadeByName(nome) != null
    }

    suspend fun getEspecialidadeCount(): Int {
        return getAllEspecialidades().let { flow ->
            // Para contar, você precisaria coletar o flow
            // Alternativamente, adicione um método específico no DAO
            0 // Placeholder - implemente conforme necessário
        }
    }
}