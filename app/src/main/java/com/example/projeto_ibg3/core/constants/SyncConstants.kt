package com.example.projeto_ibg3.core.constants

/**
 * Constantes para sincronização de dados
 */
object SyncConstants {

    // Tipos de entidade
    object EntityTypes {
        const val PACIENTE = "PACIENTE"
        const val ESPECIALIDADE = "ESPECIALIDADE"
        const val PACIENTE_ESPECIALIDADE = "PACIENTE_ESPECIALIDADE"
    }

    // Operações de sincronização
    object Operations {
        const val CREATE = "CREATE"
        const val INSERT = "INSERT"
        const val UPDATE = "UPDATE"
        const val DELETE = "DELETE"
    }

    // Status de sincronização
    object Status {
        const val PENDING = "PENDING"
        const val SYNCED = "SYNCED"
        const val ERROR = "ERROR"
    }

    // Códigos de erro
    object ErrorCodes {
        const val NETWORK_ERROR = "NETWORK_ERROR"
        const val SERVER_ERROR = "SERVER_ERROR"
        const val VALIDATION_ERROR = "VALIDATION_ERROR"
        const val CONFLICT_ERROR = "CONFLICT_ERROR"
        const val UNKNOWN_ERROR = "UNKNOWN_ERROR"
    }

    // Configurações
    object Config {
        const val MAX_RETRY_COUNT = 3
        const val SYNC_TIMEOUT_MS = 30000L
        const val RETRY_DELAY_MS = 5000L
    }
}