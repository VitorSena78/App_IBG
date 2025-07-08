package com.example.projeto_ibg3.data.remote

data class SyncInfo(
    val lastModified: Long,
    val totalPacientes: Int,
    val serverVersion: String
)
