package com.example.projeto_ibg3.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PacienteEspecialidadeDTO(
    @SerializedName("paciente_server_id")
    val pacienteServerId: Long?,

    @SerializedName("especialidade_server_id")
    val especialidadeServerId: Long?,

    @SerializedName("data_atendimento")
    val dataAtendimento: String?,

    @SerializedName("created_at")
    val createdAt: String,

    @SerializedName("updated_at")
    val updatedAt: String,

    @SerializedName("last_sync_timestamp")
    val lastSyncTimestamp: Long?,

    @SerializedName("action")
    val action: String? = null
) {
    val isDeleted: Boolean
        get() = action == "DELETE" || action == "DELETED"
}
