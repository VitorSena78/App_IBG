package com.example.projeto_ibg3.data.entity

import com.example.projeto_ibg3.model.SyncStatus
import com.example.projeto_ibg3.model.Paciente
import java.util.Date

fun PacienteEntity.toPaciente(): Paciente {
    return Paciente(
        id = this.id,
        serverId = this.serverId,
        nome = this.nome,
        dataNascimento = Date(this.dataNascimento),
        idade = this.idade,
        nomeDaMae = this.nomeDaMae,
        cpf = this.cpf,
        sus = this.sus,
        telefone = this.telefone,
        endereco = this.endereco,
        pressaoArterial = this.pressaoArterial,
        frequenciaCardiaca = this.frequenciaCardiaca,
        frequenciaRespiratoria = this.frequenciaRespiratoria,
        temperatura = this.temperatura,
        glicemia = this.glicemia,
        saturacaoOxigenio = this.saturacaoOxigenio,
        peso = this.peso,
        altura = this.altura,
        imc = this.imc
    )
}

fun Paciente.toEntity(): PacienteEntity {
    return PacienteEntity(
        id = 0, // Sempre 0 para novos registros (autoGenerate)
        serverId = this.serverId,
        nome = this.nome,
        dataNascimento = this.dataNascimento.time,
        idade = this.idade ?: this.calcularIdade,
        nomeDaMae = this.nomeDaMae,
        cpf = this.cpf,
        sus = this.sus,
        telefone = this.telefone,
        endereco = this.endereco,
        pressaoArterial = this.pressaoArterial,
        frequenciaCardiaca = this.frequenciaCardiaca,
        frequenciaRespiratoria = this.frequenciaRespiratoria,
        temperatura = this.temperatura,
        glicemia = this.glicemia,
        saturacaoOxigenio = this.saturacaoOxigenio,
        peso = this.peso,
        altura = this.altura,
        imc = this.imc ?: this.calcularImc,
        syncStatus = SyncStatus.PENDING_UPLOAD,
        lastModified = System.currentTimeMillis(),
        isDeleted = false
    )
}

// Extensão para atualizar uma entidade existente
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
        lastModified = System.currentTimeMillis()
    )
}