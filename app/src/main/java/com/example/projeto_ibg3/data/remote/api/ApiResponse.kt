package com.example.projeto_ibg3.data.remote.api

import com.google.gson.annotations.SerializedName
import java.time.LocalDateTime

data class ApiResponse<T>(
    @SerializedName("success")
    val success: Boolean,

    @SerializedName("data")
    val data: T? = null,

    @SerializedName("message")
    val message: String? = null,

    @SerializedName("error")
    val error: String? = null,

    @SerializedName("timestamp")
    val timestamp: String? = null
)
