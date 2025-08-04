package com.example.projeto_ibg3.sync.batch

//Resultado de operação em lote
data class BatchResult<T>(
    val successful: List<T> = emptyList(),
    val failed: List<BatchError<T>> = emptyList(),
    val conflicts: List<BatchConflict<T>> = emptyList()
)
