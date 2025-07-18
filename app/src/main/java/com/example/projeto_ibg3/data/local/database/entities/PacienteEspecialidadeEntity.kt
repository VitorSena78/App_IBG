package com.example.projeto_ibg3.data.local.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.TypeConverters
import com.example.projeto_ibg3.data.local.database.converters.SyncStatusConverter
import com.example.projeto_ibg3.domain.model.PacienteEspecialidade
import com.example.projeto_ibg3.domain.model.SyncStatus

@Entity(
    tableName = "paciente_has_especialidade",
    primaryKeys = ["paciente_local_id", "especialidade_local_id"],
    foreignKeys = [
        ForeignKey(
            entity = PacienteEntity::class,
            parentColumns = ["local_id"],
            childColumns = ["paciente_local_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = EspecialidadeEntity::class,
            parentColumns = ["local_id"],
            childColumns = ["especialidade_local_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["paciente_local_id"]),
        Index(value = ["especialidade_local_id"]),
        Index(value = ["paciente_server_id"]),
        Index(value = ["especialidade_server_id"])
    ]
)
@TypeConverters(SyncStatusConverter::class)
data class PacienteEspecialidadeEntity(
    // Chaves primárias compostas - IDs locais
    @ColumnInfo(name = "paciente_local_id")
    val pacienteLocalId: String,

    @ColumnInfo(name = "especialidade_local_id")
    val especialidadeLocalId: String,

    // IDs do servidor (para sincronização)
    @ColumnInfo(name = "paciente_server_id")
    val pacienteServerId: Long? = null,

    @ColumnInfo(name = "especialidade_server_id")
    val especialidadeServerId: Long? = null,

    // Dados específicos da relação
    @ColumnInfo(name = "data_atendimento")
    val dataAtendimento: Long? = null, // Timestamp da data de atendimento

    // Campos para sincronização
    @ColumnInfo(name = "sync_status")
    val syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD,

    @ColumnInfo(name = "device_id")
    val deviceId: String = "", // Será preenchido no Repository

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "last_sync_timestamp")
    val lastSyncTimestamp: Long = 0,

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,

    // Controle de versão para conflitos
    @ColumnInfo(name = "version")
    val version: Int = 1,

    // Dados conflitantes como JSON
    @ColumnInfo(name = "conflict_data")
    val conflictData: String? = null,

    // Controle de tentativas de sincronização
    @ColumnInfo(name = "sync_attempts")
    val syncAttempts: Int = 0,

    @ColumnInfo(name = "last_sync_attempt")
    val lastSyncAttempt: Long = 0,

    @ColumnInfo(name = "sync_error")
    val syncError: String? = null
) {
    // ID único para esta relação (útil para operações que precisam de um ID único)
    val relationId: String
        get() = "${pacienteLocalId}_${especialidadeLocalId}"

    companion object {
        fun create(
            pacienteLocalId: String,
            especialidadeLocalId: String,
            dataAtendimento: Long? = null,
            deviceId: String = ""
        ): PacienteEspecialidadeEntity {
            return PacienteEspecialidadeEntity(
                pacienteLocalId = pacienteLocalId,
                especialidadeLocalId = especialidadeLocalId,
                dataAtendimento = dataAtendimento,
                deviceId = deviceId
            )
        }

        fun fromServerIds(
            pacienteServerId: Long,
            especialidadeServerId: Long,
            pacienteLocalId: String,
            especialidadeLocalId: String,
            dataAtendimento: Long? = null,
            deviceId: String = ""
        ): PacienteEspecialidadeEntity {
            return PacienteEspecialidadeEntity(
                pacienteLocalId = pacienteLocalId,
                especialidadeLocalId = especialidadeLocalId,
                pacienteServerId = pacienteServerId,
                especialidadeServerId = especialidadeServerId,
                dataAtendimento = dataAtendimento,
                deviceId = deviceId,
                syncStatus = SyncStatus.SYNCED
            )
        }

        fun fromModel(model: PacienteEspecialidade, deviceId: String = ""): PacienteEspecialidadeEntity {
            return PacienteEspecialidadeEntity(
                pacienteLocalId = model.pacienteLocalId,
                especialidadeLocalId = model.especialidadeLocalId,
                dataAtendimento = model.dataAtendimento?.time,
                pacienteServerId = model.pacienteServerId,
                especialidadeServerId = model.especialidadeServerId,
                syncStatus = model.syncStatus,
                deviceId = deviceId
            )
        }
    }
}