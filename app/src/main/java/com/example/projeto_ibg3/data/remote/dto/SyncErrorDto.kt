package com.example.projeto_ibg3.data.remote.dto

import com.google.gson.annotations.SerializedName

data class SyncErrorDto(
    @SerializedName("local_id")
    val localId: String,

    @SerializedName("error_code")
    val errorCode: String,

    @SerializedName("error_message")
    val errorMessage: String,

    @SerializedName("field_errors")
    val fieldErrors: Map<String, String> = emptyMap(),

    @SerializedName("retry_after")
    val retryAfter: Long? = null, // Tempo em ms para tentar novamente

    @SerializedName("is_permanent")
    val isPermanent: Boolean = false // Se true, não deve tentar novamente
) {
    fun getFormattedErrorMessage(): String {
        return if (fieldErrors.isNotEmpty()) {
            val fieldMessages = fieldErrors.entries.joinToString(", ") { "${it.key}: ${it.value}" }
            "$errorMessage. Erros de campo: $fieldMessages"
        } else {
            errorMessage
        }
    }
}