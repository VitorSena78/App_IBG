package com.example.projeto_ibg3.domain.model

import com.example.projeto_ibg3.data.remote.dto.SyncType

data class SyncState(
    val isLoading: Boolean = false,
    val message: String = "",
    val error: String? = null,
    val lastSyncTime: Long = 0,
    val totalItems: Int = 0,
    val processedItems: Int = 0,

    // Propriedades adicionais necessárias
    val isSyncing: Boolean = isLoading, // Pode ser um alias para isLoading ou lógica própria
    val isComplete: Boolean = false,
    val hasChanges: Boolean = false
) {
    // Propriedades computadas opcionais
    val progress: Float
        get() = if (totalItems > 0) processedItems.toFloat() / totalItems else 0f

    val isSuccess: Boolean
        get() = isComplete && error == null

    val hasError: Boolean
        get() = error != null
}