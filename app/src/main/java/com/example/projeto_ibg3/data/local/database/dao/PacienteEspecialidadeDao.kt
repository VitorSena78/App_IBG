package com.example.projeto_ibg3.data.local.database.dao

import androidx.room.*
import com.example.projeto_ibg3.data.local.database.entities.EspecialidadeEntity
import com.example.projeto_ibg3.data.local.database.entities.PacienteEntity
import com.example.projeto_ibg3.data.local.database.entities.PacienteEspecialidadeEntity
import com.example.projeto_ibg3.data.local.database.relations.PacienteEspecialidadeWithDetails
import com.example.projeto_ibg3.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface PacienteEspecialidadeDao {

    // ==================== OPERAÇÕES BÁSICAS ====================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pacienteEspecialidade: PacienteEspecialidadeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pacienteEspecialidades: List<PacienteEspecialidadeEntity>)

    @Update
    suspend fun update(pacienteEspecialidade: PacienteEspecialidadeEntity)

    @Delete
    suspend fun delete(pacienteEspecialidade: PacienteEspecialidadeEntity)

    // ==================== ALIASES PARA COMPATIBILIDADE ====================

    suspend fun insertPacienteEspecialidade(pacienteEspecialidade: PacienteEspecialidadeEntity) = insert(pacienteEspecialidade)

    suspend fun insertPacienteEspecialidades(pacienteEspecialidades: List<PacienteEspecialidadeEntity>) = insertAll(pacienteEspecialidades)

    suspend fun updatePacienteEspecialidade(pacienteEspecialidade: PacienteEspecialidadeEntity) = update(pacienteEspecialidade)

    suspend fun deletePacienteEspecialidade(pacienteEspecialidade: PacienteEspecialidadeEntity) = delete(pacienteEspecialidade)

    // ==================== CONSULTAS BÁSICAS ====================

    @Query("SELECT * FROM paciente_has_especialidade WHERE paciente_local_id = :pacienteLocalId AND especialidade_local_id = :especialidadeLocalId AND is_deleted = 0")
    suspend fun getById(pacienteLocalId: String, especialidadeLocalId: String): PacienteEspecialidadeEntity?

    @Query("SELECT * FROM paciente_has_especialidade WHERE is_deleted = 0")
    suspend fun getAll(): List<PacienteEspecialidadeEntity>

    @Query("SELECT * FROM paciente_has_especialidade WHERE is_deleted = 0")
    fun getAllFlow(): Flow<List<PacienteEspecialidadeEntity>>

    @Query("SELECT * FROM paciente_has_especialidade WHERE is_deleted = 0")
    fun getAllAssociations(): Flow<List<PacienteEspecialidadeEntity>>

    // Aliases para compatibilidade
    fun getAllPacienteEspecialidades(): Flow<List<PacienteEspecialidadeEntity>> = getAllFlow()

    suspend fun getPacienteEspecialidadeByIds(pacienteLocalId: String, especialidadeLocalId: String): PacienteEspecialidadeEntity? =
        getById(pacienteLocalId, especialidadeLocalId)

    suspend fun getPacienteEspecialidade(pacienteLocalId: String, especialidadeLocalId: String): PacienteEspecialidadeEntity? =
        getById(pacienteLocalId, especialidadeLocalId)

    // ==================== CONSULTAS POR PACIENTE ====================

    @Query("""
        SELECT * FROM paciente_has_especialidade 
        WHERE paciente_local_id = :pacienteLocalId 
        AND is_deleted = 0
        ORDER BY created_at ASC
    """)
    suspend fun getByPacienteId(pacienteLocalId: String): List<PacienteEspecialidadeEntity>

    @Query("""
        SELECT * FROM paciente_has_especialidade 
        WHERE paciente_local_id = :pacienteLocalId 
        AND is_deleted = 0
        ORDER BY created_at ASC
    """)
    fun getByPacienteIdFlow(pacienteLocalId: String): Flow<List<PacienteEspecialidadeEntity>>

    @Query("""
        SELECT * FROM paciente_has_especialidade 
        WHERE paciente_server_id = :pacienteServerId 
        AND is_deleted = 0
        ORDER BY created_at ASC
    """)
    suspend fun getEspecialidadesByPacienteServerId(pacienteServerId: Long): List<PacienteEspecialidadeEntity>

    @Query("""
        SELECT e.* FROM especialidades e
        INNER JOIN paciente_has_especialidade pe ON e.local_id = pe.especialidade_local_id
        WHERE pe.paciente_local_id = :pacienteLocalId AND pe.is_deleted = 0 AND e.is_deleted = 0
        ORDER BY pe.created_at ASC
    """)
    suspend fun getEspecialidadesByPacienteId(pacienteLocalId: String): List<EspecialidadeEntity>

    @Query("""
        SELECT e.* FROM especialidades e
        INNER JOIN paciente_has_especialidade pe ON e.local_id = pe.especialidade_local_id
        WHERE pe.paciente_local_id = :pacienteLocalId AND pe.is_deleted = 0 AND e.is_deleted = 0
        ORDER BY pe.created_at ASC
    """)
    fun getEspecialidadesByPacienteIdFlow(pacienteLocalId: String): Flow<List<EspecialidadeEntity>>

    // Alias para compatibilidade
    suspend fun getEspecialidadeEntitiesByPacienteId(pacienteLocalId: String): List<EspecialidadeEntity> =
        getEspecialidadesByPacienteId(pacienteLocalId)

    // ==================== CONSULTAS POR ESPECIALIDADE ====================

    @Query("""
        SELECT * FROM paciente_has_especialidade 
        WHERE especialidade_local_id = :especialidadeLocalId 
        AND is_deleted = 0
        ORDER BY created_at ASC
    """)
    suspend fun getByEspecialidadeId(especialidadeLocalId: String): List<PacienteEspecialidadeEntity>

    @Query("""
        SELECT * FROM paciente_has_especialidade 
        WHERE especialidade_local_id = :especialidadeLocalId 
        AND is_deleted = 0
        ORDER BY created_at ASC
    """)
    fun getByEspecialidadeIdFlow(especialidadeLocalId: String): Flow<List<PacienteEspecialidadeEntity>>

    @Query("""
        SELECT * FROM paciente_has_especialidade 
        WHERE especialidade_server_id = :especialidadeServerId 
        AND is_deleted = 0
        ORDER BY created_at ASC
    """)
    suspend fun getPacientesByEspecialidadeServerId(especialidadeServerId: Long): List<PacienteEspecialidadeEntity>

    @Query("""
        SELECT p.* FROM pacientes p
        INNER JOIN paciente_has_especialidade pe ON p.local_id = pe.paciente_local_id
        WHERE pe.especialidade_local_id = :especialidadeLocalId AND pe.is_deleted = 0 AND p.is_deleted = 0
        ORDER BY pe.created_at ASC
    """)
    suspend fun getPacientesByEspecialidadeId(especialidadeLocalId: String): List<PacienteEntity>

    @Query("""
        SELECT pe.* FROM paciente_has_especialidade pe
        WHERE pe.especialidade_local_id = :especialidadeLocalId AND pe.is_deleted = 0
        ORDER BY pe.created_at ASC
    """)
    fun getPacientesByEspecialidadeIdFlow(especialidadeLocalId: String): Flow<List<PacienteEspecialidadeEntity>>

    // ==================== VERIFICAÇÃO DE EXISTÊNCIA ====================

    @Query("""
        SELECT COUNT(*) FROM paciente_has_especialidade 
        WHERE paciente_local_id = :pacienteLocalId 
        AND especialidade_local_id = :especialidadeLocalId 
        AND is_deleted = 0
    """)
    suspend fun existsAssociation(pacienteLocalId: String, especialidadeLocalId: String): Int

    // ==================== OPERAÇÕES DE REMOÇÃO ====================

    @Query("DELETE FROM paciente_has_especialidade WHERE paciente_local_id = :pacienteLocalId AND especialidade_local_id = :especialidadeLocalId")
    suspend fun deleteByIds(pacienteLocalId: String, especialidadeLocalId: String)

    @Query("DELETE FROM paciente_has_especialidade WHERE paciente_local_id = :pacienteLocalId")
    suspend fun deleteByPacienteId(pacienteLocalId: String)

    @Query("DELETE FROM paciente_has_especialidade WHERE especialidade_local_id = :especialidadeLocalId")
    suspend fun deleteByEspecialidadeId(especialidadeLocalId: String)

    @Query("DELETE FROM paciente_has_especialidade WHERE paciente_local_id = :pacienteLocalId AND especialidade_local_id = :especialidadeLocalId")
    suspend fun deletePermanently(pacienteLocalId: String, especialidadeLocalId: String)

    // Aliases para compatibilidade
    suspend fun deleteAllEspecialidadesByPacienteId(pacienteLocalId: String) = deleteByPacienteId(pacienteLocalId)

    suspend fun deleteByPacienteAndEspecialidade(pacienteLocalId: String, especialidadeLocalId: String) =
        deleteByIds(pacienteLocalId, especialidadeLocalId)

    // ==================== SOFT DELETE ====================

    @Query("""
        UPDATE paciente_has_especialidade 
        SET is_deleted = 1, 
            sync_status = :status, 
            updated_at = :timestamp 
        WHERE paciente_local_id = :pacienteLocalId 
        AND especialidade_local_id = :especialidadeLocalId
    """)
    suspend fun markAsDeleted(
        pacienteLocalId: String,
        especialidadeLocalId: String,
        status: SyncStatus = SyncStatus.PENDING_DELETE,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("UPDATE paciente_has_especialidade SET is_deleted = 1, updated_at = :timestamp, sync_status = 'PENDING_UPLOAD' WHERE paciente_local_id = :pacienteLocalId AND especialidade_local_id = :especialidadeLocalId")
    suspend fun softDelete(pacienteLocalId: String, especialidadeLocalId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE paciente_has_especialidade SET is_deleted = 0, updated_at = :timestamp, sync_status = 'PENDING_UPLOAD' WHERE paciente_local_id = :pacienteLocalId AND especialidade_local_id = :especialidadeLocalId")
    suspend fun restore(pacienteLocalId: String, especialidadeLocalId: String, timestamp: Long = System.currentTimeMillis())

    // Alias para compatibilidade
    suspend fun softDeleteByPacienteAndEspecialidade(pacienteLocalId: String, especialidadeLocalId: String) =
        softDelete(pacienteLocalId, especialidadeLocalId)

    // ==================== CONSULTAS PARA SINCRONIZAÇÃO ====================

    @Query("""
        SELECT * FROM paciente_has_especialidade 
        WHERE sync_status IN (:statuses)
        ORDER BY created_at ASC
    """)
    suspend fun getItemsNeedingSync(
        statuses: List<SyncStatus> = listOf(
            SyncStatus.PENDING_UPLOAD,
            SyncStatus.PENDING_DELETE,
            SyncStatus.UPLOAD_FAILED,
            SyncStatus.DELETE_FAILED,
            SyncStatus.CONFLICT
        )
    ): List<PacienteEspecialidadeEntity>

    @Query("SELECT * FROM paciente_has_especialidade WHERE sync_status = :status")
    suspend fun getItemsByStatus(status: SyncStatus): List<PacienteEspecialidadeEntity>

    @Query("SELECT * FROM paciente_has_especialidade WHERE sync_status = :status")
    suspend fun getByStatus(status: SyncStatus): List<PacienteEspecialidadeEntity>

    @Query("SELECT * FROM paciente_has_especialidade WHERE sync_status = :status")
    fun getByStatusFlow(status: SyncStatus): Flow<List<PacienteEspecialidadeEntity>>

    @Query("SELECT * FROM paciente_has_especialidade WHERE sync_status IN ('PENDING_UPLOAD', 'PENDING_DELETE', 'CONFLICT')")
    suspend fun getPendingSync(): List<PacienteEspecialidadeEntity>

    @Query("SELECT * FROM paciente_has_especialidade WHERE sync_status = 'PENDING_UPLOAD' OR sync_status = 'PENDING_DELETE'")
    suspend fun getPendingSyncUpload(): List<PacienteEspecialidadeEntity>

    @Query("SELECT * FROM paciente_has_especialidade WHERE sync_status = 'PENDING_UPLOAD'")
    suspend fun getPendingUpload(): List<PacienteEspecialidadeEntity>

    @Query("SELECT * FROM paciente_has_especialidade WHERE sync_status = 'CONFLICT'")
    suspend fun getConflicts(): List<PacienteEspecialidadeEntity>

    @Query("SELECT * FROM paciente_has_especialidade WHERE device_id = :deviceId")
    suspend fun getByDeviceId(deviceId: String): List<PacienteEspecialidadeEntity>

    @Query("SELECT * FROM paciente_has_especialidade WHERE sync_attempts >= :maxAttempts")
    suspend fun getFailedSyncs(maxAttempts: Int = 3): List<PacienteEspecialidadeEntity>

    // Aliases para compatibilidade
    suspend fun getPacienteEspecialidadeByStatus(status: String): List<PacienteEspecialidadeEntity> =
        getByStatus(SyncStatus.valueOf(status))

    suspend fun getPacienteEspecialidades(status: SyncStatus): List<PacienteEspecialidadeEntity> =
        getByStatus(status)

    // ==================== OPERAÇÕES DE SINCRONIZAÇÃO ====================

    @Query("""
        UPDATE paciente_has_especialidade 
        SET sync_status = :status, 
            updated_at = :timestamp 
        WHERE paciente_local_id = :pacienteLocalId 
        AND especialidade_local_id = :especialidadeLocalId
    """)
    suspend fun updateSyncStatus(
        pacienteLocalId: String,
        especialidadeLocalId: String,
        status: SyncStatus,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("""
        UPDATE paciente_has_especialidade 
        SET sync_status = :status, 
            paciente_server_id = :pacienteServerId,
            especialidade_server_id = :especialidadeServerId,
            last_sync_timestamp = :timestamp,
            sync_attempts = 0,
            sync_error = NULL,
            updated_at = :timestamp
        WHERE paciente_local_id = :pacienteLocalId 
        AND especialidade_local_id = :especialidadeLocalId
    """)
    suspend fun updateSyncStatusWithServerIds(
        pacienteLocalId: String,
        especialidadeLocalId: String,
        status: SyncStatus,
        pacienteServerId: Long?,
        especialidadeServerId: Long?,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("UPDATE paciente_has_especialidade SET sync_status = :status, last_sync_timestamp = :timestamp WHERE paciente_local_id = :pacienteLocalId AND especialidade_local_id = :especialidadeLocalId")
    suspend fun updateSyncStatusWithTimestamp(pacienteLocalId: String, especialidadeLocalId: String, status: SyncStatus, timestamp: Long)

    @Query("UPDATE paciente_has_especialidade SET paciente_server_id = :pacienteServerId, especialidade_server_id = :especialidadeServerId, sync_status = 'SYNCED', last_sync_timestamp = :timestamp WHERE paciente_local_id = :pacienteLocalId AND especialidade_local_id = :especialidadeLocalId")
    suspend fun updateServerIds(
        pacienteLocalId: String,
        especialidadeLocalId: String,
        pacienteServerId: Long?,
        especialidadeServerId: Long?,
        timestamp: Long
    )

    @Query("UPDATE paciente_has_especialidade SET sync_attempts = sync_attempts + 1, last_sync_attempt = :timestamp, sync_error = :error WHERE paciente_local_id = :pacienteLocalId AND especialidade_local_id = :especialidadeLocalId")
    suspend fun incrementSyncAttempts(pacienteLocalId: String, especialidadeLocalId: String, timestamp: Long, error: String?)

    @Query("UPDATE paciente_has_especialidade SET sync_attempts = 0, sync_error = NULL WHERE paciente_local_id = :pacienteLocalId AND especialidade_local_id = :especialidadeLocalId")
    suspend fun resetSyncAttempts(pacienteLocalId: String, especialidadeLocalId: String)

    // ==================== CONSULTAS COM DETALHES COMPLETOS ====================

    @Transaction
    @Query("""
        SELECT pe.* 
        FROM paciente_has_especialidade pe
        WHERE pe.is_deleted = 0 
    """)
    suspend fun getAllWithDetails(): List<PacienteEspecialidadeWithDetails>

    @Transaction
    @Query("""
        SELECT pe.* 
        FROM paciente_has_especialidade pe
        WHERE pe.paciente_local_id = :pacienteLocalId AND pe.is_deleted = 0 
    """)
    suspend fun getWithDetailsByPacienteId(pacienteLocalId: String): List<PacienteEspecialidadeWithDetails>

    @Transaction
    @Query("""
        SELECT pe.* 
        FROM paciente_has_especialidade pe
        WHERE pe.especialidade_local_id = :especialidadeLocalId AND pe.is_deleted = 0 
    """)
    suspend fun getWithDetailsByEspecialidadeId(especialidadeLocalId: String): List<PacienteEspecialidadeWithDetails>

    @Transaction
    @Query("SELECT * FROM paciente_has_especialidade WHERE paciente_local_id = :pacienteLocalId")
    suspend fun getPacienteEspecialidadeWithDetailsByPacienteId(pacienteLocalId: String): List<PacienteEspecialidadeWithDetails>

    @Transaction
    @Query("SELECT * FROM paciente_has_especialidade WHERE especialidade_local_id = :especialidadeLocalId")
    suspend fun getPacienteEspecialidadeWithDetailsByEspecialidadeId(especialidadeLocalId: String): List<PacienteEspecialidadeWithDetails>

    @Transaction
    @Query("SELECT * FROM paciente_has_especialidade")
    suspend fun getAllPacienteEspecialidadeWithDetails(): List<PacienteEspecialidadeWithDetails>

    @Transaction
    @Query("SELECT * FROM paciente_has_especialidade WHERE (paciente_local_id || '_' || especialidade_local_id) = :relationId")
    suspend fun getPacienteEspecialidadeWithDetailsById(relationId: String): PacienteEspecialidadeWithDetails?

    // ==================== CONSULTAS POR DATA ====================

    @Query("SELECT * FROM paciente_has_especialidade WHERE data_atendimento BETWEEN :startDate AND :endDate AND is_deleted = 0")
    suspend fun getByDataRange(startDate: Long, endDate: Long): List<PacienteEspecialidadeEntity>

    @Query("SELECT * FROM paciente_has_especialidade WHERE data_atendimento >= :date AND is_deleted = 0")
    suspend fun getFromDate(date: Long): List<PacienteEspecialidadeEntity>

    @Transaction
    @Query("""
        SELECT pe.* 
        FROM paciente_has_especialidade pe
        WHERE pe.data_atendimento BETWEEN :startDate AND :endDate AND pe.is_deleted = 0
        ORDER BY pe.data_atendimento DESC 
    """)
    suspend fun getWithDetailsByDataRange(startDate: Long, endDate: Long): List<PacienteEspecialidadeWithDetails>

    // Alias para compatibilidade
    suspend fun getAtendimentosByDateRange(startDate: Long, endDate: Long): List<PacienteEspecialidadeEntity> =
        getByDataRange(startDate, endDate)

    // ==================== ESTATÍSTICAS ====================

    @Query("SELECT COUNT(*) FROM paciente_has_especialidade WHERE is_deleted = 0")
    suspend fun getTotalAssociations(): Int

    @Query("SELECT COUNT(*) FROM paciente_has_especialidade WHERE sync_status != :status")
    suspend fun getUnsyncedCount(status: SyncStatus = SyncStatus.SYNCED): Int

    @Query("SELECT COUNT(*) FROM paciente_has_especialidade WHERE sync_status = 'PENDING_UPLOAD'")
    suspend fun countPendingUploads(): Int

    @Query("SELECT COUNT(*) FROM paciente_has_especialidade WHERE sync_status = 'CONFLICT'")
    suspend fun countConflicts(): Int

    @Query("SELECT COUNT(*) FROM paciente_has_especialidade WHERE especialidade_local_id = :especialidadeLocalId AND is_deleted = 0")
    suspend fun getAtendimentosCountByEspecialidade(especialidadeLocalId: String): Int

    // ==================== LIMPEZA E MANUTENÇÃO ====================

    @Query("DELETE FROM paciente_has_especialidade WHERE is_deleted = 1 AND updated_at < :cutoffTime")
    suspend fun cleanupOldDeleted(cutoffTime: Long)

    @Query("DELETE FROM paciente_has_especialidade WHERE sync_status = :status AND is_deleted = 1")
    suspend fun cleanupSyncedDeleted(status: SyncStatus = SyncStatus.SYNCED)

    @Query("DELETE FROM paciente_has_especialidade WHERE is_deleted = 1 AND sync_status = 'SYNCED'")
    suspend fun cleanupDeletedSynced()

    @Query("DELETE FROM paciente_has_especialidade WHERE sync_status = 'SYNCED' AND last_sync_timestamp < :olderThan")
    suspend fun cleanupOldSynced(olderThan: Long)

    // ==================== OPERAÇÕES TRANSACIONAIS ====================

    @Transaction
    suspend fun insertOrUpdateRelation(pacienteEspecialidade: PacienteEspecialidadeEntity) {
        val existing = getById(pacienteEspecialidade.pacienteLocalId, pacienteEspecialidade.especialidadeLocalId)
        if (existing != null) {
            update(pacienteEspecialidade.copy(
                version = existing.version + 1,
                updatedAt = System.currentTimeMillis()
            ))
        } else {
            insert(pacienteEspecialidade)
        }
    }

    @Transaction
    suspend fun deleteRelation(pacienteLocalId: String, especialidadeLocalId: String) {
        softDelete(pacienteLocalId, especialidadeLocalId)
    }

    @Query("SELECT * FROM pacientes WHERE server_id = :serverId AND is_deleted = 0 LIMIT 1")
    suspend fun getPacienteByServerId(serverId: Long): PacienteEntity?

    @Query("SELECT * FROM pacientes WHERE is_deleted = 0 ORDER BY nome ASC")
    suspend fun getAllPacientesList(): List<PacienteEntity>

    // Se não existir, adicione também:
    @Query("SELECT * FROM pacientes WHERE cpf = :cpf AND is_deleted = 0 LIMIT 1")
    suspend fun getPacienteByCpf(cpf: String): PacienteEntity?
}