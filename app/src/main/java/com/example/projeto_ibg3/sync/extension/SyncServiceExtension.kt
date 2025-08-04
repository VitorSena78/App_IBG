package com.example.projeto_ibg3.sync.extension

import com.example.projeto_ibg3.sync.model.SyncError

fun Exception.toSyncError(): SyncError {
    return when (this) {
        is java.net.UnknownHostException ->
            SyncError.NetworkError("Sem conexão com a internet")
        is java.net.SocketTimeoutException ->
            SyncError.NetworkError("Timeout na conexão")
        is java.net.ConnectException ->
            SyncError.NetworkError("Falha na conexão")
        is java.io.IOException ->
            SyncError.NetworkError("Erro de rede: ${this.message}")
        is retrofit2.HttpException -> {
            when (this.code()) {
                401 -> SyncError.AuthError("Não autorizado")
                403 -> SyncError.AuthError("Acesso negado")
                404 -> SyncError.ServerError("Recurso não encontrado", 404)
                409 -> SyncError.ConflictError(emptyList()) // Você pode melhorar isso
                422 -> SyncError.ValidationError(emptyList()) // Você pode melhorar isso
                500 -> SyncError.ServerError("Erro interno do servidor", 500)
                502, 503, 504 -> SyncError.ServerError("Servidor indisponível", this.code())
                else -> SyncError.ServerError("Erro do servidor: ${this.message}", this.code())
            }
        }
        is com.google.gson.JsonSyntaxException ->
            SyncError.ParseError("Erro ao analisar resposta do servidor")
        else -> SyncError.UnknownError(
            message = this.message ?: "Erro desconhecido",
            cause = this
        )
    }
}