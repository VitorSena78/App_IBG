package com.example.projeto_ibg3.data.repository.impl

import com.example.projeto_ibg3.data.local.database.dao.PacienteEspecialidadeDao
import com.example.projeto_ibg3.data.mappers.*
import com.example.projeto_ibg3.data.mappers.toDomainModel
import com.example.projeto_ibg3.domain.repository.PacienteEspecialidadeRepository
import com.example.projeto_ibg3.domain.model.Especialidade
import com.example.projeto_ibg3.domain.model.PacienteEspecialidade
import com.example.projeto_ibg3.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PacienteEspecialidadeRepositoryImpl @Inject constructor(
    private val pacienteEspecialidadeDao: PacienteEspecialidadeDao
) : PacienteEspecialidadeRepository {

    // ==================== FUNÇÕES BÁSICAS ====================

    override fun getAllPacienteEspecialidades(): Flow<List<PacienteEspecialidade>> {
        return pacienteEspecialidadeDao.getAllPacienteEspecialidades().map { entities ->
            entities.map { it.toPacienteEspecialidade() }
        }
    }

    override suspend fun getEspecialidadesByPacienteId(pacienteLocalId: String): List<Especialidade> {
        return pacienteEspecialidadeDao.getEspecialidadesByPacienteId(pacienteLocalId)
            .map { it.toDomainModel() }
    }

    override suspend fun getPacientesByEspecialidadeId(especialidadeLocalId: String): List<PacienteEspecialidade> {
        return pacienteEspecialidadeDao.getByEspecialidadeId(especialidadeLocalId)
            .map { it.toPacienteEspecialidade() }
    }

    override suspend fun getPacienteEspecialidade(pacienteLocalId: String, especialidadeLocalId: String): PacienteEspecialidade? {
        return pacienteEspecialidadeDao.getPacienteEspecialidade(
            pacienteLocalId,
            especialidadeLocalId
        )?.toPacienteEspecialidade()
    }

    // ==================== OPERAÇÕES DE CRUD ====================

    override suspend fun insertPacienteEspecialidade(pacienteEspecialidade: PacienteEspecialidade) {
        pacienteEspecialidadeDao.insertPacienteEspecialidade(pacienteEspecialidade.toEntity())
    }

    override suspend fun insertPacienteEspecialidades(pacienteEspecialidades: List<PacienteEspecialidade>) {
        val entities = pacienteEspecialidades.map { it.toEntity() }
        pacienteEspecialidadeDao.insertPacienteEspecialidades(entities)
    }

    override suspend fun updatePacienteEspecialidade(pacienteEspecialidade: PacienteEspecialidade) {
        pacienteEspecialidadeDao.updatePacienteEspecialidade(pacienteEspecialidade.toEntity())
    }

    override suspend fun deletePacienteEspecialidade(pacienteEspecialidade: PacienteEspecialidade) {
        pacienteEspecialidadeDao.deletePacienteEspecialidade(pacienteEspecialidade.toEntity())
    }

    override suspend fun deleteByIds(pacienteLocalId: String, especialidadeLocalId: String) {
        pacienteEspecialidadeDao.deleteByIds(pacienteLocalId, especialidadeLocalId)
    }

    override suspend fun deleteAllEspecialidadesByPacienteId(pacienteLocalId: String) {
        pacienteEspecialidadeDao.deleteAllEspecialidadesByPacienteId(pacienteLocalId)
    }

    // ==================== MÉTODOS PARA O VIEWMODEL ====================

    override suspend fun addEspecialidadeToPaciente(pacienteLocalId: String, especialidadeLocalId: String) {
        val pacienteEspecialidade = PacienteEspecialidade(
            pacienteLocalId = pacienteLocalId,
            especialidadeLocalId = especialidadeLocalId,
            dataAtendimento = null,
            syncStatus = SyncStatus.PENDING_UPLOAD
        )
        insertPacienteEspecialidade(pacienteEspecialidade)
    }

    override suspend fun removeEspecialidadeFromPaciente(pacienteLocalId: String, especialidadeLocalId: String) {
        deleteByIds(pacienteLocalId, especialidadeLocalId)
    }

    // ==================== CONSULTAS POR DATA ====================

    override suspend fun getAtendimentosByDateRange(dataInicio: String, dataFim: String): List<PacienteEspecialidade> {
        val startTimestamp = convertDateStringToTimestamp(dataInicio)
        val endTimestamp = convertDateStringToTimestamp(dataFim)

        return pacienteEspecialidadeDao.getAtendimentosByDateRange(startTimestamp, endTimestamp)
            .map { it.toPacienteEspecialidade() }
    }

    override suspend fun getAtendimentosCountByEspecialidade(especialidadeLocalId: String): Int {
        return pacienteEspecialidadeDao.getAtendimentosCountByEspecialidade(especialidadeLocalId)
    }

    // ==================== SINCRONIZAÇÃO ====================

    override suspend fun syncPacienteEspecialidades(pacienteLocalId: String, especialidadeIds: List<String>) {
        deleteAllEspecialidadesByPacienteId(pacienteLocalId)

        val novasAssociacoes = especialidadeIds.map { especialidadeId ->
            PacienteEspecialidade(
                pacienteLocalId = pacienteLocalId,
                especialidadeLocalId = especialidadeId,
                dataAtendimento = null
            )
        }

        if (novasAssociacoes.isNotEmpty()) {
            insertPacienteEspecialidades(novasAssociacoes)
        }
    }

    override suspend fun getPendingSync(): List<PacienteEspecialidade> {
        return pacienteEspecialidadeDao.getPendingUpload()
            .map { it.toPacienteEspecialidade() }
    }

    override suspend fun getConflicts(): List<PacienteEspecialidade> {
        return pacienteEspecialidadeDao.getConflicts()
            .map { it.toPacienteEspecialidade() }
    }

    override suspend fun markAsSynced(pacienteLocalId: String, especialidadeLocalId: String) {
        val timestamp = System.currentTimeMillis()
        pacienteEspecialidadeDao.updateSyncStatusWithTimestamp(
            pacienteLocalId,
            especialidadeLocalId,
            SyncStatus.SYNCED,
            timestamp
        )
    }

    override suspend fun markAsConflict(pacienteLocalId: String, especialidadeLocalId: String) {
        pacienteEspecialidadeDao.updateSyncStatus(
            pacienteLocalId,
            especialidadeLocalId,
            SyncStatus.CONFLICT
        )
    }

    // ==================== FUNÇÕES AUXILIARES ====================

    private fun convertDateStringToTimestamp(dateString: String): Long {
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            sdf.parse(dateString)?.time ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    override suspend fun softDeletePacienteEspecialidade(pacienteLocalId: String, especialidadeLocalId: String) {
        val timestamp = System.currentTimeMillis()
        pacienteEspecialidadeDao.softDelete(pacienteLocalId, especialidadeLocalId, timestamp)
    }

    override suspend fun restorePacienteEspecialidade(pacienteLocalId: String, especialidadeLocalId: String) {
        val timestamp = System.currentTimeMillis()
        pacienteEspecialidadeDao.restore(pacienteLocalId, especialidadeLocalId, timestamp)
    }

    // ==================== ESTATÍSTICAS ====================

    override suspend fun countPendingUploads(): Int {
        return pacienteEspecialidadeDao.countPendingUploads()
    }

    override suspend fun countConflicts(): Int {
        return pacienteEspecialidadeDao.countConflicts()
    }

    // ==================== LIMPEZA ====================

    override suspend fun cleanupDeletedSynced() {
        pacienteEspecialidadeDao.cleanupDeletedSynced()
    }

    override suspend fun cleanupOldSynced(daysOld: Int) {
        val cutoffTime = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)
        pacienteEspecialidadeDao.cleanupOldSynced(cutoffTime)
    }
}