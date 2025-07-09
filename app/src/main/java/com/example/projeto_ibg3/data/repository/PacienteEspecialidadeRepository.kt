package com.example.projeto_ibg3.data.repository

import com.example.projeto_ibg3.data.dao.PacienteEspecialidadeDao
import com.example.projeto_ibg3.data.entity.PacienteEspecialidadeEntity
import com.example.projeto_ibg3.model.PacienteEspecialidade
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PacienteEspecialidadeRepository @Inject constructor(
    private val pacienteEspecialidadeDao: PacienteEspecialidadeDao
) {

    fun getAllPacienteEspecialidades(): Flow<List<PacienteEspecialidade>> {
        return pacienteEspecialidadeDao.getAllPacienteEspecialidades().map { entities ->
            entities.map { it.toPacienteEspecialidade() }
        }
    }

    suspend fun getEspecialidadesByPacienteId(pacienteId: Long): List<PacienteEspecialidade> {
        return pacienteEspecialidadeDao.getEspecialidadesByPacienteId(pacienteId)
            .map { it.toPacienteEspecialidade() }
    }

    suspend fun getPacientesByEspecialidadeId(especialidadeId: Long): List<PacienteEspecialidade> {
        return pacienteEspecialidadeDao.getPacientesByEspecialidadeId(especialidadeId)
            .map { it.toPacienteEspecialidade() }
    }

    suspend fun getPacienteEspecialidade(pacienteId: Long, especialidadeId: Long): PacienteEspecialidade? {
        return pacienteEspecialidadeDao.getPacienteEspecialidade(pacienteId, especialidadeId)
            ?.toPacienteEspecialidade()
    }

    suspend fun insertPacienteEspecialidade(pacienteEspecialidade: PacienteEspecialidade) {
        pacienteEspecialidadeDao.insertPacienteEspecialidade(pacienteEspecialidade.toEntity())
    }

    suspend fun insertPacienteEspecialidades(pacienteEspecialidades: List<PacienteEspecialidade>) {
        val entities = pacienteEspecialidades.map { it.toEntity() }
        pacienteEspecialidadeDao.insertPacienteEspecialidades(entities)
    }

    suspend fun updatePacienteEspecialidade(pacienteEspecialidade: PacienteEspecialidade) {
        pacienteEspecialidadeDao.updatePacienteEspecialidade(pacienteEspecialidade.toEntity())
    }

    suspend fun deletePacienteEspecialidade(pacienteEspecialidade: PacienteEspecialidade) {
        pacienteEspecialidadeDao.deletePacienteEspecialidade(pacienteEspecialidade.toEntity())
    }

    suspend fun deleteByIds(pacienteId: Long, especialidadeId: Long) {
        pacienteEspecialidadeDao.deleteByIds(pacienteId, especialidadeId)
    }

    suspend fun deleteAllEspecialidadesByPacienteId(pacienteId: Long) {
        pacienteEspecialidadeDao.deleteAllEspecialidadesByPacienteId(pacienteId)
    }

    suspend fun getAtendimentosByDateRange(dataInicio: String, dataFim: String): List<PacienteEspecialidade> {
        return pacienteEspecialidadeDao.getAtendimentosByDateRange(dataInicio, dataFim)
            .map { it.toPacienteEspecialidade() }
    }

    suspend fun getAtendimentosCountByEspecialidade(especialidadeId: Long): Int {
        return pacienteEspecialidadeDao.getAtendimentosCountByEspecialidade(especialidadeId)
    }

    // Método para sincronizar especialidades de um paciente
    suspend fun syncPacienteEspecialidades(pacienteId: Long, especialidadeIds: List<Long>) {
        // Remover todas as especialidades existentes do paciente
        deleteAllEspecialidadesByPacienteId(pacienteId)

        // Adicionar as novas especialidades
        val novasAssociacoes = especialidadeIds.map { especialidadeId ->
            PacienteEspecialidade(
                pacienteId = pacienteId,
                especialidadeId = especialidadeId,
                dataAtendimento = null // ou Date() se necessário
            )
        }

        if (novasAssociacoes.isNotEmpty()) {
            insertPacienteEspecialidades(novasAssociacoes)
        }
    }
}

// Extensões para conversão entre Entity e Model
private fun PacienteEspecialidadeEntity.toPacienteEspecialidade(): PacienteEspecialidade {
    return PacienteEspecialidade(
        pacienteId = this.pacienteId,
        especialidadeId = this.especialidadeId,
        dataAtendimento = this.dataAtendimento?.let { Date(it) } // Converte Long para Date
    )
}

private fun PacienteEspecialidade.toEntity(): PacienteEspecialidadeEntity {
    return PacienteEspecialidadeEntity(
        pacienteId = this.pacienteId,
        especialidadeId = this.especialidadeId,
        dataAtendimento = this.dataAtendimento?.time // Converte Date para Long
    )
}