package com.example.projeto_ibg3.data.mappers

import com.example.projeto_ibg3.data.local.database.entities.PacienteEntity
import com.example.projeto_ibg3.domain.model.Paciente
import com.example.projeto_ibg3.domain.model.SyncStatus

/**
 * Extensões específicas para validação e sincronização de Paciente
 * Separadas do mapper principal para melhor organização
 */

// ========== VALIDAÇÃO ==========

// Valida se o PacienteEntity tem dados obrigatórios
fun PacienteEntity.isValid(): Boolean {
    return nome.isNotBlank() &&
            nomeDaMae.isNotBlank() &&
            cpf.isNotBlank() &&
            sus.isNotBlank()
}

//Valida se o Paciente tem dados obrigatórios
fun Paciente.isValid(): Boolean {
    return nome.isNotBlank() &&
            nomeDaMae.isNotBlank() &&
            cpf.isNotBlank() &&
            sus.isNotBlank()
}

//Filtra apenas entidades válidas de uma lista
@JvmName("onlyValidPacienteEntity")
fun List<PacienteEntity>.onlyValid(): List<PacienteEntity> = filter { it.isValid() }

//Filtra apenas pacientes válidos de uma lista
@JvmName("onlyValidPaciente")
fun List<Paciente>.onlyValid(): List<Paciente> = filter { it.isValid() }

// ========== COMPARAÇÃO DE DADOS ==========

//Verifica se dois PacienteEntity são iguais nos campos de negócio
fun PacienteEntity.isDataEqual(other: PacienteEntity): Boolean {
    return this.nome == other.nome &&
            this.dataNascimento == other.dataNascimento &&
            this.idade == other.idade &&
            this.nomeDaMae == other.nomeDaMae &&
            this.cpf == other.cpf &&
            this.sus == other.sus &&
            this.telefone == other.telefone &&
            this.endereco == other.endereco &&
            this.pressaoArterial == other.pressaoArterial &&
            this.frequenciaCardiaca == other.frequenciaCardiaca &&
            this.frequenciaRespiratoria == other.frequenciaRespiratoria &&
            this.temperatura == other.temperatura &&
            this.glicemia == other.glicemia &&
            this.saturacaoOxigenio == other.saturacaoOxigenio &&
            this.peso == other.peso &&
            this.altura == other.altura &&
            this.imc == other.imc
}

// ========== SINCRONIZAÇÃO ==========

//Cria uma cópia do PacienteEntity com novos dados de sincronização
fun PacienteEntity.copyWithSyncData(
    syncStatus: SyncStatus = this.syncStatus,
    serverId: Long? = this.serverId,
    lastSyncTimestamp: Long = System.currentTimeMillis(),
    syncAttempts: Int = this.syncAttempts,
    syncError: String? = this.syncError
): PacienteEntity {
    return this.copy(
        syncStatus = syncStatus,
        serverId = serverId,
        lastSyncTimestamp = lastSyncTimestamp,
        syncAttempts = syncAttempts,
        syncError = syncError,
        updatedAt = System.currentTimeMillis()
    )
}

//Verifica se dois PacienteEntity têm conflito de dados
fun PacienteEntity.hasConflictWith(other: PacienteEntity): Boolean {
    return this.updatedAt != other.updatedAt &&
            this.version != other.version &&
            !this.isDataEqual(other)
}

//Resolve conflito automaticamente baseado em timestamp
fun PacienteEntity.resolveConflictWith(other: PacienteEntity): PacienteEntity {
    return if (this.updatedAt >= other.updatedAt) {
        this
    } else {
        other
    }
}

// ========== ATUALIZAÇÃO DE DADOS ==========

//Atualiza PacienteEntity com dados do Paciente (model)
fun PacienteEntity.updateFrom(paciente: Paciente): PacienteEntity {
    return this.copy(
        nome = paciente.nome,
        dataNascimento = paciente.dataNascimento.time,
        idade = paciente.idade ?: paciente.calcularIdade,
        nomeDaMae = paciente.nomeDaMae,
        cpf = paciente.cpf,
        sus = paciente.sus,
        telefone = paciente.telefone,
        endereco = paciente.endereco,
        pressaoArterial = paciente.pressaoArterial,
        frequenciaCardiaca = paciente.frequenciaCardiaca,
        frequenciaRespiratoria = paciente.frequenciaRespiratoria,
        temperatura = paciente.temperatura,
        glicemia = paciente.glicemia,
        saturacaoOxigenio = paciente.saturacaoOxigenio,
        peso = paciente.peso,
        altura = paciente.altura,
        imc = paciente.imc ?: paciente.calcularImc,
        syncStatus = SyncStatus.PENDING_UPLOAD,
        updatedAt = System.currentTimeMillis(),
        version = this.version + 1
    )
}

// ========== DEBUG ==========

//Converte PacienteEntity para string amigável para debug
fun PacienteEntity.toDebugString(): String {
    return """
        PacienteEntity(
            localId='$localId',
            serverId=$serverId,
            nome='$nome',
            cpf='$cpf',
            sus='$sus',
            syncStatus=$syncStatus,
            version=$version,
            isDeleted=$isDeleted,
            syncAttempts=$syncAttempts,
            lastSyncTimestamp=$lastSyncTimestamp
        )
    """.trimIndent()
}

//Converte Paciente para string amigável para debug
fun Paciente.toDebugString(): String {
    return """
        Paciente(
            localId='$localId',
            serverId=$serverId,
            nome='$nome',
            cpf='$cpf',
            sus='$sus',
            syncStatus=$syncStatus,
            version=$version
        )
    """.trimIndent()
}

// ========== EXTENSÕES PARA CÁLCULOS ==========

//Calcula idade baseada na data de nascimento
val Paciente.calcularIdade: Int
    get() = try {
        val hoje = java.util.Calendar.getInstance()
        val nascimento = java.util.Calendar.getInstance().apply {
            time = this@calcularIdade.dataNascimento
        }

        var idade = hoje.get(java.util.Calendar.YEAR) - nascimento.get(java.util.Calendar.YEAR)

        // Ajusta se ainda não fez aniversário este ano
        if (hoje.get(java.util.Calendar.DAY_OF_YEAR) < nascimento.get(java.util.Calendar.DAY_OF_YEAR)) {
            idade--
        }

        maxOf(0, idade) // Garante que não retorne idade negativa
    } catch (e: Exception) {
        0
    }

//Calcula IMC baseado no peso e altura
val Paciente.calcularImc: Double?
    get() = try {
        val pesoValue = this.peso
        val alturaValue = this.altura

        if (pesoValue != null && alturaValue != null && alturaValue > 0) {
            val alturaMetros = alturaValue / 100.0 // Converte cm para metros
            val imc = pesoValue / (alturaMetros * alturaMetros)

            // Arredonda para 2 casas decimais
            Math.round(imc * 100.0) / 100.0
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }

// ========== EXTENSÕES PARA VALIDAÇÃO ==========

//Valida se o CPF está em formato válido
fun String.isValidCpf(): Boolean {
    val cpf = this.replace(Regex("[^0-9]"), "")

    if (cpf.length != 11) return false
    if (cpf.all { it == cpf[0] }) return false // Todos os dígitos iguais

    // Validação dos dígitos verificadores
    fun calcularDigito(cpf: String, posicoes: IntRange): Int {
        var soma = 0
        var multiplicador = posicoes.last + 1

        for (i in posicoes) {
            soma += cpf[i].toString().toInt() * multiplicador--
        }

        val resto = soma % 11
        return if (resto < 2) 0 else 11 - resto
    }

    val digito1 = calcularDigito(cpf, 0..8)
    val digito2 = calcularDigito(cpf, 0..9)

    return cpf[9].toString().toInt() == digito1 && cpf[10].toString().toInt() == digito2
}

// Valida se o SUS está em formato válido (básico)
fun String.isValidSus(): Boolean {
    val sus = this.replace(Regex("[^0-9]"), "")
    return sus.length == 15 && sus.all { it.isDigit() }
}

//Formata CPF para exibição
fun String.formatCpf(): String {
    val cpf = this.replace(Regex("[^0-9]"), "")
    return if (cpf.length == 11) {
        "${cpf.substring(0, 3)}.${cpf.substring(3, 6)}.${cpf.substring(6, 9)}-${cpf.substring(9, 11)}"
    } else {
        this
    }
}

//Formata SUS para exibição
fun String.formatSus(): String {
    val sus = this.replace(Regex("[^0-9]"), "")
    return if (sus.length == 15) {
        "${sus.substring(0, 3)} ${sus.substring(3, 7)} ${sus.substring(7, 11)} ${sus.substring(11, 15)}"
    } else {
        this
    }
}