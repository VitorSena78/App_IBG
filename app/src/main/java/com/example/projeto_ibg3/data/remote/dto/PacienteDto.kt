package com.example.projeto_ibg3.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PacienteDto(
    @SerializedName("id")
    val id: Long,

    @SerializedName("nome")
    val nome: String,

    @SerializedName("data_nascimento")
    val dataNascimento: String? = null, // ISO format: "2023-01-15"

    @SerializedName("idade")
    val idade: Int? = null,

    @SerializedName("nome_da_mae")
    val nomeDaMae: String? = null,

    @SerializedName("cpf")
    val cpf: String? = null,

    @SerializedName("sus")
    val sus: String? = null,

    @SerializedName("telefone")
    val telefone: String? = null,

    @SerializedName("endereco")
    val endereco: String? = null,

    @SerializedName("pa_x_mmhg")
    val paXMmhg: String? = null,

    @SerializedName("fc_bpm")
    val fcBpm: Float? = null,

    @SerializedName("fr_ibpm")
    val frIbpm: Float? = null,

    @SerializedName("temperatura_c")
    val temperaturaC: Float? = null,

    @SerializedName("hgt_mgld")
    val hgtMgld: Float? = null,

    @SerializedName("spo2")
    val spo2: Float? = null,

    @SerializedName("peso")
    val peso: Float? = null,

    @SerializedName("altura")
    val altura: Float? = null,

    @SerializedName("imc")
    val imc: Float? = null,

    @SerializedName("created_at")
    val createdAt: String? = null,

    @SerializedName("updated_at")
    val updatedAt: String? = null
)
