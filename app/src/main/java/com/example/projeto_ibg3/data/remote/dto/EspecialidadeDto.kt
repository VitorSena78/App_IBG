package com.example.projeto_ibg3.data.remote.dto

import com.google.gson.annotations.SerializedName
import java.util.UUID

data class EspecialidadeDto(
    // O servidor retorna "id", não "server_id"
    @SerializedName("server_id", ["id"])
    val serverId: Long?,

    @SerializedName("nome")
    val nome: String,

    @SerializedName("fichas")
    val fichas: Int? = null,

    @SerializedName("atendimentos_restantes_hoje")
    val atendimentosRestantesHoje: Int? = null,

    @SerializedName("atendimentos_totais_hoje")
    val atendimentosTotaisHoje: Int? = null,

    // O servidor retorna datas como string ISO, não timestamp
    @SerializedName("created_at")
    val createdAt: String? = null,

    @SerializedName("updated_at")
    val updatedAt: String? = null,

    @SerializedName("is_deleted")
    val isDeleted: Boolean? = null // Tornar opcional pois o servidor não envia
)

