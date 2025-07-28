package com.example.projeto_ibg3.data.local.database.dao

import androidx.room.*
import com.example.projeto_ibg3.data.local.database.entities.PacienteEntity
import com.example.projeto_ibg3.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface PacienteDao {
    // ========== CONSULTAS BÁSICAS ==========

    @Query("SELECT * FROM pacientes WHERE local_id = :localId AND is_deleted = 0")
    suspend fun getPacienteById(localId: String): PacienteEntity?

    @Query("SELECT * FROM pacientes WHERE is_deleted = 0 ORDER BY nome ASC")
    fun getAllPacientes(): Flow<List<PacienteEntity>>

    @Query("SELECT * FROM pacientes WHERE local_id = :localId AND is_deleted = 0")
    suspend fun getPacienteByLocalId(localId: String): PacienteEntity?

    @Query("SELECT * FROM pacientes WHERE server_id = :serverId AND is_deleted = 0")
    suspend fun getPacienteByServerId(serverId: Long): PacienteEntity?

    @Query("SELECT * FROM pacientes WHERE cpf = :cpf AND is_deleted = 0")
    suspend fun getPacienteByCpf(cpf: String): PacienteEntity?

    @Query("SELECT * FROM pacientes WHERE sus = :sus AND is_deleted = 0")
    suspend fun getPacienteBySus(sus: String?): PacienteEntity?

    @Query("SELECT * FROM pacientes WHERE is_deleted = 0 ORDER BY nome ASC")
    suspend fun getAllPacientesList(): List<PacienteEntity>

    // ========== BUSCA AVANÇADA ==========

    @Query("""
        SELECT * FROM pacientes 
        WHERE is_deleted = 0 AND (
            nome LIKE '%' || :query || '%' OR
            nome_da_mae LIKE '%' || :query || '%' OR
            cpf LIKE '%' || :query || '%' OR
            sus LIKE '%' || :query || '%' OR
            telefone LIKE '%' || :query || '%'
        )
        ORDER BY nome ASC
    """)
    fun searchPacientes(query: String): Flow<List<PacienteEntity>>

    // ========== INSERÇÕES E ATUALIZAÇÕES ==========

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPaciente(paciente: PacienteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPacienteAndGetLocalId(paciente: PacienteEntity): String {
        insertPaciente(paciente)
        return paciente.localId
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPacientes(pacientes: List<PacienteEntity>)

    @Update
    suspend fun updatePaciente(paciente: PacienteEntity)

    // ========== SINCRONIZAÇÃO - MÉTODOS PRINCIPAIS ==========

    // Buscar itens que precisam de sincronização
    @Query("""
        SELECT * FROM pacientes 
        WHERE sync_status IN (:pendingUpload, :pendingDelete, :uploadFailed, :deleteFailed, :conflict)
        ORDER BY created_at ASC
    """)
    suspend fun getItemsNeedingSync(
        pendingUpload: SyncStatus = SyncStatus.PENDING_UPLOAD,
        pendingDelete: SyncStatus = SyncStatus.PENDING_DELETE,
        uploadFailed: SyncStatus = SyncStatus.UPLOAD_FAILED,
        deleteFailed: SyncStatus = SyncStatus.DELETE_FAILED,
        conflict: SyncStatus = SyncStatus.CONFLICT
    ): List<PacienteEntity>

    // Buscar apenas itens pendentes para upload
    @Query("SELECT * FROM pacientes WHERE sync_status = :status AND is_deleted = 0")
    suspend fun getPendingUpload(status: SyncStatus = SyncStatus.PENDING_UPLOAD): List<PacienteEntity>

    // Buscar apenas itens pendentes para deleção
    @Query("SELECT * FROM pacientes WHERE sync_status = :status AND is_deleted = 1")
    suspend fun getPendingDeletion(status: SyncStatus = SyncStatus.PENDING_DELETE): List<PacienteEntity>

    // Buscar itens que falharam na sincronização
    @Query("SELECT * FROM pacientes WHERE sync_status IN (:uploadFailed, :deleteFailed)")
    suspend fun getFailedSyncItems(
        uploadFailed: SyncStatus = SyncStatus.UPLOAD_FAILED,
        deleteFailed: SyncStatus = SyncStatus.DELETE_FAILED
    ): List<PacienteEntity>

    // Buscar itens com conflito
    @Query("SELECT * FROM pacientes WHERE sync_status = :status")
    suspend fun getConflictItems(status: SyncStatus = SyncStatus.CONFLICT): List<PacienteEntity>

    // ========== ATUALIZAÇÕES DE STATUS ==========

    @Query("UPDATE pacientes SET sync_status = :status, updated_at = :timestamp WHERE local_id = :localId")
    suspend fun updateSyncStatus(localId: String, status: SyncStatus, timestamp: Long = System.currentTimeMillis())

    @Query("""
        UPDATE pacientes 
        SET sync_status = :status, 
            server_id = :serverId, 
            last_sync_timestamp = :timestamp,
            sync_attempts = 0,
            sync_error = NULL,
            updated_at = :timestamp
        WHERE local_id = :localId
    """)
    suspend fun updateSyncStatusAndServerId(
        localId: String,
        status: SyncStatus,
        serverId: Long,
        timestamp: Long = System.currentTimeMillis()
    )

    // Atualizar contador de tentativas
    @Query("""
        UPDATE pacientes 
        SET sync_attempts = sync_attempts + 1,
            last_sync_attempt = :timestamp,
            sync_error = :error
        WHERE local_id = :localId
    """)
    suspend fun incrementSyncAttempts(localId: String, timestamp: Long, error: String?)

    // Marcar como conflito
    @Query("""
        UPDATE pacientes 
        SET sync_status = :status,
            conflict_data = :conflictData,
            updated_at = :timestamp
        WHERE local_id = :localId
    """)
    suspend fun markAsConflict(
        localId: String,
        conflictData: String,
        status: SyncStatus = SyncStatus.CONFLICT,
        timestamp: Long = System.currentTimeMillis()
    )

    // ========== MARCAÇÃO PARA DELEÇÃO ==========

    @Query("""
        UPDATE pacientes 
        SET is_deleted = 1, 
            sync_status = :status, 
            updated_at = :timestamp 
        WHERE local_id = :localId
    """)
    suspend fun markAsDeleted(
        localId: String,
        status: SyncStatus = SyncStatus.PENDING_DELETE,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("DELETE FROM pacientes WHERE local_id = :localId")
    suspend fun deletePacientePermanently(localId: String)

    // ========== ESTATÍSTICAS ==========

    @Query("SELECT COUNT(*) FROM pacientes WHERE is_deleted = 0")
    suspend fun getTotalPacientes(): Int

    @Query("SELECT COUNT(*) FROM pacientes WHERE sync_status != :status")
    suspend fun getUnsyncedCount(status: SyncStatus = SyncStatus.SYNCED): Int

    @Query("SELECT COUNT(*) FROM pacientes WHERE sync_status = :status")
    suspend fun getPendingSyncCount(status: SyncStatus = SyncStatus.PENDING_UPLOAD): Int

    @Query("SELECT COUNT(*) FROM pacientes WHERE sync_status IN (:uploadFailed, :deleteFailed)")
    suspend fun getFailedSyncCount(
        uploadFailed: SyncStatus = SyncStatus.UPLOAD_FAILED,
        deleteFailed: SyncStatus = SyncStatus.DELETE_FAILED
    ): Int

    @Query("SELECT COUNT(*) FROM pacientes WHERE sync_status = :status")
    suspend fun getConflictCount(status: SyncStatus = SyncStatus.CONFLICT): Int

    // ========== VERIFICAÇÃO DE DUPLICATAS ==========

    @Query("SELECT COUNT(*) FROM pacientes WHERE cpf = :cpf AND is_deleted = 0 AND local_id != :excludeId")
    suspend fun countPacientesByCpfExcluding(cpf: String, excludeId: String?): Int

    @Query("SELECT COUNT(*) FROM pacientes WHERE sus = :sus AND is_deleted = 0 AND local_id != :excludeId")
    suspend fun countPacientesBySusExcluding(sus: String?, excludeId: String?): Int

    // ========== LIMPEZA DE DADOS ==========

    @Query("DELETE FROM pacientes WHERE is_deleted = 1 AND updated_at < :cutoffTime")
    suspend fun cleanupOldDeletedPacientes(cutoffTime: Long)

    @Query("DELETE FROM pacientes WHERE sync_status = :status AND is_deleted = 1")
    suspend fun cleanupSyncedDeletedPacientes(status: SyncStatus = SyncStatus.SYNCED)

    // ========== MÉTODOS AUXILIARES PARA SINCRONIZAÇÃO ==========

    @Query("SELECT MAX(last_sync_timestamp) FROM pacientes WHERE sync_status = :status")
    suspend fun getLastSyncTimestamp(status: SyncStatus = SyncStatus.SYNCED): Long?

    // Limpar tentativas de sincronização após sucesso
    @Query("""
        UPDATE pacientes 
        SET sync_attempts = 0, 
            sync_error = NULL,
            last_sync_attempt = 0
        WHERE sync_status = :status
    """)
    suspend fun clearSyncAttempts(status: SyncStatus = SyncStatus.SYNCED)

    // Buscar itens que excederam o limite de tentativas
    @Query("SELECT * FROM pacientes WHERE sync_attempts >= :maxAttempts AND sync_status IN (:uploadFailed, :deleteFailed)")
    suspend fun getItemsExceededMaxAttempts(
        maxAttempts: Int = 3,
        uploadFailed: SyncStatus = SyncStatus.UPLOAD_FAILED,
        deleteFailed: SyncStatus = SyncStatus.DELETE_FAILED
    ): List<PacienteEntity>

    //Atualiza o status de sincronização de um paciente pelo localId
    @Query("UPDATE pacientes SET sync_status = :status, updated_at = :timestamp WHERE local_id = :localId")
    suspend fun updateSyncStatusByLocalId(
        localId: String,
        status: SyncStatus,
        timestamp: Long = System.currentTimeMillis()
    )

    @Query("SELECT * FROM pacientes WHERE sync_status = :status")
    suspend fun getItemsByStatus(status: SyncStatus): List<PacienteEntity>

    @Query("SELECT * FROM pacientes WHERE sync_status IN (:statuses)")
    suspend fun getItemsNeedingSync(
        statuses: List<SyncStatus> = listOf(
            SyncStatus.PENDING_UPLOAD,
            SyncStatus.PENDING_DELETE,
            SyncStatus.CONFLICT,
            SyncStatus.UPLOAD_FAILED
        )
    ): List<PacienteEntity>

}