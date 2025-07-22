package com.example.projeto_ibg3.data.local.database.dao

import androidx.room.*
import com.example.projeto_ibg3.data.local.database.entities.EspecialidadeEntity
import com.example.projeto_ibg3.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface EspecialidadeDao {

    // CONSULTAS BÁSICAS
    @Query("SELECT * FROM especialidades WHERE is_deleted = 0 ORDER BY nome ASC")
    fun getAllEspecialidades(): Flow<List<EspecialidadeEntity>>

    @Query("SELECT * FROM especialidades WHERE is_deleted = 0 ORDER BY nome ASC")
    suspend fun getAllEspecialidadesList(): List<EspecialidadeEntity>

    @Query("SELECT * FROM especialidades WHERE local_id = :localId AND is_deleted = 0")
    suspend fun getEspecialidadeById(localId: String): EspecialidadeEntity?

    @Query("SELECT * FROM especialidades WHERE local_id = :localId AND is_deleted = 0")
    suspend fun getEspecialidadeByLocalId(localId: String): EspecialidadeEntity?

    @Query("SELECT * FROM especialidades WHERE server_id = :serverId AND is_deleted = 0")
    suspend fun getEspecialidadeByServerId(serverId: Long?): EspecialidadeEntity?

    @Query("SELECT * FROM especialidades WHERE nome = :nome AND is_deleted = 0")
    suspend fun getEspecialidadeByName(nome: String): EspecialidadeEntity?

    @Query("SELECT COUNT(*) FROM especialidades WHERE is_deleted = 0")
    suspend fun getEspecialidadesCount(): Int

    @Query("SELECT * FROM especialidades WHERE nome LIKE '%' || :nome || '%' AND is_deleted = 0")
    suspend fun searchEspecialidadesByName(nome: String): List<EspecialidadeEntity>

    // INSERÇÃO E ATUALIZAÇÃO
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEspecialidade(especialidade: EspecialidadeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEspecialidades(especialidades: List<EspecialidadeEntity>)

    @Update
    suspend fun updateEspecialidade(especialidade: EspecialidadeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateEspecialidade(especialidade: EspecialidadeEntity)

    // SOFT DELETE
    @Query("UPDATE especialidades SET is_deleted = 1, sync_status = :status, updated_at = :timestamp WHERE local_id = :localId")
    suspend fun markAsDeleted(localId: String, status: SyncStatus = SyncStatus.PENDING_DELETE, timestamp: Long = System.currentTimeMillis())

    // HARD DELETE
    @Query("DELETE FROM especialidades WHERE local_id = :localId")
    suspend fun deleteEspecialidadePermanently(localId: String)

    // SINCRONIZAÇÃO - MÉTODOS ESSENCIAIS
    @Query("SELECT * FROM especialidades WHERE sync_status = :status")
    suspend fun getEspecialidadesByStatus(status: SyncStatus): List<EspecialidadeEntity>

    @Query("SELECT * FROM especialidades WHERE sync_status IN (:statuses)")
    suspend fun getEspecialidadesByMultipleStatus(statuses: List<SyncStatus>): List<EspecialidadeEntity>

    // ADICIONAR ESTE MÉTODO - FOI O QUE ESTAVA FALTANDO
    @Query("SELECT COUNT(*) FROM especialidades WHERE sync_status = :status")
    suspend fun countByStatus(status: SyncStatus): Int

    // Para buscar itens que precisam ser sincronizados
    @Query("SELECT * FROM especialidades WHERE sync_status IN ('PENDING_UPLOAD', 'PENDING_DELETE', 'UPLOAD_FAILED', 'DELETE_FAILED') ORDER BY created_at ASC")
    suspend fun getPendingSyncItems(): List<EspecialidadeEntity>

    // Atualizar status de sincronização
    @Query("UPDATE especialidades SET sync_status = :newStatus, updated_at = :timestamp WHERE local_id = :localId")
    suspend fun updateSyncStatus(localId: String, newStatus: SyncStatus, timestamp: Long = System.currentTimeMillis())

    // Marcar como sincronizado e atualizar server_id
    @Query("UPDATE especialidades SET sync_status = :status, server_id = :serverId, last_sync_timestamp = :timestamp, updated_at = :timestamp WHERE local_id = :localId")
    suspend fun markAsSynced(localId: String, serverId: Long, status: SyncStatus = SyncStatus.SYNCED, timestamp: Long = System.currentTimeMillis())

    // Buscar especialidades modificadas após determinada data
    @Query("SELECT * FROM especialidades WHERE updated_at > :timestamp AND is_deleted = 0")
    suspend fun getModifiedSince(timestamp: Long): List<EspecialidadeEntity>

    // Verificar se existe especialidade com mesmo nome (para evitar duplicatas)
    @Query("SELECT COUNT(*) FROM especialidades WHERE LOWER(nome) = LOWER(:nome) AND is_deleted = 0 AND local_id != :excludeId")
    suspend fun countEspecialidadesByName(nome: String, excludeId: String = ""): Int

    // Limpeza de registros antigos (hard delete)
    @Query("DELETE FROM especialidades WHERE is_deleted = 1 AND updated_at < :cutoffTimestamp")
    suspend fun cleanupOldDeletedRecords(cutoffTimestamp: Long)

    // Verificar se há mudanças pendentes de sincronização
    @Query("SELECT COUNT(*) FROM especialidades WHERE sync_status IN ('PENDING_UPLOAD', 'PENDING_DELETE', 'UPLOAD_FAILED', 'DELETE_FAILED')")
    suspend fun countPendingSync(): Int

    // Resetar status de sincronização para registros com erro
    @Query("UPDATE especialidades SET sync_status = :newStatus WHERE sync_status = :oldStatus")
    suspend fun resetSyncStatus(oldStatus: SyncStatus, newStatus: SyncStatus)

    // Buscar registros deletados (para auditoria/recuperação)
    @Query("SELECT * FROM especialidades WHERE is_deleted = 1 ORDER BY updated_at DESC")
    suspend fun getDeletedEspecialidades(): List<EspecialidadeEntity>

    // Restaurar registro deletado
    @Query("UPDATE especialidades SET is_deleted = 0, sync_status = :status, updated_at = :timestamp WHERE local_id = :localId")
    suspend fun restoreDeleted(localId: String, status: SyncStatus = SyncStatus.PENDING_UPLOAD, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT MAX(updated_at) FROM especialidades WHERE sync_status = :status")
    suspend fun getLastSyncTimestamp(status: SyncStatus = SyncStatus.SYNCED): Long?

    @Query("SELECT MAX(updated_at) FROM especialidades")
    suspend fun getLastUpdatedTimestamp(): Long?
}