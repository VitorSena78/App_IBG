package com.example.projeto_ibg3.data.dao

import androidx.room.*
import com.example.projeto_ibg3.data.entity.EspecialidadeEntity
import com.example.projeto_ibg3.data.entity.PacienteEspecialidadeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PacienteEspecialidadeDao {

    @Query("SELECT * FROM Paciente_has_Especialidade")
    suspend fun getAll(): List<PacienteEspecialidadeEntity>

    @Query("SELECT * FROM Paciente_has_Especialidade")
    fun getAllPacienteEspecialidades(): Flow<List<PacienteEspecialidadeEntity>>

    @Query("SELECT * FROM Paciente_has_Especialidade WHERE Paciente_id = :pacienteId")
    suspend fun getEspecialidadesByPacienteId(pacienteId: Long): List<PacienteEspecialidadeEntity>

    @Query("SELECT * FROM Paciente_has_Especialidade WHERE Especialidade_id = :especialidadeId")
    suspend fun getPacientesByEspecialidadeId(especialidadeId: Long): List<PacienteEspecialidadeEntity>

    @Query("SELECT * FROM Paciente_has_Especialidade WHERE Paciente_id = :pacienteId AND Especialidade_id = :especialidadeId")
    suspend fun getPacienteEspecialidade(pacienteId: Long, especialidadeId: Long): PacienteEspecialidadeEntity?

    // Query corrigida com os nomes corretos das tabelas e colunas
    @Query("""
        SELECT e.* FROM especialidades e 
        INNER JOIN Paciente_has_Especialidade pe ON e.id = pe.Especialidade_id 
        WHERE pe.Paciente_id = :pacienteId AND e.is_Deleted = 0
    """)
    suspend fun getEspecialidadeEntitiesByPacienteId(pacienteId: Long): List<EspecialidadeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPacienteEspecialidade(pacienteEspecialidade: PacienteEspecialidadeEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPacienteEspecialidades(pacienteEspecialidades: List<PacienteEspecialidadeEntity>)

    @Update
    suspend fun updatePacienteEspecialidade(pacienteEspecialidade: PacienteEspecialidadeEntity)

    @Delete
    suspend fun deletePacienteEspecialidade(pacienteEspecialidade: PacienteEspecialidadeEntity)

    @Query("DELETE FROM Paciente_has_Especialidade WHERE Paciente_id = :pacienteId AND Especialidade_id = :especialidadeId")
    suspend fun deleteByIds(pacienteId: Long, especialidadeId: Long)

    @Query("DELETE FROM Paciente_has_Especialidade WHERE Paciente_id = :pacienteId")
    suspend fun deleteByPacienteId(pacienteId: Long)

    @Query("DELETE FROM Paciente_has_Especialidade WHERE Paciente_id = :pacienteId")
    suspend fun deleteAllEspecialidadesByPacienteId(pacienteId: Long)

    @Query("SELECT * FROM Paciente_has_Especialidade WHERE data_atendimento BETWEEN :dataInicio AND :dataFim")
    suspend fun getAtendimentosByDateRange(dataInicio: String, dataFim: String): List<PacienteEspecialidadeEntity>

    @Query("SELECT COUNT(*) FROM Paciente_has_Especialidade WHERE Especialidade_id = :especialidadeId")
    suspend fun getAtendimentosCountByEspecialidade(especialidadeId: Long): Int
}