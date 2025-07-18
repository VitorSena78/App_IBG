package com.example.projeto_ibg3.data.local.database.relations

import androidx.room.Embedded
import androidx.room.Relation
import com.example.projeto_ibg3.data.local.database.entities.EspecialidadeEntity
import com.example.projeto_ibg3.data.local.database.entities.PacienteEntity
import com.example.projeto_ibg3.data.local.database.entities.PacienteEspecialidadeEntity

// Classe para consultas com informações completas
data class PacienteEspecialidadeWithDetails(
    @Embedded
    val pacienteEspecialidade: PacienteEspecialidadeEntity,

    @Relation(
        parentColumn = "paciente_local_id",  // campo na PacienteEspecialidadeEntity
        entityColumn = "local_id"            // campo na PacienteEntity
    )
    val paciente: PacienteEntity,

    @Relation(
        parentColumn = "especialidade_local_id", // campo na PacienteEspecialidadeEntity
        entityColumn = "local_id"                // campo na EspecialidadeEntity
    )
    val especialidade: EspecialidadeEntity
)
