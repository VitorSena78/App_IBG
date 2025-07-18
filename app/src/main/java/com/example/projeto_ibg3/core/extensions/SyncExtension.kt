package com.example.projeto_ibg3.core.extensions

import com.example.projeto_ibg3.sync.model.SyncError
import com.example.projeto_ibg3.sync.model.SyncResult

// Extensões úteis
fun SyncResult.isSuccess(): Boolean = this is SyncResult.SUCCESS

fun SyncResult.getSuccessData(): SyncResult.SUCCESS? = this as? SyncResult.SUCCESS

fun SyncResult.getErrorData(): SyncResult.ERROR? = this as? SyncResult.ERROR

fun SyncResult.getMessage(): String? = when (this) {
    is SyncResult.SUCCESS -> message
    is SyncResult.ERROR -> when (error) {
        is SyncError.NetworkError -> "Erro de rede: ${error.message}"
        is SyncError.ServerError -> "Erro do servidor: ${error.message}"
        is SyncError.ParseError -> "Erro de parsing: ${error.message}"
        is SyncError.AuthError -> "Erro de autenticação: ${error.message}"
        is SyncError.ConflictError -> "Conflitos detectados: ${error.conflicts.size} itens"
        is SyncError.ValidationError -> "Erros de validação: ${error.errors.size} itens"
        is SyncError.UnknownError -> "Erro desconhecido: ${error.message}"
    }
    is SyncResult.NO_NETWORK -> "Sem conexão de rede"
    is SyncResult.InProgress -> "Sincronização em andamento"
}

