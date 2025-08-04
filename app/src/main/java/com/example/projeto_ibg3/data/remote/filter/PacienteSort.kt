package com.example.projeto_ibg3.data.remote.filter

// Opções de ordenação
data class PacienteSort(
    val field: SortField = SortField.NOME,
    val direction: SortDirection = SortDirection.ASC
)
