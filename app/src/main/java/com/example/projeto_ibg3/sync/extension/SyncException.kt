package com.example.projeto_ibg3.sync.extension

/**
 * Exceção base para problemas de sincronização
 */
sealed class SyncException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

    /**
     * Erro de conflito de dados (ex: versão desatualizada)
     */
    class ConflictException(
        message: String,
        cause: Throwable? = null,
        val conflictData: Any? = null
    ) : SyncException(message, cause)

    /**
     * Erro quando o servidor está temporariamente indisponível
     */
    class ServerUnavailableException(
        message: String,
        cause: Throwable? = null
    ) : SyncException(message, cause)

    /**
     * Erro quando os dados locais estão corrompidos
     */
    class DataCorruptionException(
        message: String,
        cause: Throwable? = null
    ) : SyncException(message, cause)
}