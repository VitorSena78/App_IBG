package com.example.projeto_ibg3.data.mappers

import com.example.projeto_ibg3.data.remote.dto.PacienteDto
import com.example.projeto_ibg3.domain.model.SyncStatus
import java.util.*

/**
 * Utilitários centralizados para mapeamento de Pacientes
 * Funcionalidades comuns usadas em diferentes pontos do sistema
 */
object PacienteMapperUtils {

    /**
     * Valores padrão para campos obrigatórios
     */
    private const val DEFAULT_NOME = "Nome não informado"
    private const val DEFAULT_MAE = "Não informado"
    private const val DEFAULT_STRING = ""

    /**
     * Sanitiza campos de string, aplicando valores padrão quando necessário
     */
    fun sanitizeString(value: String?, defaultValue: String = DEFAULT_STRING): String {
        return value?.takeIf { it.isNotBlank() } ?: defaultValue
    }

    /**
     * Sanitiza nome do paciente
     */
    fun sanitizeName(name: String?): String {
        return sanitizeString(name, DEFAULT_NOME)
    }

    /**
     * Sanitiza nome da mãe
     */
    fun sanitizeMotherName(name: String?): String {
        return sanitizeString(name, DEFAULT_MAE)
    }

    /**
     * Gera um novo local ID se não existir
     */
    fun generateOrKeepLocalId(existingId: String?): String {
        return existingId?.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
    }

    /**
     * Determina o sync status baseado na operação
     */
    fun determineSyncStatus(isFromServer: Boolean, isNewRecord: Boolean): SyncStatus {
        return when {
            isFromServer -> SyncStatus.SYNCED
            isNewRecord -> SyncStatus.PENDING_UPLOAD
            else -> SyncStatus.PENDING_UPLOAD
        }
    }

    /**
     * Converte valor nullable para timestamp ou usa current time
     */
    fun getTimestampOrCurrent(value: Long?): Long {
        return if (value != null && value > 0) value else System.currentTimeMillis()
    }

    /**
     * Aplica valores padrão para campos vitais
     */
    fun applyVitalDefaults(
        pressaoArterial: String?,
        frequenciaCardiaca: Int?,
        frequenciaRespiratoria: Int?,
        temperatura: Double?,
        glicemia: Int?,
        saturacaoOxigenio: Int?,
        peso: Double?,
        altura: Double?,
        imc: Double?
    ): VitalSigns {
        return VitalSigns(
            pressaoArterial = pressaoArterial,
            frequenciaCardiaca = frequenciaCardiaca,
            frequenciaRespiratoria = frequenciaRespiratoria,
            temperatura = temperatura,
            glicemia = glicemia,
            saturacaoOxigenio = saturacaoOxigenio,
            peso = peso,
            altura = altura,
            imc = imc
        )
    }

    /**
     * Data class para agrupar sinais vitais
     */
    data class VitalSigns(
        val pressaoArterial: String?,
        val frequenciaCardiaca: Int?,
        val frequenciaRespiratoria: Int?,
        val temperatura: Double?,
        val glicemia: Int?,
        val saturacaoOxigenio: Int?,
        val peso: Double?,
        val altura: Double?,
        val imc: Double?
    )

    /**
     * Cria configuração padrão para entity
     */
    fun createDefaultEntityConfig(
        deviceId: String,
        syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD,
        isDeleted: Boolean = false
    ): EntityConfig {
        return EntityConfig(
            deviceId = deviceId,
            syncStatus = syncStatus,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            lastSyncTimestamp = 0,
            isDeleted = isDeleted,
            version = 1,
            syncAttempts = 0,
            syncError = null
        )
    }

    /**
     * Data class para configuração de entity
     */
    data class EntityConfig(
        val deviceId: String,
        val syncStatus: SyncStatus,
        val createdAt: Long,
        val updatedAt: Long,
        val lastSyncTimestamp: Long,
        val isDeleted: Boolean,
        val version: Int,
        val syncAttempts: Int,
        val syncError: String?
    )

    /**
     * Merge dados de dois DTOs (útil para resolução de conflitos)
     */
    fun mergeDtoData(primary: PacienteDto, secondary: PacienteDto): PacienteDto {
        return primary.copy(
            nome = primary.nome ?: secondary.nome,
            dataNascimento = primary.dataNascimento ?: secondary.dataNascimento,
            idade = primary.idade ?: secondary.idade,
            nomeDaMae = primary.nomeDaMae ?: secondary.nomeDaMae,
            cpf = primary.cpf ?: secondary.cpf,
            sus = primary.sus ?: secondary.sus,
            telefone = primary.telefone ?: secondary.telefone,
            endereco = primary.endereco ?: secondary.endereco,
            paXMmhg = primary.paXMmhg ?: secondary.paXMmhg,
            fcBpm = primary.fcBpm ?: secondary.fcBpm,
            frIbpm = primary.frIbpm ?: secondary.frIbpm,
            temperaturaC = primary.temperaturaC ?: secondary.temperaturaC,
            hgtMgld = primary.hgtMgld ?: secondary.hgtMgld,
            spo2 = primary.spo2 ?: secondary.spo2,
            peso = primary.peso ?: secondary.peso,
            altura = primary.altura ?: secondary.altura,
            imc = primary.imc ?: secondary.imc,
            updatedAt = listOfNotNull(primary.updatedAt, secondary.updatedAt).max()
        )
    }

    /**
     * Verifica se o paciente tem dados mínimos necessários
     */
    fun hasMinimalData(nome: String?, nomeDaMae: String?, cpf: String?, sus: String?): Boolean {
        return !nome.isNullOrBlank() &&
                !nomeDaMae.isNullOrBlank() &&
                !cpf.isNullOrBlank() &&
                !sus.isNullOrBlank()
    }
}