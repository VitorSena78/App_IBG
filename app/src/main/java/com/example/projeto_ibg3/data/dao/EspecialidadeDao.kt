package com.example.projeto_ibg3.data.dao

import androidx.room.*
import com.example.projeto_ibg3.data.entity.EspecialidadeEntity
import com.example.projeto_ibg3.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface EspecialidadeDao {

    // CONSULTAS BÁSICAS (já estão corretas)
    @Query("SELECT * FROM especialidades WHERE is_deleted = 0 ORDER BY nome ASC")
    fun getAllEspecialidades(): Flow<List<EspecialidadeEntity>>

    @Query("SELECT * FROM especialidades WHERE id = :id AND is_deleted = 0")
    suspend fun getEspecialidadeById(id: Long): EspecialidadeEntity?

    @Query("SELECT * FROM especialidades WHERE server_id = :serverId AND is_deleted = 0")
    suspend fun getEspecialidadeByServerId(serverId: Long): EspecialidadeEntity?

    // CORREÇÃO: Adicionar filtro is_deleted na busca por nome
    @Query("SELECT * FROM especialidades WHERE nome = :nome AND is_deleted = 0")
    suspend fun getEspecialidadeByName(nome: String): EspecialidadeEntity?

    // CORREÇÃO: Filtrar apenas registros não deletados na contagem
    @Query("SELECT COUNT(*) FROM especialidades WHERE is_deleted = 0")
    suspend fun getEspecialidadesCount(): Int

    @Query("SELECT * FROM especialidades WHERE nome LIKE '%' || :nome || '%' AND is_deleted = 0")
    suspend fun searchEspecialidadesByName(nome: String): List<EspecialidadeEntity>

    // INSERÇÃO E ATUALIZAÇÃO (já estão corretas)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEspecialidade(especialidade: EspecialidadeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEspecialidades(especialidades: List<EspecialidadeEntity>)

    @Update
    suspend fun updateEspecialidade(especialidade: EspecialidadeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateEspecialidade(especialidade: EspecialidadeEntity)

    // SOFT DELETE (já está correto)
    @Query("UPDATE especialidades SET is_deleted = 1, sync_status = :status, last_modified = :timestamp WHERE id = :id")
    suspend fun markAsDeleted(id: Long, status: SyncStatus = SyncStatus.PENDING_DELETE, timestamp: Long)

    // HARD DELETE (manter para limpeza)
    @Query("DELETE FROM especialidades WHERE id = :id")
    suspend fun deleteEspecialidadePermanently(id: Long)

    // SINCRONIZAÇÃO (já estão corretas)
    @Query("SELECT * FROM especialidades WHERE sync_status = :status AND is_deleted = 0")
    suspend fun getPendingSync(status: SyncStatus = SyncStatus.PENDING_UPLOAD): List<EspecialidadeEntity>

    @Query("SELECT * FROM especialidades WHERE sync_status = :status AND is_deleted = 1")
    suspend fun getPendingDeletions(status: SyncStatus = SyncStatus.PENDING_DELETE): List<EspecialidadeEntity>

    // MELHORIAS ADICIONAIS RECOMENDADAS:

    // 1. Método para restaurar registro deletado
    @Query("UPDATE especialidades SET is_deleted = 0, sync_status = :status, last_modified = :timestamp WHERE id = :id")
    suspend fun restoreDeleted(id: Long, status: SyncStatus = SyncStatus.PENDING_UPLOAD, timestamp: Long = System.currentTimeMillis())

    // 2. Buscar registros deletados (para auditoria/recuperação)
    @Query("SELECT * FROM especialidades WHERE is_deleted = 1 ORDER BY last_modified DESC")
    suspend fun getDeletedEspecialidades(): List<EspecialidadeEntity>

    // 3. Limpeza de registros antigos (hard delete)
    @Query("DELETE FROM especialidades WHERE is_deleted = 1 AND last_modified < :cutoffTimestamp")
    suspend fun cleanupOldDeletedRecords(cutoffTimestamp: Long)

    // 4. Verificar se existe especialidade com mesmo nome (para evitar duplicatas)
    @Query("SELECT COUNT(*) FROM especialidades WHERE LOWER(nome) = LOWER(:nome) AND is_deleted = 0 AND id != :excludeId")
    suspend fun countEspecialidadesByName(nome: String, excludeId: Long = 0): Int

    // 5. Atualizar status de sincronização
    @Query("UPDATE especialidades SET sync_status = :newStatus, last_modified = :timestamp WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, newStatus: SyncStatus, timestamp: Long = System.currentTimeMillis())

    // 6. Buscar especialidades modificadas após determinada data
    @Query("SELECT * FROM especialidades WHERE last_modified > :timestamp AND is_deleted = 0")
    suspend fun getModifiedSince(timestamp: Long): List<EspecialidadeEntity>

    // 7. Marcar múltiplas especialidades como deletadas
    @Query("UPDATE especialidades SET is_deleted = 1, sync_status = :status, last_modified = :timestamp WHERE id IN (:ids)")
    suspend fun markMultipleAsDeleted(ids: List<Long>, status: SyncStatus = SyncStatus.PENDING_DELETE, timestamp: Long = System.currentTimeMillis())

    // 8. Buscar especialidades por status de sincronização
    @Query("SELECT * FROM especialidades WHERE sync_status = :status")
    suspend fun getEspecialidadesByStatus(status: SyncStatus): List<EspecialidadeEntity>

    // 9. Resetar status de sincronização para registros com erro
    @Query("UPDATE especialidades SET sync_status = :newStatus WHERE sync_status = :oldStatus")
    suspend fun resetSyncStatus(oldStatus: SyncStatus, newStatus: SyncStatus)

    // 10. Verificar se há mudanças pendentes de sincronização
    @Query("SELECT COUNT(*) FROM especialidades WHERE sync_status IN (:statuses)")
    suspend fun countPendingSync(statuses: List<SyncStatus> = listOf(SyncStatus.PENDING_UPLOAD, SyncStatus.PENDING_DELETE)): Int
}