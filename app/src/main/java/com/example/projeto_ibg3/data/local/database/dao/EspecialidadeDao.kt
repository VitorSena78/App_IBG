package com.example.projeto_ibg3.data.local.database.dao

import androidx.room.*
import com.example.projeto_ibg3.data.local.database.entities.EspecialidadeEntity
import com.example.projeto_ibg3.data.local.database.entities.EspecialidadeFichas
import com.example.projeto_ibg3.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface EspecialidadeDao {

    // CONSULTAS BÁSICAS
    @Query("SELECT * FROM especialidades WHERE is_deleted = 0 ORDER BY nome ASC")
    fun getAllEspecialidades(): Flow<List<EspecialidadeEntity>>

    @Query("SELECT * FROM especialidades WHERE is_deleted = 0 ORDER BY nome ASC")
    suspend fun getAllEspecialidadesList(): List<EspecialidadeEntity>

    //Consulta apenas especialidades disponíveis (com fichas > 0)
    @Query("SELECT * FROM especialidades WHERE is_deleted = 0 AND fichas > 0 ORDER BY nome ASC")
    fun getEspecialidadesDisponiveis(): Flow<List<EspecialidadeEntity>>

    @Query("SELECT * FROM especialidades WHERE is_deleted = 0 AND fichas > 0 ORDER BY nome ASC")
    suspend fun getEspecialidadesDisponiveisList(): List<EspecialidadeEntity>

    // Consulta especialidades esgotadas (fichas = 0)
    @Query("SELECT * FROM especialidades WHERE is_deleted = 0 AND fichas = 0 ORDER BY nome ASC")
    suspend fun getEspecialidadesEsgotadas(): List<EspecialidadeEntity>

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

    // Contar especialidades disponíveis
    @Query("SELECT COUNT(*) FROM especialidades WHERE is_deleted = 0 AND fichas > 0")
    suspend fun getEspecialidadesDisponiveisCount(): Int

    @Query("SELECT * FROM especialidades WHERE nome LIKE '%' || :nome || '%' AND is_deleted = 0")
    suspend fun searchEspecialidadesByName(nome: String): List<EspecialidadeEntity>

    //  Buscar especialidades disponíveis por nome
    @Query("SELECT * FROM especialidades WHERE nome LIKE '%' || :nome || '%' AND is_deleted = 0 AND fichas > 0")
    suspend fun searchEspecialidadesDisponiveisByName(nome: String): List<EspecialidadeEntity>

    // INSERÇÃO E ATUALIZAÇÃO
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEspecialidade(especialidade: EspecialidadeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEspecialidades(especialidades: List<EspecialidadeEntity>)

    @Update
    suspend fun updateEspecialidade(especialidade: EspecialidadeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateEspecialidade(especialidade: EspecialidadeEntity)

    // ==================== MÉTODOS EM BATCH OTIMIZADOS ====================

    /**
     * Atualiza múltiplas especialidades em uma única transação
     */
    @Update
    suspend fun updateEspecialidades(especialidades: List<EspecialidadeEntity>)

    /**
     * Inserção ou atualização em batch (upsert)
     * Usa REPLACE strategy para atualizar se já existir
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEspecialidades(especialidades: List<EspecialidadeEntity>)

    /**
     * Busca fichas de múltiplas especialidades por IDs (para cache otimizado)
     */
    @Query("SELECT local_id, fichas FROM especialidades WHERE local_id IN (:especialidadeIds)")
    suspend fun getFichasByIds(especialidadeIds: List<String>): List<EspecialidadeFichas>

    /**
     * Atualização em batch do status de sincronização
     */
    @Query("UPDATE especialidades SET sync_status = :status, updated_at = :timestamp WHERE local_id IN (:localIds)")
    suspend fun batchUpdateSyncStatus(
        localIds: List<String>,
        status: SyncStatus,
        timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Atualização em batch do status de sincronização com server_id
     */
    @Transaction
    suspend fun batchUpdateSyncStatusAndServerId(updates: List<Triple<String, SyncStatus, Long>>) {
        updates.forEach { (localId, status, serverId) ->
            markAsSynced(localId, serverId, status)
        }
    }

    /**
     * Incrementa tentativas de sincronização em batch
     */
    @Transaction
    suspend fun batchIncrementSyncAttempts(attempts: List<Triple<String, Long, String?>>) {
        attempts.forEach { (localId, timestamp, errorMessage) ->
            // Assumindo que você tenha campos para tracking de tentativas
            updateSyncStatus(localId, SyncStatus.UPLOAD_FAILED, timestamp)
        }
    }

    /**
     * Busca especialidades por múltiplos server_ids (otimizado para cache)
     */
    @Query("SELECT * FROM especialidades WHERE server_id IN (:serverIds) AND is_deleted = 0")
    suspend fun getEspecialidadesByServerIds(serverIds: List<Long>): List<EspecialidadeEntity>

    /**
     * Decrementa fichas de múltiplas especialidades atomicamente
     */
    @Transaction
    suspend fun decrementarFichasMultiplas(especialidadeIds: List<String>) {
        especialidadeIds.forEach { localId ->
            decrementarFichas(localId)
        }
    }

    /**
     * Incrementa fichas de múltiplas especialidades atomicamente
     */
    @Transaction
    suspend fun incrementarFichasMultiplas(especialidadeIds: List<String>) {
        especialidadeIds.forEach { localId ->
            incrementarFichas(localId)
        }
    }

    /**
     * Busca mapa de especialidades por server_id para cache otimizado
     */
    @Query("SELECT * FROM especialidades WHERE server_id IS NOT NULL AND is_deleted = 0")
    suspend fun getAllEspecialidadesWithServerIdMap(): List<EspecialidadeEntity>

    /**
     * Verifica existência de múltiplas especialidades por nome
     */
    @Query("SELECT nome FROM especialidades WHERE nome IN (:nomes) AND is_deleted = 0")
    suspend fun getExistingEspecialidadeNames(nomes: List<String>): List<String>

    /**
     * Atualização otimizada de fichas restantes do dia
     */
    @Query("""
    UPDATE especialidades 
    SET atendimentos_restantes_hoje = :restantes,
        atendimentos_totais_hoje = :totais,
        updated_at = :timestamp 
    WHERE local_id = :localId
""")
    suspend fun updateAtendimentosHoje(
        localId: String,
        restantes: Int,
        totais: Int,
        timestamp: Long = System.currentTimeMillis()
    )

    /**
     * Atualização em batch dos atendimentos de hoje
     */
    @Transaction
    suspend fun batchUpdateAtendimentosHoje(updates: List<Triple<String, Int, Int>>) {
        val timestamp = System.currentTimeMillis()
        updates.forEach { (localId, restantes, totais) ->
            updateAtendimentosHoje(localId, restantes, totais, timestamp)
        }
    }

    // Métodos para gerenciar fichas
    @Query("UPDATE especialidades SET fichas = fichas - 1, updated_at = :timestamp WHERE local_id = :localId AND fichas > 0")
    suspend fun decrementarFichas(localId: String, timestamp: Long = System.currentTimeMillis()): Int

    @Query("UPDATE especialidades SET fichas = fichas + 1, updated_at = :timestamp WHERE local_id = :localId")
    suspend fun incrementarFichas(localId: String, timestamp: Long = System.currentTimeMillis()): Int

    @Query("UPDATE especialidades SET fichas = :novaQuantidade, updated_at = :timestamp WHERE local_id = :localId")
    suspend fun updateFichas(localId: String, novaQuantidade: Int, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT fichas FROM especialidades WHERE local_id = :localId")
    suspend fun getFichasCount(localId: String): Int

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

    @Query("SELECT * FROM especialidades WHERE server_id = :serverId AND is_deleted = 0 LIMIT 1")
    suspend fun getEspecialidadeByServerId(serverId: Long): EspecialidadeEntity?
}