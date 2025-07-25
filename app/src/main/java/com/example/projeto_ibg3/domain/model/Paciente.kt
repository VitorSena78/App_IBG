package com.example.projeto_ibg3.domain.model

import java.util.Calendar
import java.util.Date

// Data class para representar um paciente
data class Paciente(
    val localId: String,
    val serverId: Long? = null,
    val nome: String,
    val dataNascimento: Date,  //mudar para Long futuramente
    val idade: Int? = null,
    val nomeDaMae: String?,
    val cpf: String,
    val sus: String?,
    val telefone: String?,
    val endereco: String?,
    // Dados médicos
    val pressaoArterial: String? = null,           // Pressão arterial no formato "120/80 mmHg"
    val frequenciaCardiaca: Float? = null,         // Frequência cardíaca em bpm
    val frequenciaRespiratoria: Float? = null,     // Frequência respiratória em ipm
    val temperatura: Float? = null,                // Temperatura corporal em °C
    val glicemia: Float? = null,                   // Glicemia capilar em mg/dL
    val saturacaoOxigenio: Float? = null,          // SpO2 em porcentagem
    val peso: Float? = null,                       // Peso corporal em kg
    val altura: Float? = null,                      // Altura em metros
    val imc: Float? = null,
    // Especialidades associadas
    val especialidades: List<Especialidade> = emptyList(),
    //Dados de controle
    val createdAt: Date,  //mudar para Long futuramente
    val updatedAt: Date,  //mudar para Long futuramente
    // Campo de sincronização:
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val version: Int = 1
) {


    // Propriedade calculada para idade
    val calcularIdade: Int
        get() {
            val calendar = Calendar.getInstance()
            val currentYear = calendar.get(Calendar.YEAR)
            calendar.time = dataNascimento
            val birthYear = calendar.get(Calendar.YEAR)
            return currentYear - birthYear
        }

    // Propriedade calculada para IMC
    val calcularImc: Float?
        get() = if (peso != null && altura != null && altura > 0) {
            peso / (altura * altura)
        } else null

    // Propriedade para obter iniciais do nome
    val iniciais: String
        get() {
            val nomes = nome.split(" ")
            return if (nomes.size >= 2) {
                "${nomes[0].first()}${nomes[1].first()}".uppercase()
            } else {
                nome.take(2).uppercase()
            }
        }

    // Formatação do CPF
    val cpfFormatado: String
        get() = if (cpf.length == 11) {
            "${cpf.substring(0, 3)}.${cpf.substring(3, 6)}.${cpf.substring(6, 9)}-${cpf.substring(9)}"
        } else cpf

    // Formatação do telefone
    val telefoneFormatado: String
        get() {
            val tel = telefone ?: return ""
            return when (tel.length) {
                11 -> "(${tel.substring(0, 2)}) ${tel.substring(2, 7)}-${tel.substring(7)}"
                10 -> "(${tel.substring(0, 2)}) ${tel.substring(2, 6)}-${tel.substring(6)}"
                else -> tel
            }
        }


    // Formatação da pressão arterial
    val pressaoArterialFormatada: String
        get() = pressaoArterial ?: "N/A"

    // Formatação da frequência cardíaca
    val frequenciaCardiacaFormatada: String
        get() = frequenciaCardiaca?.let { "${it.toInt()} bpm" } ?: "N/A"

    // Formatação da frequência respiratória
    val frequenciaRespiratoriaFormatada: String
        get() = frequenciaRespiratoria?.let { "${it.toInt()} ipm" } ?: "N/A"

    // Formatação da temperatura
    val temperaturaFormatada: String
        get() = temperatura?.let { String.format("%.1f °C", it) } ?: "N/A"

    // Formatação da glicemia
    val glicemiaFormatada: String
        get() = glicemia?.let { "${it.toInt()} mg/dL" } ?: "N/A"

    // Formatação da saturação de oxigênio
    val saturacaoOxigenioFormatada: String
        get() = saturacaoOxigenio?.let { "${it.toInt()} %" } ?: "N/A"

    // Formatação do peso
    val pesoFormatado: String
        get() = peso?.let { String.format("%.1f kg", it) } ?: "N/A"

    // Formatação da altura
    val alturaFormatada: String
        get() = altura?.let { String.format("%.2f m", it) } ?: "N/A"

    // Formatação do IMC
    val imcFormatado: String
        get() = calcularImc?.let { String.format("%.2f", it) } ?: "N/A"

    // Classificação do IMC
    val classificacaoImc: String
        get() = calcularImc?.let {
            when {
                it < 18.5 -> "Abaixo do peso"
                it < 25.0 -> "Peso normal"
                it < 30.0 -> "Sobrepeso"
                it < 35.0 -> "Obesidade grau I"
                it < 40.0 -> "Obesidade grau II"
                else -> "Obesidade grau III"
            }
        } ?: "N/A"

    // Função para criar um resumo dos sinais vitais
    fun resumoSinaisVitais(): String {
        val sinais = mutableListOf<String>()

        pressaoArterial?.let { sinais.add("PA: $it") }
        frequenciaCardiaca?.let { sinais.add("FC: ${it.toInt()} bpm") }
        frequenciaRespiratoria?.let { sinais.add("FR: ${it.toInt()} ipm") }
        temperatura?.let { sinais.add("T: ${String.format("%.1f", it)}°C") }
        glicemia?.let { sinais.add("HGT: ${it.toInt()} mg/dL") }
        saturacaoOxigenio?.let { sinais.add("SpO2: ${it.toInt()}%") }

        return if (sinais.isNotEmpty()) {
            sinais.joinToString(" | ")
        } else {
            "Sinais vitais não registrados"
        }
    }
}

