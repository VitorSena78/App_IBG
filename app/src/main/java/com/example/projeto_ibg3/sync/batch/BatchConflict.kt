package com.example.projeto_ibg3.sync.batch

import com.example.projeto_ibg3.data.remote.conflict.ConflictType

// Conflito em operação em lote
data class BatchConflict<T>(
    val localItem: T,
    val serverItem: T,
    val conflictType: ConflictType
)
