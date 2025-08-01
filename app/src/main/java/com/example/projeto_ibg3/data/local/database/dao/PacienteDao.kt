package com.example.projeto_ibg3.data.local.database.dao

import androidx.room.*
import com.example.projeto_ibg3.data.local.database.entities.PacienteEntity
import com.example.projeto_ibg3.domain.model.SyncStatus
import com.example.projeto_ibg3.domain.model.SyncStatusCount
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

    // ==================== MÉTODOS PARA SINCRONIZAÇÃO (podem estar faltando) ====================

    /**
     * Busca pacientes por status específico
     */
    @Query("SELECT * FROM pacientes WHERE sync_status = :syncStatus")
    suspend fun getPacientesBySyncStatus(syncStatus: SyncStatus): List<PacienteEntity>

    /**
     * CRÍTICO: Pacientes novos (sem serverId)
     */
    @Query("SELECT * FROM pacientes WHERE server_id IS NULL AND sync_status = :syncStatus")
    suspend fun getNovosPacientes(syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD): List<PacienteEntity>

    /**
     * CRÍTICO: Pacientes para atualizar (com serverId)
     */
    @Query("SELECT * FROM pacientes WHERE server_id IS NOT NULL AND sync_status = :syncStatus")
    suspend fun getPacientesParaAtualizar(syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD): List<PacienteEntity>


    // ==================== MÉTODOS ADICIONAIS ÚTEIS ====================

    /**
     * Buscar pacientes que falharam na sincronização
     */
    @Query("SELECT * FROM pacientes WHERE sync_status IN (:failedStatuses)")
    suspend fun getPacientesComFalha(
        failedStatuses: List<SyncStatus> = listOf(
            SyncStatus.UPLOAD_FAILED,
            SyncStatus.DELETE_FAILED
        )
    ): List<PacienteEntity>

    /**
     * Marcar para deleção (soft delete)
     */
    @Query("UPDATE pacientes SET is_deleted = 1, sync_status = :syncStatus, updated_at = :updatedAt WHERE local_id = :localId")
    suspend fun markForDeletion(
        localId: String,
        syncStatus: SyncStatus = SyncStatus.PENDING_DELETE,
        updatedAt: Long = System.currentTimeMillis()
    )

    // ==================== MÉTODOS BATCH PARA OTIMIZAÇÃO ====================

    /**
     * Atualização em batch de status de sincronização e server_id
     * Usado após criar pacientes no servidor
     */
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
    suspend fun updateSyncStatusAndServerIdSingle(
        localId: String,
        status: SyncStatus,
        serverId: Long,
        timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Atualização em batch de múltiplos pacientes com seus server_ids
     * Triple: localId, syncStatus, serverId
     */
    suspend fun batchUpdateSyncStatusAndServerId(updates: List<Triple<String, SyncStatus, Long>>) {
        updates.forEach { (localId, status, serverId) ->
            updateSyncStatusAndServerIdSingle(localId, status, serverId)
        }
    }

    /**
     * Atualização em batch apenas do status de sincronização
     */
    suspend fun batchUpdateSyncStatus(localIds: List<String>, status: SyncStatus) {
        localIds.forEach { localId ->
            updateSyncStatus(localId, status)
        }
    }

    /**
     * Incremento em batch de tentativas de sincronização
     * Triple: localId, timestamp, error
     */
    suspend fun batchIncrementSyncAttempts(attempts: List<Triple<String, Long, String?>>) {
        attempts.forEach { (localId, timestamp, error) ->
            incrementSyncAttempts(localId, timestamp, error)
        }
    }

    /**
     * Atualização de múltiplos pacientes
     */
    suspend fun updatePacientes(pacientes: List<PacienteEntity>) {
        pacientes.forEach { paciente ->
            updatePaciente(paciente)
        }
    }

// ==================== MÉTODOS AUXILIARES PARA ESPECIALIDADES ====================

    /**
     * Buscar fichas de múltiplas especialidades de uma vez
     */
    @Query("SELECT local_id, fichas FROM especialidades WHERE local_id IN (:especialidadeIds)")
    suspend fun getFichasByIdsFromEspecialidadesRaw(especialidadeIds: List<String>): List<FichaInfo>


// ==================== BUSCA OTIMIZADA POR SERVIDOR ====================

    /**
     * Buscar pacientes por múltiplos server_ids
     */
    @Query("SELECT * FROM pacientes WHERE server_id IN (:serverIds) AND is_deleted = 0")
    suspend fun getPacientesByServerIds(serverIds: List<Long>): List<PacienteEntity>

    /**
     * Buscar pacientes por múltiplos CPFs
     */
    @Query("SELECT * FROM pacientes WHERE cpf IN (:cpfs) AND is_deleted = 0")
    suspend fun getPacientesByCpfs(cpfs: List<String>): List<PacienteEntity>

// ==================== MÉTODOS DE LIMPEZA E MANUTENÇÃO ====================

    /**
     * Reset de tentativas de sincronização para itens que tiveram sucesso
     */
    @Query("""
    UPDATE pacientes 
    SET sync_attempts = 0, 
        sync_error = NULL, 
        last_sync_attempt = 0 
    WHERE sync_status = :status
""")
    suspend fun resetSyncAttemptsForStatus(status: SyncStatus = SyncStatus.SYNCED)

    /**
     * Limpeza de pacientes com muitas tentativas falhadas
     */
    @Query("""
    UPDATE pacientes 
    SET sync_status = :newStatus 
    WHERE sync_attempts >= :maxAttempts 
    AND sync_status IN (:failedStatuses)
""")
    suspend fun markFailedAttemptsAsAbandoned(
        maxAttempts: Int = 5,
        newStatus: SyncStatus = SyncStatus.CONFLICT,
        failedStatuses: List<SyncStatus> = listOf(
            SyncStatus.UPLOAD_FAILED,
            SyncStatus.DELETE_FAILED
        )
    )

// ==================== ESTATÍSTICAS AVANÇADAS ====================

    /**
     * Contagem de pacientes por status de sincronização
     */
    @Query("SELECT sync_status, COUNT(*) as count FROM pacientes WHERE is_deleted = 0 GROUP BY sync_status")
    suspend fun getSyncStatusCountsRaw(): List<SyncStatusCount>

    // Método auxiliar para converter em Map
    suspend fun getSyncStatusCounts(): Map<SyncStatus, Int> {
        return getSyncStatusCountsRaw().associate { it.syncStatus to it.count }
    }

    /**
     * Pacientes que falharam mais de X vezes
     */
    @Query("""
    SELECT * FROM pacientes 
    WHERE sync_attempts > :threshold 
    AND sync_status IN (:failedStatuses)
    ORDER BY sync_attempts DESC
""")
    suspend fun getPacientesWithHighFailureRate(
        threshold: Int = 3,
        failedStatuses: List<SyncStatus> = listOf(
            SyncStatus.UPLOAD_FAILED,
            SyncStatus.DELETE_FAILED
        )
    ): List<PacienteEntity>

    /**
     * Últimos pacientes modificados
     */
    @Query("""
    SELECT * FROM pacientes 
    WHERE is_deleted = 0 
    AND updated_at > :since 
    ORDER BY updated_at DESC 
    LIMIT :limit
""")
    suspend fun getRecentlyModifiedPacientes(
        since: Long,
        limit: Int = 50
    ): List<PacienteEntity>

// ==================== VALIDAÇÃO E INTEGRIDADE ====================

    /**
     * Pacientes órfãos (sem relacionamentos)
     */
    @Query("""
    SELECT p.* FROM pacientes p 
    LEFT JOIN paciente_has_especialidade pe ON p.local_id = pe.paciente_local_id 
    WHERE pe.paciente_local_id IS NULL 
    AND p.is_deleted = 0
""")
    suspend fun getPacientesSemRelacionamentos(): List<PacienteEntity>



    /**
     * Pacientes com dados inconsistentes
     */
    @Query("""
    SELECT cpf, COUNT(*) as count 
    FROM pacientes 
    WHERE cpf IS NOT NULL 
    AND cpf != '' 
    AND is_deleted = 0 
    GROUP BY cpf 
    HAVING COUNT(*) > 1
""")
    suspend fun findDuplicatesByCpfRaw(): List<DuplicateCpfInfo>

    suspend fun findDuplicatesByCpf(): List<Map<String, Any>> {
        return findDuplicatesByCpfRaw().map {
            mapOf("cpf" to it.cpf, "count" to it.count)
        }
    }

// ==================== BACKUP E RESTAURAÇÃO ====================

    /**
     * Exportar todos os pacientes para backup
     */
    @Query("SELECT * FROM pacientes ORDER BY created_at DESC")
    suspend fun exportAllPacientesForBackup(): List<PacienteEntity>

    /**
     * Pacientes criados em um período específico
     */
    @Query("""
    SELECT * FROM pacientes 
    WHERE created_at BETWEEN :startDate AND :endDate 
    AND is_deleted = 0 
    ORDER BY created_at DESC
""")
    suspend fun getPacientesByDateRange(
        startDate: Long,
        endDate: Long
    ): List<PacienteEntity>

}