package com.example.projeto_ibg3.data.remote.dto

import com.example.projeto_ibg3.model.Paciente
import java.util.Date

//Função para converter DTO para modelo de domínio
//Conversões DTO para Model
fun PacienteDto.toPaciente(): Paciente {
    return Paciente(
        id = 0,
        serverId = this.id,
        nome = this.nome?.takeIf { it.isNotBlank() } ?: "Nome não informado",
        nomeDaMae = this.nomeDaMae?.takeIf { it.isNotBlank() } ?: "Não informado",
        dataNascimento = this.dataNascimento?.let { parseDate(it) } ?: Date(),
        idade = this.idade,
        cpf = this.cpf?.takeIf { it.isNotBlank() } ?: "",
        sus = this.sus?.takeIf { it.isNotBlank() } ?: "",
        telefone = this.telefone?.takeIf { it.isNotBlank() } ?: "",
        endereco = this.endereco?.takeIf { it.isNotBlank() } ?: "",
        // resto permanece igual...
        pressaoArterial = this.paXMmhg,
        frequenciaCardiaca = this.fcBpm,
        frequenciaRespiratoria = this.frIbpm,
        temperatura = this.temperaturaC,
        glicemia = this.hgtMgld,
        saturacaoOxigenio = this.spo2,
        peso = this.peso,
        altura = this.altura,
        imc = this.imc
    )
}

fun Paciente.toDto(): PacienteDto {
    return PacienteDto(
        id = this.serverId ?: 0,
        nome = this.nome,
        nomeDaMae = this.nomeDaMae,
        dataNascimento = this.dataNascimento.let { formatDate(it) },
        idade = this.idade,
        cpf = this.cpf,
        sus = this.sus,
        telefone = this.telefone,
        endereco = this.endereco,
        paXMmhg = this.pressaoArterial,
        fcBpm = this.frequenciaCardiaca,
        frIbpm = this.frequenciaRespiratoria,
        temperaturaC = this.temperatura,
        hgtMgld = this.glicemia,
        spo2 = this.saturacaoOxigenio,
        peso = this.peso,
        altura = this.altura,
        imc = this.imc
    )
}

// Utilitários para conversão de data
private fun parseDate(dateString: String): Date? {
    return try {
        val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        formatter.parse(dateString)
    } catch (e: Exception) {
        null
    }
}

private fun formatDate(date: Date): String {
    val formatter = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    return formatter.format(date)
}