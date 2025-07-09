package com.example.projeto_ibg3.data.dao

import androidx.room.*
import com.example.projeto_ibg3.data.entity.EspecialidadeEntity
import com.example.projeto_ibg3.model.SyncStatus
import kotlinx.coroutines.flow.Flow

@Dao
interface EspecialidadeDao {

    @Query("SELECT * FROM especialidades WHERE is_deleted = 0 ORDER BY nome ASC")
    fun getAllEspecialidades(): Flow<List<EspecialidadeEntity>>

    @Query("SELECT * FROM especialidades WHERE id = :id AND is_deleted = 0")
    suspend fun getEspecialidadeById(id: Long): EspecialidadeEntity?

    @Query("SELECT * FROM especialidades WHERE server_id = :serverId AND is_deleted = 0")
    suspend fun getEspecialidadeByServerId(serverId: Long): EspecialidadeEntity?

    @Query("SELECT * FROM especialidades WHERE nome = :nome")
    suspend fun getEspecialidadeByName(nome: String): EspecialidadeEntity?

    @Query("SELECT COUNT(*) FROM especialidades")
    suspend fun getEspecialidadesCount(): Int

    @Query("SELECT * FROM especialidades WHERE nome LIKE '%' || :nome || '%' AND is_deleted = 0")
    suspend fun searchEspecialidadesByName(nome: String): List<EspecialidadeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEspecialidade(especialidade: EspecialidadeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEspecialidades(especialidades: List<EspecialidadeEntity>)

    @Update
    suspend fun updateEspecialidade(especialidade: EspecialidadeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateEspecialidade(especialidade: EspecialidadeEntity)

    @Query("UPDATE especialidades SET is_deleted = 1, sync_status = :status, last_modified = :timestamp WHERE id = :id")
    suspend fun markAsDeleted(id: Long, status: SyncStatus = SyncStatus.PENDING_DELETE, timestamp: Long)

    @Query("DELETE FROM especialidades WHERE id = :id")
    suspend fun deleteEspecialidadePermanently(id: Long)

    // SINCRONIZAÇÃO
    @Query("SELECT * FROM especialidades WHERE sync_status = :status AND is_deleted = 0")
    suspend fun getPendingSync(status: SyncStatus = SyncStatus.PENDING_UPLOAD): List<EspecialidadeEntity>

    @Query("SELECT * FROM especialidades WHERE sync_status = :status AND is_deleted = 1")
    suspend fun getPendingDeletions(status: SyncStatus = SyncStatus.PENDING_DELETE): List<EspecialidadeEntity>
}