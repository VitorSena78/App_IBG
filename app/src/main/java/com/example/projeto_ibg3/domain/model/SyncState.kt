package com.example.projeto_ibg3.domain.model

import com.example.projeto_ibg3.data.remote.dto.SyncType

data class SyncState(
    val isLoading: Boolean = false,
    val message: String = "",
    val error: String? = null,
    val lastSyncTime: Long = 0,
    val totalItems: Int = 0,
    val processedItems: Int = 0
)
