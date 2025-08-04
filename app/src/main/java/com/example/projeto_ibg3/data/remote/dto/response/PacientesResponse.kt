package com.example.projeto_ibg3.data.remote.dto.response

import com.example.projeto_ibg3.data.remote.dto.PacienteDto
import com.google.gson.annotations.SerializedName

data class PacientesResponse(
    @SerializedName("data")
    val pacientes: List<PacienteDto>,

    @SerializedName("total")
    val total: Int,

    @SerializedName("page")
    val page: Int
)
