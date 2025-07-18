package com.example.projeto_ibg3.sync.extension

/**
 * Exceção para problemas de rede que podem ser retriados
 */
class NetworkException(
    message: String,
    cause: Throwable? = null,
    val isRetriable: Boolean = true
) : Exception(message, cause)
