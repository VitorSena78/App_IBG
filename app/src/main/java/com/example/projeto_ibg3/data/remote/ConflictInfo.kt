package com.example.projeto_ibg3.data.remote

data class ConflictInfo(
    val pacienteId: Long,
    val localVersion: Long,
    val serverVersion: Long,
    val conflictType: ConflictType
)
