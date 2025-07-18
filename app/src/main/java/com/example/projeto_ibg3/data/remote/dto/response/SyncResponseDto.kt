package com.example.projeto_ibg3.data.remote.dto.response

import com.example.projeto_ibg3.data.remote.dto.ConflictDto
import com.example.projeto_ibg3.data.remote.dto.EspecialidadeDto
import com.example.projeto_ibg3.data.remote.dto.PacienteDto
import com.example.projeto_ibg3.data.remote.dto.SyncErrorDto
import com.google.gson.annotations.SerializedName

data class SyncResponseDto(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("sync_timestamp")
    val syncTimestamp: Long,

    @SerializedName("pacientes_servidor")
    val pacientesServidor: List<PacienteDto> = emptyList(),

    @SerializedName("especialidades")
    val especialidades: List<EspecialidadeDto> = emptyList(),

    @SerializedName("conflitos")
    val conflitos: List<ConflictDto> = emptyList(),

    @SerializedName("errors")
    val errors: List<SyncErrorDto> = emptyList(),

    @SerializedName("sync_stats")
    val syncStats: SyncStatsDto? = null,

    @SerializedName("next_sync_recommended")
    val nextSyncRecommended: Long? = null, // Timestamp recomendado para próxima sincronização

    @SerializedName("server_version")
    val serverVersion: String? = null
)

