package com.example.projeto_ibg3.sync.model

import com.example.projeto_ibg3.data.remote.dto.ConflictDto
import com.example.projeto_ibg3.data.remote.dto.SyncErrorDto

sealed class SyncError {
    data class NetworkError(val message: String, val code: Int? = null) : SyncError()
    data class ServerError(val message: String, val code: Int) : SyncError()
    data class ParseError(val message: String) : SyncError()
    data class AuthError(val message: String) : SyncError()
    data class ConflictError(val conflicts: List<ConflictDto>) : SyncError()
    data class ValidationError(val errors: List<SyncErrorDto>) : SyncError()
    data class UnknownError(val message: String, val cause: Throwable? = null) : SyncError()
}