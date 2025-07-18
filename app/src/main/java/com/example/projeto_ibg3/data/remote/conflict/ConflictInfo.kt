package com.example.projeto_ibg3.data.remote.conflict

import java.util.UUID

data class ConflictInfo(
    val pacienteId: Long,
    val localVersion: Long,
    val serverVersion: Long,
    val conflictType: ConflictType,
    val localId: String,
    val serverId: Long?,
    val lastLocalUpdate: Long,
    val lastServerUpdate: Long,
    val conflictFields: List<String> = emptyList(),

    // Dados para resolução
    val localData: Map<String, Any>,
    val serverData: Map<String, Any>,
    val conflictId: String = UUID.randomUUID().toString(),
    val detectedAt: Long = System.currentTimeMillis()
)
