package com.example.projeto_ibg3.data.dao

// Classe para resultado da query complexa
data class PacienteComEspecialidadeResult(
    val id: Long,
    val serverId: Long?,
    val nome: String,
    val dataNascimento: java.util.Date?,
    val idade: Int?,
    val nomeDaMae: String?,
    val cpf: String?,
    val sus: String?,
    val telefone: String?,
    val endereco: String?,
    val paXMmhg: String?,
    val fcBpm: Float?,
    val frIbpm: Float?,
    val temperaturaC: Float?,
    val hgtMgld: Float?,
    val spo2: Float?,
    val peso: Float?,
    val altura: Float?,
    val imc: Float?,
    val especialidadesNomes: String?,
    val especialidadesIds: String?
)