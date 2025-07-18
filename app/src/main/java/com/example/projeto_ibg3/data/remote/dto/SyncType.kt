package com.example.projeto_ibg3.data.remote.dto

import com.google.gson.annotations.SerializedName

enum class SyncType {
    @SerializedName("incremental")
    INCREMENTAL,

    @SerializedName("full")
    FULL
}