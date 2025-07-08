package com.example.projeto_ibg3.data.relation

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.example.projeto_ibg3.data.entity.EspecialidadeEntity
import com.example.projeto_ibg3.data.entity.PacienteEntity
import com.example.projeto_ibg3.data.entity.PacienteEspecialidadeEntity
import com.example.projeto_ibg3.model.Paciente
import com.example.projeto_ibg3.data.entity.toEspecialidade
import com.example.projeto_ibg3.data.entity.toPaciente

data class PacienteComEspecialidades(
    @Embedded val paciente: PacienteEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            PacienteEspecialidadeEntity::class,
            parentColumn = "Paciente_id",
            entityColumn = "Especialidade_id"
        )
    )
    val especialidades: List<EspecialidadeEntity>
)

// Função para converter para o modelo de domínio
fun PacienteComEspecialidades.toPaciente(): Paciente {
    return paciente.toPaciente().copy(
        especialidades = especialidades.map { it.toEspecialidade() }
    )
}