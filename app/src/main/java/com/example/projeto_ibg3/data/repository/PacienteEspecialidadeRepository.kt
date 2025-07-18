package com.example.projeto_ibg3.data.repository

import com.example.projeto_ibg3.domain.model.Especialidade
import com.example.projeto_ibg3.domain.model.PacienteEspecialidade
import kotlinx.coroutines.flow.Flow

interface PacienteEspecialidadeRepository {

    // ==================== FUNÇÕES BÁSICAS ====================

    fun getAllPacienteEspecialidades(): Flow<List<PacienteEspecialidade>>

    suspend fun getEspecialidadesByPacienteId(pacienteLocalId: String): List<Especialidade>

    suspend fun getPacientesByEspecialidadeId(especialidadeLocalId: String): List<PacienteEspecialidade>

    suspend fun getPacienteEspecialidade(pacienteLocalId: String, especialidadeLocalId: String): PacienteEspecialidade?

    // ==================== OPERAÇÕES DE CRUD ====================

    suspend fun insertPacienteEspecialidade(pacienteEspecialidade: PacienteEspecialidade)

    suspend fun insertPacienteEspecialidades(pacienteEspecialidades: List<PacienteEspecialidade>)

    suspend fun updatePacienteEspecialidade(pacienteEspecialidade: PacienteEspecialidade)

    suspend fun deletePacienteEspecialidade(pacienteEspecialidade: PacienteEspecialidade)

    suspend fun deleteByIds(pacienteLocalId: String, especialidadeLocalId: String)

    suspend fun deleteAllEspecialidadesByPacienteId(pacienteLocalId: String)

    // ==================== MÉTODOS PARA O VIEWMODEL ====================

    suspend fun addEspecialidadeToPaciente(pacienteLocalId: String, especialidadeLocalId: String)

    suspend fun removeEspecialidadeFromPaciente(pacienteLocalId: String, especialidadeLocalId: String)

    // ==================== CONSULTAS POR DATA ====================

    suspend fun getAtendimentosByDateRange(dataInicio: String, dataFim: String): List<PacienteEspecialidade>

    suspend fun getAtendimentosCountByEspecialidade(especialidadeLocalId: String): Int

    // ==================== SINCRONIZAÇÃO ====================

    suspend fun syncPacienteEspecialidades(pacienteLocalId: String, especialidadeIds: List<String>)

    suspend fun getPendingSync(): List<PacienteEspecialidade>

    suspend fun getConflicts(): List<PacienteEspecialidade>

    suspend fun markAsSynced(pacienteLocalId: String, especialidadeLocalId: String)

    suspend fun markAsConflict(pacienteLocalId: String, especialidadeLocalId: String)

    // ==================== FUNÇÕES AUXILIARES ====================

    suspend fun softDeletePacienteEspecialidade(pacienteLocalId: String, especialidadeLocalId: String)

    suspend fun restorePacienteEspecialidade(pacienteLocalId: String, especialidadeLocalId: String)

    // ==================== ESTATÍSTICAS ====================

    suspend fun countPendingUploads(): Int

    suspend fun countConflicts(): Int

    // ==================== LIMPEZA ====================

    suspend fun cleanupDeletedSynced()

    suspend fun cleanupOldSynced(daysOld: Int = 30)
}