package com.example.projeto_ibg3.sync.extension

import android.util.Log

/**
 * Utilitário para classificar exceções e decidir estratégias de retry
 */
object ExceptionHandler {

    /**
     * Determina se uma exceção deve ser retriada
     */
    fun shouldRetry(exception: Exception): Boolean {
        return when (exception) {
            // Nunca retriar estas exceções
            is AuthException,
            is ValidationException,
            is SyncException.DataCorruptionException -> false

            // Retriar exceções de rede
            is NetworkException -> exception.isRetriable

            // Retriar exceções de servidor temporariamente indisponível
            is SyncException.ServerUnavailableException -> true

            // Avaliar outros tipos de exceção
            else -> isRetriableException(exception)
        }
    }

    /**
     * Classifica exceções genéricas
     */
    private fun isRetriableException(exception: Exception): Boolean {
        return when {
            // Exceções de conectividade
            exception.message?.contains("timeout", ignoreCase = true) == true -> true
            exception.message?.contains("connection", ignoreCase = true) == true -> true
            exception.message?.contains("network", ignoreCase = true) == true -> true

            // Códigos de erro HTTP retriáveis (se você estiver usando)
            exception.message?.contains("500") == true -> true // Internal Server Error
            exception.message?.contains("502") == true -> true // Bad Gateway
            exception.message?.contains("503") == true -> true // Service Unavailable
            exception.message?.contains("504") == true -> true // Gateway Timeout

            else -> false
        }
    }

    /**
     * Loga exceções com contexto apropriado
     */
    fun logException(exception: Exception, context: String) {
        when (exception) {
            is AuthException -> Log.w("SyncAuth", "Authentication error in $context: ${exception.message}")
            is ValidationException -> Log.w("SyncValidation", "Validation error in $context: ${exception.message}")
            is NetworkException -> Log.i("SyncNetwork", "Network error in $context: ${exception.message}")
            is SyncException -> Log.e("SyncError", "Sync error in $context: ${exception.message}")
            else -> Log.e("SyncGeneric", "Generic error in $context: ${exception.message}")
        }
    }
}