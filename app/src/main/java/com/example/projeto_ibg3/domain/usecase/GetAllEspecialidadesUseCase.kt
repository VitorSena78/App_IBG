package com.example.projeto_ibg3.domain.usecase

import com.example.projeto_ibg3.domain.model.Especialidade
import com.example.projeto_ibg3.domain.repository.EspecialidadeRepository
import com.example.projeto_ibg3.domain.repository.SyncRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class GetAllEspecialidadesUseCase @Inject constructor(
    private val especialidadeRepository: EspecialidadeRepository,
    private val syncRepository: SyncRepository
) {

    /**
     * Retorna as especialidades do banco local como Flow.
     * Observa mudanças em tempo real.
     */
    fun observeEspecialidades(): Flow<List<Especialidade>> {
        return especialidadeRepository.getAllEspecialidades()
            .let { flow ->
                // Converter de Entity para Domain Model usando o mapper
                kotlinx.coroutines.flow.flow {
                    flow.collect { entitiesList ->
                        val domainList = entitiesList.map { entity ->
                            Especialidade(
                                localId = entity.localId,
                                serverId = entity.serverId,
                                nome = entity.nome
                            )
                        }
                        emit(domainList)
                    }
                }
            }
    }

    /**
     * Busca especialidades do banco local (snapshot único).
     * Consulta única, não observável.
     */
    suspend fun getEspecialidades(): List<Especialidade> {
        return observeEspecialidades().first()
    }

    //Sincroniza especialidades com o servidor.
    suspend fun syncEspecialidades(): Result<Unit> {
        return syncRepository.syncEspecialidades()
    }

    /**
     * Verifica se há especialidades no banco local.
     * Verifica se é necessário sincronizar.
     */
    suspend fun hasLocalEspecialidades(): Boolean {
        return especialidadeRepository.getEspecialidadeCount() > 0
    }

    //Busca especialidades por nome.
    suspend fun searchEspecialidades(query: String): List<Especialidade> {
        return especialidadeRepository.searchEspecialidades(query)
    }

    /**
     * Carrega especialidades com estratégia:
     * 1. Se há dados locais, retorna imediatamente
     * 2. Se não há dados ou são antigos, sincroniza com servidor
     * 3. Se sincronização falha, retorna dados locais (se houver)
     */
    suspend fun loadEspecialidades(forceSync: Boolean = false): Result<List<Especialidade>> {
        return try {
            // Verificar dados locais
            val localData = getEspecialidades()

            // Se temos dados locais e não é sync forçado, usar dados locais
            if (localData.isNotEmpty() && !forceSync) {
                return Result.success(localData)
            }

            // Tentar sincronizar
            val syncResult = syncEspecialidades()

            if (syncResult.isSuccess) {
                // Sincronização ok, buscar dados atualizados
                val updatedData = getEspecialidades()
                Result.success(updatedData)
            } else {
                // Sincronização falhou, usar dados locais se houver
                if (localData.isNotEmpty()) {
                    Result.success(localData)
                } else {
                    Result.failure(syncResult.exceptionOrNull() ?: Exception("Falha na sincronização"))
                }
            }

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}