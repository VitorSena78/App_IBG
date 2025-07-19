package com.example.projeto_ibg3.domain.repository


import com.example.projeto_ibg3.data.local.database.entities.PacienteEntity
import com.example.projeto_ibg3.domain.model.Paciente
import kotlinx.coroutines.flow.Flow

interface PacienteRepository {
    // Operações básicas
    fun getAllPacientes(): Flow<List<Paciente>>
    suspend fun getPacienteById(localId: String): Paciente?
    suspend fun insertPaciente(paciente: Paciente): String
    suspend fun updatePaciente(paciente: Paciente)
    suspend fun deletePaciente(localId: String)

    // Sincronização
    suspend fun syncPacientes(): Result<Unit>
    suspend fun getPacientesNaoSincronizados(): List<Paciente>
    suspend fun markAsSynced(localId: String, serverId: Long)

    // Busca
    suspend fun searchPacientes(query: String): Flow<List<Paciente>>
    suspend fun getPacienteCount(): Int

    // Validação
    suspend fun pacienteExists(cpf: String, excludeId: String = ""): Boolean
}