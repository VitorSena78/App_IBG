package com.example.projeto_ibg3.sync.model

data class SyncInfo(
    val lastModified: Long,
    val totalPacientes: Int,
    val serverVersion: String
)
