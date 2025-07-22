package com.example.projeto_ibg3.data.remote.dto

import com.google.gson.annotations.SerializedName
import java.util.UUID

data class EspecialidadeDto(
    // CORRIGIDO: O servidor retorna "id", não "server_id"
    @SerializedName("id")
    val serverId: Long?,

    @SerializedName("local_id")
    val localId: String = UUID.randomUUID().toString(),

    @SerializedName("nome")
    val nome: String,

    @SerializedName("device_id")
    val deviceId: String? = null,

    @SerializedName("last_sync_timestamp")
    val lastSyncTimestamp: Long? = null, // Tornar opcional

    // CORRIGIDO: O servidor retorna datas como string ISO, não timestamp
    @SerializedName("createdAt")
    val createdAt: String? = null,

    @SerializedName("updatedAt")
    val updatedAt: String? = null,

    @SerializedName("is_deleted")
    val isDeleted: Boolean? = null // Tornar opcional pois o servidor não envia
)

