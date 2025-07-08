package com.example.projeto_ibg3.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.example.projeto_ibg3.model.SyncStatus

@Entity(
    tableName = "Paciente_has_especialidade",
    primaryKeys = ["Paciente_id", "Especialidade_id"],
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = PacienteEntity::class,
            parentColumns = ["id"],
            childColumns = ["Paciente_id"],
            onDelete = androidx.room.ForeignKey.NO_ACTION,
            onUpdate = androidx.room.ForeignKey.NO_ACTION
        ),
        androidx.room.ForeignKey(
            entity = EspecialidadeEntity::class,
            parentColumns = ["id"],
            childColumns = ["Especialidade_id"],
            onDelete = androidx.room.ForeignKey.NO_ACTION,
            onUpdate = androidx.room.ForeignKey.NO_ACTION
        )
    ]
)
data class PacienteEspecialidadeEntity(
    @ColumnInfo(name = "Paciente_id")
    val pacienteId: Long,

    @ColumnInfo(name = "Especialidade_id")
    val especialidadeId: Long,

    @ColumnInfo(name = "data_atendimento")
    val dataAtendimento: Long = System.currentTimeMillis(), // Timestamp da data

    // Campos para sincronização
    @ColumnInfo(name = "sync_status")
    val syncStatus: SyncStatus = SyncStatus.SYNCED,

    @ColumnInfo(name = "last_modified")
    val lastModified: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false
)
