package com.example.projeto_ibg3.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PacientesResponse(
    @SerializedName("data")
    val pacientes: List<PacienteDto>,

    @SerializedName("total")
    val total: Int,

    @SerializedName("page")
    val page: Int
)
