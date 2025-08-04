package com.example.projeto_ibg3.core.extensions

import com.example.projeto_ibg3.data.local.database.dao.EspecialidadeDao
import com.example.projeto_ibg3.data.local.database.dao.PacienteDao
import com.example.projeto_ibg3.data.local.database.dao.PacienteEspecialidadeDao
import com.example.projeto_ibg3.data.local.database.entities.EspecialidadeEntity
import com.example.projeto_ibg3.data.local.database.entities.PacienteEntity
import com.example.projeto_ibg3.data.local.database.entities.PacienteEspecialidadeEntity
import com.example.projeto_ibg3.data.remote.conflict.ConflictResolution
import com.example.projeto_ibg3.domain.model.SyncStatus
import java.util.UUID

// ========== EXTENSÕES PARA ESPECIALIDADE DAO ==========

/**
 * Método para deletar todas as especialidades (equivalente ao deleteAllEspecialidades)
 */
suspend fun EspecialidadeDao.deleteAllEspecialidades() {
    // Primeiro fazer soft delete
    val allEspecialidades = getAllEspecialidadesAsList()
    allEspecialidades.forEach { especialidade ->
        val deleted = especialidade.copy(
            isDeleted = true,
            syncStatus = SyncStatus.PENDING_DELETE,
            updatedAt = getCurrentTimestamp()
        )
        updateEspecialidade(deleted)
    }
}

/**
 * Método para inserir especialidades substituindo existentes
 */
suspend fun EspecialidadeDao.insertEspecialidadesReplace(especialidades: List<EspecialidadeEntity>) {
    // Usar o método existente insertEspecialidades que já tem OnConflictStrategy.REPLACE
    insertEspecialidades(especialidades)
}

/**
 * Método para buscar todas as especialidades como lista (não Flow)
 */
suspend fun EspecialidadeDao.getAllEspecialidadesAsList(): List<EspecialidadeEntity> {
    return getEspecialidadesByStatus(SyncStatus.SYNCED) +
            getEspecialidadesByStatus(SyncStatus.PENDING_UPLOAD) +
            getEspecialidadesByStatus(SyncStatus.PENDING_DELETE)
}

// ========== EXTENSÕES PARA PACIENTE DAO ==========

/**
 * Método para sincronizar pacientes (equivalente ao syncPacientes)
 */
suspend fun PacienteDao.syncPacientes(pacientes: List<PacienteEntity>) {
    insertPacientes(pacientes)
}

/**
 * Método para buscar pacientes que precisam ser sincronizados
 */
suspend fun PacienteDao.getPacientesForSync(): List<PacienteEntity> {
    return getItemsNeedingSync()
}

/**
 * MÉTODO CORRIGIDO: Resolução de conflitos usando os métodos específicos do seu DAO
 */
suspend fun PacienteDao.resolveConflict(
    localId: String,
    resolution: ConflictResolution,
    serverData: PacienteEntity? = null
): Boolean {
    return try {
        when (resolution) {
            ConflictResolution.KEEP_LOCAL -> {
                // Usar método específico do seu DAO
                updateSyncStatusByLocalId(localId, SyncStatus.PENDING_UPLOAD)
                true
            }

            ConflictResolution.KEEP_SERVER -> {
                serverData?.let { serverPaciente ->
                    // Atualizar com dados do servidor
                    val updatedPaciente = serverPaciente.copy(
                        localId = localId,
                        syncStatus = SyncStatus.SYNCED,
                        updatedAt = getCurrentTimestamp(),
                        lastSyncTimestamp = getCurrentTimestamp()
                    )
                    updatePaciente(updatedPaciente)

                    // Se servidor tem ID, usar método específico
                    serverPaciente.serverId?.let { serverId ->
                        updateSyncStatusAndServerId(localId, SyncStatus.SYNCED, serverId)
                    }
                } ?: updateSyncStatusByLocalId(localId, SyncStatus.SYNCED)
                true
            }

            ConflictResolution.MERGE_AUTOMATIC -> {
                val localPaciente = getPacienteByLocalId(localId)
                if (localPaciente != null && serverData != null) {
                    val mergedPaciente = performAutomaticMerge(localPaciente, serverData)
                    updatePaciente(mergedPaciente)

                    // Se servidor tem ID, atualizar também
                    serverData.serverId?.let { serverId ->
                        updateSyncStatusAndServerId(localId, SyncStatus.SYNCED, serverId)
                    }
                } else {
                    updateSyncStatusByLocalId(localId, SyncStatus.SYNCED)
                }
                true
            }

            ConflictResolution.MERGE_MANUAL -> {
                // Dados já foram mesclados manualmente
                updateSyncStatusByLocalId(localId, SyncStatus.SYNCED)
                true
            }

            ConflictResolution.MANUAL -> {
                // Usar método específico para marcar conflito
                val localPaciente = getPacienteByLocalId(localId)
                if (localPaciente != null && serverData != null) {
                    val conflictJson = createConflictJson(localPaciente, serverData)
                    markAsConflict(localId, conflictJson)
                } else {
                    updateSyncStatusByLocalId(localId, SyncStatus.CONFLICT)
                }
                false
            }
        }
    } catch (e: Exception) {
        // Usar método específico para incrementar tentativas
        incrementSyncAttempts(localId, getCurrentTimestamp(), e.message)
        updateSyncStatusByLocalId(localId, SyncStatus.UPLOAD_FAILED)
        false
    }
}

/**
 * Função para fazer merge automático de dados do Paciente
 */
private fun performAutomaticMerge(
    localData: PacienteEntity,
    serverData: PacienteEntity
): PacienteEntity {
    val useLocalForCriticalData = localData.updatedAt > serverData.updatedAt

    return localData.copy(
        // === DADOS QUE SEMPRE VÊM DO SERVIDOR ===
        serverId = serverData.serverId ?: localData.serverId,

        // === DADOS CRÍTICOS (IDENTIDADE) - USAR MAIS RECENTE ===
        nome = if (useLocalForCriticalData) localData.nome else serverData.nome,
        dataNascimento = if (useLocalForCriticalData) localData.dataNascimento else serverData.dataNascimento,
        nomeDaMae = if (useLocalForCriticalData) localData.nomeDaMae else serverData.nomeDaMae,
        cpf = if (useLocalForCriticalData) localData.cpf else serverData.cpf,
        sus = if (useLocalForCriticalData) localData.sus else serverData.sus,
        telefone = if (useLocalForCriticalData) localData.telefone else serverData.telefone,
        endereco = if (useLocalForCriticalData) localData.endereco else serverData.endereco,

        // === DADOS DE SAÚDE - PRIORIZAR SERVIDOR ===
        pressaoArterial = serverData.pressaoArterial ?: localData.pressaoArterial,
        frequenciaCardiaca = serverData.frequenciaCardiaca ?: localData.frequenciaCardiaca,
        frequenciaRespiratoria = serverData.frequenciaRespiratoria ?: localData.frequenciaRespiratoria,
        temperatura = serverData.temperatura ?: localData.temperatura,
        glicemia = serverData.glicemia ?: localData.glicemia,
        saturacaoOxigenio = serverData.saturacaoOxigenio ?: localData.saturacaoOxigenio,
        peso = serverData.peso ?: localData.peso,
        altura = serverData.altura ?: localData.altura,
        imc = serverData.imc ?: localData.imc,
        idade = if (serverData.updatedAt > localData.updatedAt) serverData.idade else localData.idade,

        // === MANTER DADOS LOCAIS ===
        localId = localData.localId,
        deviceId = localData.deviceId,

        // === ATUALIZAR CONTROLE ===
        syncStatus = SyncStatus.SYNCED,
        version = maxOf(localData.version, serverData.version) + 1,
        updatedAt = getCurrentTimestamp(),
        lastSyncTimestamp = getCurrentTimestamp(),
        createdAt = localData.createdAt,

        // === LIMPAR DADOS DE CONFLITO ===
        conflictData = null,
        syncAttempts = 0,
        lastSyncAttempt = getCurrentTimestamp(),
        syncError = null,

        // === SERVIDOR DECIDE DELEÇÃO ===
        isDeleted = serverData.isDeleted
    )
}

/**
 * Função para criar JSON dos dados conflitantes
 */
private fun createConflictJson(local: PacienteEntity, server: PacienteEntity): String {
    return """
    {
        "local": {
            "nome": "${local.nome}",
            "cpf": "${local.cpf}",
            "updatedAt": ${local.updatedAt},
            "version": ${local.version}
        },
        "server": {
            "nome": "${server.nome}",
            "cpf": "${server.cpf}",
            "updatedAt": ${server.updatedAt},
            "version": ${server.version}
        }
    }
    """.trimIndent()
}

// ========== EXTENSÕES PARA PACIENTE ESPECIALIDADE DAO ==========

/**
 * Método para buscar especialidades de um paciente retornando localIds
 */
suspend fun PacienteEspecialidadeDao.getEspecialidadeIdsByPacienteId(pacienteId: String): List<String> {
    return try {
        val especialidades = getEspecialidadesByPacienteId(pacienteId)

        // Mapear para os localIds das especialidades
        especialidades.map { especialidade ->
            especialidade.localId // <- acessar o localId da EspecialidadeEntity
        }
    } catch (e: Exception) {
        // Log do erro e retornar lista vazia
        println("Erro ao buscar especialidades do paciente $pacienteId: ${e.message}")
        emptyList()
    }
}

// ========== MÉTODOS AUXILIARES PARA PACIENTE ESPECIALIDADE DAO ==========

/**
 * Buscar relacionamento específico entre paciente e especialidade
 */
suspend fun PacienteEspecialidadeDao.getRelationByIds(
    pacienteId: String,
    especialidadeId: String
): PacienteEspecialidadeEntity? {
    return try {
        // Você precisa implementar este método no seu DAO
        getPacienteEspecialidadeByIds(pacienteId, especialidadeId)
    } catch (e: Exception) {
        null
    }
}

/**
 * Verificar se relacionamento existe
 */
suspend fun PacienteEspecialidadeDao.relationExists(
    pacienteId: String,
    especialidadeId: String
): Boolean {
    return getRelationByIds(pacienteId, especialidadeId) != null
}

/**
 * Contar relacionamentos de um paciente
 */
suspend fun PacienteEspecialidadeDao.countEspecialidadesByPaciente(pacienteId: String): Int {
    return try {
        getEspecialidadesByPacienteId(pacienteId).size
    } catch (e: Exception) {
        0
    }
}

/**
 * Buscar todos os pacientes de uma especialidade
 */
suspend fun PacienteEspecialidadeDao.getPacienteIdsByEspecialidadeId(especialidadeId: String): List<String> {
    return try {
        val pacientes = getPacientesByEspecialidadeId(especialidadeId)

        // Mapear para os localIds dos pacientes
        pacientes.map { paciente ->
            paciente.localId // <- acessar o localId da PacienteEntity
        }
    } catch (e: Exception) {
        emptyList()
    }
}

// ========== MÉTODOS PARA SINCRONIZAÇÃO DE RELACIONAMENTOS ==========

/**
 * Sincronizar relacionamentos paciente-especialidade
 */
suspend fun PacienteEspecialidadeDao.syncRelationships(
    relationships: List<PacienteEspecialidadeEntity>
) {
    relationships.forEach { relationship ->
        try {
            val existing = getRelationByIds(
                relationship.pacienteLocalId,
                relationship.especialidadeLocalId
            )

            if (existing == null) {
                // Novo relacionamento
                insertPacienteEspecialidade(relationship.copy(
                    syncStatus = SyncStatus.SYNCED,
                    updatedAt = getCurrentTimestamp()
                ))
            } else {
                // Atualizar relacionamento existente
                // CORREÇÃO: Remover o localId que não existe
                updatePacienteEspecialidade(relationship.copy(
                    // Manter as chaves primárias do relationship
                    pacienteLocalId = existing.pacienteLocalId,
                    especialidadeLocalId = existing.especialidadeLocalId,
                    // Atualizar campos de sincronização
                    syncStatus = SyncStatus.SYNCED,
                    updatedAt = getCurrentTimestamp(),
                    // Preservar versão incrementada
                    version = existing.version + 1,
                    // Preservar data de criação
                    createdAt = existing.createdAt,
                    // Atualizar timestamp de sync
                    lastSyncTimestamp = getCurrentTimestamp()
                ))
            }
        } catch (e: Exception) {
            // Log do erro
            println("Erro ao sincronizar relacionamento: ${e.message}")
        }
    }
}

// ========== MÉTODOS SEGUROS PARA OPERAÇÕES ==========

/**
 * Inserir relacionamento com validação
 */
suspend fun PacienteEspecialidadeDao.insertRelationshipSafe(
    pacienteId: String,
    especialidadeId: String,
    deviceId: String
): Boolean {
    return try {
        // Verificar se já existe
        if (relationExists(pacienteId, especialidadeId)) {
            return false // Já existe
        }

        // Validar IDs
        if (!pacienteId.isValidUUID() || !especialidadeId.isValidUUID()) {
            return false // IDs inválidos
        }

        val relationship = PacienteEspecialidadeEntity(
            pacienteLocalId = pacienteId,
            especialidadeLocalId = especialidadeId,
            deviceId = deviceId,
            syncStatus = SyncStatus.PENDING_UPLOAD,
            createdAt = getCurrentTimestamp(),
            updatedAt = getCurrentTimestamp()
        )

        insertPacienteEspecialidade(relationship)
        true
    } catch (e: Exception) {
        false
    }
}

/**
 * Remover relacionamento com validação
 */
suspend fun PacienteEspecialidadeDao.removeRelationshipSafe(
    pacienteId: String,
    especialidadeId: String
): Boolean {
    return try {
        val existing = getRelationByIds(pacienteId, especialidadeId)

        if (existing != null) {
            // Soft delete
            val deleted = existing.copy(
                isDeleted = true,
                syncStatus = SyncStatus.PENDING_DELETE,
                updatedAt = getCurrentTimestamp()
            )
            updatePacienteEspecialidade(deleted)
            true
        } else {
            false // Relacionamento não existe
        }
    } catch (e: Exception) {
        false
    }
}

/**
 * Método para inserir relacionamento paciente-especialidade
 */
suspend fun PacienteEspecialidadeDao.insertPacienteEspecialidadeRelation(
    pacienteId: String,
    especialidadeId: String,
    syncStatus: SyncStatus = SyncStatus.PENDING_UPLOAD
) {
    val relation = PacienteEspecialidadeEntity(
        pacienteLocalId = pacienteId,
        especialidadeLocalId = especialidadeId,
        syncStatus = syncStatus,
        createdAt = getCurrentTimestamp(),
        updatedAt = getCurrentTimestamp()
    )
    insertPacienteEspecialidade(relation)
}

/**
 * Método para remover relacionamento paciente-especialidade
 */
suspend fun PacienteEspecialidadeDao.removePacienteEspecialidadeRelation(
    pacienteId: String,
    especialidadeId: String
) {
    val existing = getRelationByIds(pacienteId, especialidadeId)
    existing?.let { relation ->
        val deleted = relation.copy(
            isDeleted = true,
            syncStatus = SyncStatus.PENDING_DELETE,
            updatedAt = getCurrentTimestamp()
        )
        updatePacienteEspecialidade(deleted)
    }
}

// ========== FUNÇÕES UTILITÁRIAS ==========

/**
 * Extensão para validar se uma String é um UUID válido
 */
fun String?.isValidUUID(): Boolean {
    return try {
        this?.let { UUID.fromString(it) }
        true
    } catch (e: IllegalArgumentException) {
        false
    }
}

/**
 * Extensão para gerar UUID se a String for inválida
 */
fun String?.toValidUUID(): String {
    return if (this.isValidUUID()) {
        this!!
    } else {
        UUID.randomUUID().toString()
    }
}

/**
 * Função para gerar timestamp atual
 */
fun getCurrentTimestamp(): Long = System.currentTimeMillis()

/**
 * Função para verificar se uma sincronização é necessária
 */
fun needsSync(lastSyncTimestamp: Long, itemTimestamp: Long): Boolean {
    return itemTimestamp > lastSyncTimestamp
}

/**
 * Função para gerar novo UUID
 */
fun generateNewUUID(): String = UUID.randomUUID().toString()

/**
 * Função para validar e converter lista de IDs
 */
fun List<String>.validateUUIDs(): List<String> {
    return this.filter { it.isValidUUID() }
}

/**
 * Exemplo de uso na prática
 */
suspend fun exemploUsoAdaptado(
    pacienteEspecialidadeDao: PacienteEspecialidadeDao,
    pacienteId: String,
    especialidadeId: String
) {
    val validPacienteId = pacienteId.toValidUUID()
    val validEspecialidadeId = especialidadeId.toValidUUID()

    pacienteEspecialidadeDao.insertPacienteEspecialidadeRelation(
        pacienteId = validPacienteId,
        especialidadeId = validEspecialidadeId
    )

    val especialidadeIds = pacienteEspecialidadeDao
        .getEspecialidadeIdsByPacienteId(validPacienteId)

    println("Especialidades do paciente $validPacienteId: $especialidadeIds")
}