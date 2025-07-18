package com.example.projeto_ibg3.data.remote.dto

import com.google.gson.annotations.SerializedName

data class EspecialidadeDto(
    @SerializedName("server_id")
    val serverId: Long?,

    @SerializedName("local_id")
    val localId: String,

    @SerializedName("nome")
    val nome: String,

    @SerializedName("device_id")
    val deviceId: String? = null,

    @SerializedName("last_sync_timestamp")
    val lastSyncTimestamp: Long = 0,

    @SerializedName("created_at")
    val createdAt: Long? = null,

    @SerializedName("updated_at")
    val updatedAt: Long? = null,

    @SerializedName("is_deleted")
    val isDeleted: Boolean = false
)