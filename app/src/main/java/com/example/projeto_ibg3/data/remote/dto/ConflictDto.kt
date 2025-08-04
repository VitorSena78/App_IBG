package com.example.projeto_ibg3.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ConflictDto(
    @SerializedName("local_id")
    val localId: String,

    @SerializedName("server_id")
    val serverId: Long,

    @SerializedName("entity_type")
    val entityType: String = "PACIENTE",

    @SerializedName("server_data")
    val serverData: PacienteDto,

    @SerializedName("client_data")
    val clientData: PacienteDto,

    @SerializedName("description")
    val description: String,

    @SerializedName("conflict_fields")
    val conflictFields: List<String> = emptyList()
)
