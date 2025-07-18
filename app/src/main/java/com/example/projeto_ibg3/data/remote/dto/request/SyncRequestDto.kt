package com.example.projeto_ibg3.data.remote.dto.request

import com.example.projeto_ibg3.data.remote.dto.PacienteDto
import com.example.projeto_ibg3.data.remote.dto.SyncType
import com.google.gson.annotations.SerializedName

data class SyncRequestDto(
    @SerializedName("device_id")
    val deviceId: String,

    @SerializedName("last_sync_timestamp")
    val lastSyncTimestamp: Long,

    @SerializedName("pacientes")
    val pacientes: List<PacienteDto>,

    @SerializedName("app_version")
    val appVersion: String? = null,

    @SerializedName("sync_type")
    val syncType: SyncType = SyncType.INCREMENTAL,

    @SerializedName("batch_size")
    val batchSize: Int = 50
)

