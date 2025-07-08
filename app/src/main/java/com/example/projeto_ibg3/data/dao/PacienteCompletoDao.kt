package com.example.projeto_ibg3.data.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PacienteCompletoDao {

    @Query("""
        SELECT p.*, 
               GROUP_CONCAT(e.nome) as especialidades_nomes,
               GROUP_CONCAT(e.id) as especialidades_ids
        FROM paciente p
        LEFT JOIN paciente_has_especialidade phe ON p.id = phe.paciente_id AND phe.is_deleted = 0
        LEFT JOIN especialidade e ON phe.especialidade_id = e.id AND e.is_deleted = 0
        WHERE p.is_deleted = 0
        GROUP BY p.id
        ORDER BY p.nome ASC
    """)
    fun getPacientesComEspecialidades(): Flow<List<PacienteComEspecialidadeResult>>

    @Query("""
        SELECT p.*, 
               GROUP_CONCAT(e.nome) as especialidades_nomes,
               GROUP_CONCAT(e.id) as especialidades_ids
        FROM paciente p
        LEFT JOIN paciente_has_especialidade phe ON p.id = phe.paciente_id AND phe.is_deleted = 0
        LEFT JOIN especialidade e ON phe.especialidade_id = e.id AND e.is_deleted = 0
        WHERE p.id = :pacienteId AND p.is_deleted = 0
        GROUP BY p.id
    """)
    suspend fun getPacienteComEspecialidades(pacienteId: Long): PacienteComEspecialidadeResult?
}