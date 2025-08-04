package com.example.projeto_ibg3.data.local.database.dao

import androidx.room.*
import com.example.projeto_ibg3.data.local.database.relations.PacienteComEspecialidadeResult
import kotlinx.coroutines.flow.Flow

@Dao
interface PacienteCompletoDao {

    @Query("""
        SELECT p.*, 
               GROUP_CONCAT(e.nome) as especialidades_nomes,
               GROUP_CONCAT(e.localId) as especialidades_ids
        FROM paciente p
        LEFT JOIN paciente_has_especialidade phe ON p.localId = phe.paciente_id AND phe.is_deleted = 0
        LEFT JOIN especialidade e ON phe.especialidade_id = e.localId AND e.is_deleted = 0
        WHERE p.is_deleted = 0
        GROUP BY p.localId
        ORDER BY p.nome ASC
    """)
    fun getPacientesComEspecialidades(): Flow<List<PacienteComEspecialidadeResult>>

    @Query("""
        SELECT p.*, 
               GROUP_CONCAT(e.nome) as especialidades_nomes,
               GROUP_CONCAT(e.localId) as especialidades_ids
        FROM paciente p
        LEFT JOIN paciente_has_especialidade phe ON p.localId = phe.paciente_id AND phe.is_deleted = 0
        LEFT JOIN especialidade e ON phe.especialidade_id = e.localId AND e.is_deleted = 0
        WHERE p.localId = :pacienteLocalId AND p.is_deleted = 0
        GROUP BY p.localId
    """)
    suspend fun getPacienteComEspecialidades(pacienteId: Long): PacienteComEspecialidadeResult?
}