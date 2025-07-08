package com.example.projeto_ibg3.data.remote.dto

import com.google.gson.annotations.SerializedName

data class EspecialidadeDto(
    @SerializedName("id")
    val id: Long,

    @SerializedName("nome")
    val nome: String
)
