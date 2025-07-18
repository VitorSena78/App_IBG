package com.example.projeto_ibg3.sync.batch

//Erro em operação em lote
data class BatchError<T>(
    val item: T,
    val error: String,
    val exception: Exception? = null
)
