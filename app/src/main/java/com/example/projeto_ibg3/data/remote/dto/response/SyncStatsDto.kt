package com.example.projeto_ibg3.data.remote.dto.response

import com.google.gson.annotations.SerializedName

data class SyncStatsDto(
    @SerializedName("processed_items")
    val processedItems: Int,

    @SerializedName("successful_items")
    val successfulItems: Int,

    @SerializedName("failed_items")
    val failedItems: Int,

    @SerializedName("conflict_items")
    val conflictItems: Int
)
