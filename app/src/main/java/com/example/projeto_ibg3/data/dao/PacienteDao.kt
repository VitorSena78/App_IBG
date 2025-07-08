package com.example.projeto_ibg3.data.dao

import androidx.room.*
import com.example.projeto_ibg3.data.entity.PacienteEntity
import com.example.projeto_ibg3.data.relation.PacienteComEspecialidades
import com.example.projeto_ibg3.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface PacienteDao {
    // ========== CONSULTAS BÁSICAS ==========

    @Query("SELECT * FROM pacientes WHERE is_deleted = 0 ORDER BY nome ASC")
    fun getAllPacientes(): Flow<List<PacienteEntity>>

    @Query("SELECT * FROM pacientes WHERE id = :id AND is_deleted = 0")
    suspend fun getPacienteById(id: Long): PacienteEntity?

    @Query("SELECT * FROM pacientes WHERE server_id = :serverId AND is_deleted = 0")
    suspend fun getPacienteByServerId(serverId: Long): PacienteEntity?

    @Query("SELECT * FROM pacientes WHERE cpf = :cpf AND is_deleted = 0")
    suspend fun getPacienteByCpf(cpf: String): PacienteEntity?

    @Query("SELECT * FROM pacientes WHERE sus = :sus AND is_deleted = 0")
    suspend fun getPacienteBySus(sus: String): PacienteEntity?

    @Query("SELECT * FROM pacientes WHERE telefone = :telefone AND is_deleted = 0")
    suspend fun getPacientesByTelefone(telefone: String): List<PacienteEntity>

    @Query("SELECT COUNT(*) FROM pacientes WHERE is_deleted = 0")
    suspend fun getTotalPacientes(): Int

    // ========== CONSULTAS RELACIONADAS ==========

    @Transaction
    @Query("SELECT * FROM pacientes WHERE is_deleted = 0 ORDER BY nome ASC")
    fun getAllPacientesComEspecialidades(): Flow<List<PacienteComEspecialidades>>

    @Transaction
    @Query("SELECT * FROM pacientes WHERE id = :id AND is_deleted = 0")
    suspend fun getPacienteComEspecialidades(id: Long): PacienteComEspecialidades?

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
    suspend fun insertPacientes(pacientes: List<PacienteEntity>)

    @Update
    suspend fun updatePaciente(paciente: PacienteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePaciente(paciente: PacienteEntity)

    // ========== MARCAÇÃO E DELEÇÃO ==========

    @Query("UPDATE pacientes SET is_deleted = 1, sync_status = :status, last_modified = :timestamp WHERE id = :id")
    suspend fun markAsDeleted(id: Long, status: SyncStatus = SyncStatus.PENDING_DELETION, timestamp: Long)

    @Query("DELETE FROM pacientes WHERE id = :id")
    suspend fun deletePacientePermanently(id: Long)

    // ========== SINCRONIZAÇÃO ==========

    @Query("SELECT * FROM pacientes WHERE sync_status = :status AND is_deleted = 0")
    suspend fun getPendingSync(status: SyncStatus = SyncStatus.PENDING_UPLOAD): List<PacienteEntity>

    @Query("SELECT * FROM pacientes WHERE sync_status = :status AND is_deleted = 1")
    suspend fun getPendingDeletions(status: SyncStatus = SyncStatus.PENDING_DELETION): List<PacienteEntity>

    @Query("SELECT * FROM pacientes WHERE sync_status = :status")
    suspend fun getPacientesBySyncStatus(status: SyncStatus): List<PacienteEntity>

    @Query("UPDATE pacientes SET sync_status = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, status: SyncStatus)

    @Query("UPDATE pacientes SET sync_status = :status, server_id = :serverId WHERE id = :id")
    suspend fun updateSyncStatusAndServerId(id: Long, status: SyncStatus, serverId: Long)

    // ========== ESTATÍSTICAS E CONTAGEM DE SINCRONIZAÇÃO ==========

    @Query("SELECT COUNT(*) FROM pacientes WHERE sync_status != :status")
    suspend fun getUnsyncedCount(status: SyncStatus = SyncStatus.SYNCED): Int

    @Query("SELECT COUNT(*) FROM pacientes WHERE sync_status = 'PENDING_UPLOAD'")
    suspend fun getPendingSyncCount(): Int

    @Query("SELECT COUNT(*) FROM pacientes WHERE sync_status = 'PENDING_DELETION'")
    suspend fun getPendingDeletionCount(): Int

    @Query("SELECT COUNT(*) FROM pacientes WHERE sync_status = 'CONFLICT'")
    suspend fun getConflictCount(): Int

    // ========== RELATÓRIOS MÉDICOS ==========

    @Query("SELECT * FROM pacientes WHERE data_nascimento BETWEEN :startDate AND :endDate AND is_deleted = 0 ORDER BY data_nascimento ASC")
    fun getPacientesByAgeRange(startDate: Long, endDate: Long): Flow<List<PacienteEntity>>

    @Query("SELECT * FROM pacientes WHERE peso IS NOT NULL AND altura IS NOT NULL AND is_deleted = 0")
    suspend fun getPacientesWithBMI(): List<PacienteEntity>

    @Query("SELECT * FROM pacientes WHERE temperatura_c > :tempMin AND temperatura_c IS NOT NULL AND is_deleted = 0")
    suspend fun getPacientesWithFever(tempMin: Float = 37.5f): List<PacienteEntity>

    @Query("SELECT * FROM pacientes WHERE hgt_mgld > :glicemiaMax AND hgt_mgld IS NOT NULL AND is_deleted = 0")
    suspend fun getPacientesWithHighGlycemia(glicemiaMax: Float = 100f): List<PacienteEntity>

    @Query("""
        SELECT * FROM pacientes 
        WHERE is_deleted = 0 AND 
              (peso IS NOT NULL OR altura IS NOT NULL OR pa_x_mmhg IS NOT NULL)
        ORDER BY last_modified DESC
    """)
    fun getPacientesWithVitalSigns(): Flow<List<PacienteEntity>>

    // ========== VERIFICAÇÃO DE DUPLICATAS ==========

    @Query("SELECT COUNT(*) FROM pacientes WHERE cpf = :cpf AND is_deleted = 0 AND id != :excludeId")
    suspend fun countPacientesByCpfExcluding(cpf: String, excludeId: Long): Int

    @Query("SELECT COUNT(*) FROM pacientes WHERE sus = :sus AND is_deleted = 0 AND id != :excludeId")
    suspend fun countPacientesBySusExcluding(sus: String, excludeId: Long): Int

    // ========== METADADOS DE SINCRONIZAÇÃO ==========

    @Query("SELECT MAX(last_modified) FROM pacientes")
    suspend fun getLastSyncTimestamp(): Long?

    // ========== LIMPEZA DE DADOS ==========

    @Query("DELETE FROM pacientes WHERE is_deleted = 1 AND last_modified < :cutoffTime")
    suspend fun cleanupOldDeletedPacientes(cutoffTime: Long)

    @Query("DELETE FROM pacientes WHERE sync_status = 'SYNCED' AND is_deleted = 1")
    suspend fun cleanupSyncedDeletedPacientes()
}