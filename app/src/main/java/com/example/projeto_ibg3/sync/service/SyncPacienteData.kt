package com.example.projeto_ibg3.sync.service

data class SyncPacienteData(
    val serverId: Long?,
    val localId: String,
    val nome: String,
    val dataNascimento: String?,
    val idade: Int?,
    val nomeDaMae: String?,
    val cpf: String,
    val sus: String?,
    val telefone: String?,
    val endereco: String?,
    val especialidadeIds: List<Int>,
    val lastSyncTimestamp: Long
)
