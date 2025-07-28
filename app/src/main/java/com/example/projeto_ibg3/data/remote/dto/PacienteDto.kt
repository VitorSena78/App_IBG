package com.example.projeto_ibg3.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PacienteDto(
    @SerializedName("server_id", ["id"])
    val serverId: Long? = null, // Pode ser null para novos pacientes

    @SerializedName("local_id")
    val localId: String, // Para sincronização

    @SerializedName("nome")
    val nome: String,

    @SerializedName("data_nascimento")
    val dataNascimento: String? = null, // ISO format: "2023-01-15"

    @SerializedName("idade")
    val idade: Int? = null,

    @SerializedName("nome_da_mae")
    val nomeDaMae: String? = null,

    @SerializedName("cpf")
    val cpf: String,

    @SerializedName("sus")
    val sus: String? = null,

    @SerializedName("telefone")
    val telefone: String? = null,

    @SerializedName("endereço")
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
    val createdAt: String,

    @SerializedName("updated_at")
    val updatedAt: String,

    // NOVOS: Campos para sincronização
    @SerializedName("device_id")
    val deviceId: String? = null,

    @SerializedName("version")
    val version: Int = 1,

    @SerializedName("last_sync_timestamp")
    val lastSyncTimestamp: Long? = null,

    @SerializedName("is_deleted")
    val isDeleted: Boolean = false
)
