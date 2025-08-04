package com.example.projeto_ibg3.data.remote.dto.request

import com.example.projeto_ibg3.data.remote.dto.PacienteDto
import com.google.gson.annotations.SerializedName

data class ConflictResolutionDto(
    @SerializedName("local_id")
    val localId: String,

    @SerializedName("server_id")
    val serverId: Long,

    @SerializedName("entity_type")
    val entityType: String = "PACIENTE",

    @SerializedName("resolution_strategy")
    val resolutionStrategy: String, // "USE_SERVER", "USE_CLIENT", "MERGE"

    @SerializedName("resolved_data")
    val resolvedData: PacienteDto? = null
)
