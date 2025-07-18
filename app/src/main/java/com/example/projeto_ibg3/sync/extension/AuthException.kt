package com.example.projeto_ibg3.sync.extension

/**
 * Exceção lançada quando há problemas de autenticação durante a sincronização
 * Esta exceção não deve ser retriada automaticamente
 */
class AuthException(
    message: String,
    cause: Throwable? = null,
    val errorCode: String? = null
) : Exception(message, cause) {

    companion object {
        const val INVALID_TOKEN = "INVALID_TOKEN"
        const val EXPIRED_TOKEN = "EXPIRED_TOKEN"
        const val UNAUTHORIZED = "UNAUTHORIZED"
        const val FORBIDDEN = "FORBIDDEN"
    }

    constructor(errorCode: String, message: String) : this(message, null, errorCode)
}