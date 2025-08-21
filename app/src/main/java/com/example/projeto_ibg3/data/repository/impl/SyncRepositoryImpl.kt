package com.example.projeto_ibg3.data.repository.impl

import android.util.Log
import com.example.projeto_ibg3.data.local.database.dao.EspecialidadeDao
import com.example.projeto_ibg3.data.local.database.dao.PacienteDao
import com.example.projeto_ibg3.data.local.database.dao.PacienteEspecialidadeDao
import com.example.projeto_ibg3.data.local.database.entities.EspecialidadeEntity
import com.example.projeto_ibg3.data.local.database.entities.PacienteEntity
import com.example.projeto_ibg3.data.local.database.entities.PacienteEspecialidadeEntity
import com.example.projeto_ibg3.data.mappers.*
import com.example.projeto_ibg3.data.remote.api.ApiService
import com.example.projeto_ibg3.data.remote.api.NetworkManager
import com.example.projeto_ibg3.data.remote.dto.EspecialidadeDto
import com.example.projeto_ibg3.data.remote.dto.PacienteDto
import com.example.projeto_ibg3.data.remote.dto.PacienteEspecialidadeDTO
import com.example.projeto_ibg3.domain.model.SyncProgress
import com.example.projeto_ibg3.domain.model.SyncState
import com.example.projeto_ibg3.domain.model.SyncStatus
import com.example.projeto_ibg3.domain.repository.SyncRepository
import com.example.projeto_ibg3.domain.repository.EspecialidadeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncRepositoryImpl @Inject constructor(
    private val networkManager: NetworkManager,
    private val especialidadeDao: EspecialidadeDao,
    private val pacienteDao: PacienteDao,
    private val pacienteEspecialidadeDao: PacienteEspecialidadeDao,
    private val especialidadeRepository: EspecialidadeRepository,
    private val apiService: ApiService
) : SyncRepository {

    companion object {
        private const val TAG = "SyncRepository"
        private const val BATCH_SIZE = 100 // Aumentado para melhor performance
        private const val TOTAL_SYNC_STEPS = 6
        private const val CONCURRENT_REQUESTS = 3 // Para processamento paralelo
    }

    // ==================== ESTADOS OBSERVÁVEIS ====================
    private val _syncState = MutableStateFlow(SyncState())
    override val syncState: Flow<SyncState> = _syncState.asStateFlow()

    private val _syncProgress = MutableStateFlow(SyncProgress())
    override val syncProgress: Flow<SyncProgress> = _syncProgress.asStateFlow()

    // Cache para evitar consultas repetidas
    private var cachedPacientes: Map<Long, PacienteEntity>? = null
    private var cachedEspecialidades: Map<Long, EspecialidadeEntity>? = null
    private var cacheTimestamp: Long = 0
    private val cacheTimeout = 5 * 60 * 1000L // 5 minutos

    // ==================== SINCRONIZAÇÃO COMPLETA OTIMIZADA ====================
    override suspend fun startSync(): Flow<SyncState> = flow {
        Log.d(TAG, "Iniciando sincronização completa otimizada...")

        updateSyncState("Iniciando sincronização completa...", isLoading = true)
        emit(_syncState.value)

        try {
            if (!isNetworkAvailable()) {
                emitErrorState("Sem conexão com a internet")
                return@flow
            }

            // Executar sincronizações em paralelo quando possível
            supervisorScope {
                var currentStep = 0

                // Etapa 1: Sincronizar especialidades (crítico)
                currentStep++
                updateSyncState("Sincronizando especialidades...", currentStep, TOTAL_SYNC_STEPS)
                emit(_syncState.value)

                val especialidadesResult = syncEspecialidades()
                if (especialidadesResult.isFailure) {
                    emitErrorState(especialidadesResult.exceptionOrNull()?.message ?: "Erro nas especialidades")
                    return@supervisorScope
                }

                // Etapas 2-3: Upload e download de pacientes em paralelo
                currentStep++
                updateSyncState("Sincronizando pacientes...", currentStep, TOTAL_SYNC_STEPS)
                emit(_syncState.value)

                val (uploadPacientesResult, downloadPacientesResult) = awaitAll(
                    async { uploadPendingPacientes() },
                    async { downloadUpdatedPacientes() }
                )

                // Etapas 4-5: Relacionamentos em paralelo
                currentStep += 2
                updateSyncState("Sincronizando relacionamentos...", currentStep, TOTAL_SYNC_STEPS)
                emit(_syncState.value)

                val (uploadRelResult, downloadRelResult) = awaitAll(
                    async { uploadPendingPacienteEspecialidades() },
                    async { downloadUpdatedPacienteEspecialidades() }
                )

                // Etapa 6: Finalização
                currentStep++
                updateSyncState("Finalizando...", currentStep, TOTAL_SYNC_STEPS)
                emit(_syncState.value)

                clearCache() // Limpar cache após sincronização
                updateLastSyncTimestamp(System.currentTimeMillis())

                emitSuccessState("Sincronização concluída com sucesso!", TOTAL_SYNC_STEPS)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Erro inesperado durante sincronização", e)
            emitErrorState(e.message ?: "Erro inesperado durante a sincronização")
        }
    }

    // ==================== CACHE MANAGEMENT ====================
    private fun clearCache() {
        cachedPacientes = null
        cachedEspecialidades = null
        cacheTimestamp = 0
    }

    private fun isCacheValid(): Boolean {
        return System.currentTimeMillis() - cacheTimestamp < cacheTimeout
    }

    private suspend fun getCachedPacientes(): Map<Long, PacienteEntity> {
        if (cachedPacientes == null || !isCacheValid()) {
            cachedPacientes = pacienteDao.getAllPacientesList()
                .filter { it.serverId != null }
                .associateBy { it.serverId!! }
            cacheTimestamp = System.currentTimeMillis()
        }
        return cachedPacientes!!
    }

    private suspend fun getCachedEspecialidades(): Map<Long, EspecialidadeEntity> {
        if (cachedEspecialidades == null || !isCacheValid()) {
            cachedEspecialidades = especialidadeDao.getAllEspecialidadesList()
                .filter { it.serverId != null }
                .associateBy { it.serverId!! }
            cacheTimestamp = System.currentTimeMillis()
        }
        return cachedEspecialidades!!
    }

    // ==================== SINCRONIZAÇÃO DE ESPECIALIDADES OTIMIZADA ====================
    override suspend fun syncEspecialidades(): Result<Unit> {
        return try {
            Log.d(TAG, "Iniciando syncEspecialidades otimizado")

            if (!isNetworkAndServerAvailable()) {
                return Result.failure(Exception("Conectividade não disponível"))
            }

            val response = apiService.getAllEspecialidades()
            if (!response.isSuccessful || response.body()?.success != true) {
                return Result.failure(Exception("Erro API: ${response.code()} - ${response.message()}"))
            }

            val especialidadesDto = response.body()?.data ?: emptyList()
            Log.d(TAG, "Recebidas ${especialidadesDto.size} especialidades")

            if (especialidadesDto.isNotEmpty()) {
                processarEspecialidadesOtimizado(especialidadesDto)
            }

            clearCache() // Invalidar cache após atualização
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Erro em syncEspecialidades", e)
            Result.failure(e)
        }
    }

    private suspend fun processarEspecialidadesOtimizado(especialidadesDto: List<EspecialidadeDto>) {
        // Buscar todas as especialidades locais de uma vez
        val especialidadesLocaisByServerId = especialidadeDao.getAllEspecialidadesList()
            .filter { it.serverId != null }
            .associateBy { it.serverId!! }

        val especialidadesLocaisByName = especialidadeDao.getAllEspecialidadesList()
            .associateBy { it.nome }

        val paraInserir = mutableListOf<EspecialidadeEntity>()
        val paraAtualizar = mutableListOf<EspecialidadeEntity>()

        especialidadesDto.forEach { dto ->
            if (dto.serverId == null) {
                Log.w(TAG, "DTO inválido: ${dto.nome}")
                return@forEach
            }

            val existingByServerId = especialidadesLocaisByServerId[dto.serverId]
            val existingByName = especialidadesLocaisByName[dto.nome]

            when {
                existingByServerId != null -> {
                    // Atualizar se necessário
                    if (shouldUpdateEspecialidade(existingByServerId, dto)) {
                        paraAtualizar.add(createUpdatedEspecialidade(existingByServerId, dto))
                    }
                }
                existingByName != null && existingByName.serverId == null -> {
                    // Vincular especialidade local ao servidor
                    paraAtualizar.add(linkLocalEspecialidadeToServer(existingByName, dto))
                }
                existingByName == null -> {
                    // Nova especialidade
                    paraInserir.add(createNewEspecialidadeFromDto(dto))
                }
                else -> {
                    Log.w(TAG, "Conflito de nomes: ${dto.nome}")
                }
            }
        }

        // Executar operações em batch
        if (paraInserir.isNotEmpty()) {
            especialidadeDao.insertEspecialidades(paraInserir)
            Log.d(TAG, "✅ ${paraInserir.size} especialidades inseridas")
        }

        if (paraAtualizar.isNotEmpty()) {
            especialidadeDao.updateEspecialidades(paraAtualizar)
            Log.d(TAG, "✅ ${paraAtualizar.size} especialidades atualizadas")
        }
    }

    private fun shouldUpdateEspecialidade(existing: EspecialidadeEntity, dto: EspecialidadeDto): Boolean {
        return existing.updatedAt < (dto.updatedAt.toDateLong() ?: 0L) ||
                existing.fichas != dto.fichas ||
                existing.nome != dto.nome
    }

    private fun createUpdatedEspecialidade(existing: EspecialidadeEntity, dto: EspecialidadeDto): EspecialidadeEntity {
        return existing.copy(
            nome = dto.nome,
            fichas = dto.fichas ?: 0,
            atendimentosRestantesHoje = dto.atendimentosRestantesHoje,
            atendimentosTotaisHoje = dto.atendimentosTotaisHoje,
            syncStatus = SyncStatus.SYNCED,
            updatedAt = dto.updatedAt.toDateLong() ?: 0L,
            lastSyncTimestamp = System.currentTimeMillis()
        )
    }

    private fun linkLocalEspecialidadeToServer(existing: EspecialidadeEntity, dto: EspecialidadeDto): EspecialidadeEntity {
        return existing.copy(
            serverId = dto.serverId,
            fichas = dto.fichas ?: 0,
            atendimentosRestantesHoje = dto.atendimentosRestantesHoje,
            atendimentosTotaisHoje = dto.atendimentosTotaisHoje,
            syncStatus = SyncStatus.SYNCED,
            updatedAt = dto.updatedAt.toDateLong() ?: 0L,
            lastSyncTimestamp = System.currentTimeMillis()
        )
    }

    private fun createNewEspecialidadeFromDto(dto: EspecialidadeDto): EspecialidadeEntity {
        return dto.toEntity("default_device", SyncStatus.SYNCED).copy(
            lastSyncTimestamp = System.currentTimeMillis()
        )
    }

    // ==================== SINCRONIZAÇÃO DE PACIENTES OTIMIZADA ====================
    private suspend fun uploadPendingPacientes(): Result<Unit> = coroutineScope {
        try {
            Log.d(TAG, "=== UPLOAD OTIMIZADO DE PACIENTES ===")

            val pendingPacientes = getPacientesPendentes()
            if (pendingPacientes.isEmpty()) {
                Log.d(TAG, "Nenhum paciente pendente")
                return@coroutineScope Result.success(Unit)
            }

            val operacoes = categorizarPacientesPorOperacao(pendingPacientes)

            // Executar operações em paralelo quando possível
            val results = awaitAll(
                async {
                    operacoes["create"]?.takeIf { it.isNotEmpty() }?.let {
                        processarCriacaoPacientesOtimizado(it)
                    } ?: Result.success(Unit)
                },
                async {
                    operacoes["update"]?.takeIf { it.isNotEmpty() }?.let {
                        processarAtualizacaoPacientesOtimizado(it)
                    } ?: Result.success(Unit)
                },
                async {
                    operacoes["delete"]?.takeIf { it.isNotEmpty() }?.let {
                        processarDelecaoPacientesOtimizado(it)
                    } ?: Result.success(Unit)
                }
            )

            Log.d(TAG, "✅ Upload de pacientes concluído")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "❌ Erro no upload de pacientes", e)
            Result.failure(e)
        }
    }

    private suspend fun processarCriacaoPacientesOtimizado(pacientes: List<PacienteEntity>): Result<Unit> = coroutineScope {
        try {
            // Processar em batches paralelos
            val batches = pacientes.chunked(BATCH_SIZE)
            val jobs = batches.chunked(CONCURRENT_REQUESTS).map { batchGroup ->
                async {
                    batchGroup.map { batch ->
                        async { processarBatchCriacao(batch) }
                    }.awaitAll()
                }
            }

            jobs.awaitAll()
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Erro na criação otimizada", e)
            Result.failure(e)
        }
    }

    private suspend fun processarBatchCriacao(batch: List<PacienteEntity>): Result<Unit> {
        return try {
            val pacientesDto = batch.map { it.toPacienteDto() }
            val response = apiService.createPacientesBatch(pacientesDto)

            if (response.isSuccessful && response.body()?.success == true) {
                val createdPacientes = response.body()?.data ?: emptyList()

                // Atualizar IDs em batch
                val updates = batch.zip(createdPacientes) { original, created ->
                    Triple(original.localId, SyncStatus.SYNCED, created.serverId ?: 0L)
                }

                pacienteDao.batchUpdateSyncStatusAndServerId(updates)
                Log.d(TAG, "✅ Batch de ${batch.size} pacientes criado")
                Result.success(Unit)
            } else {
                // Marcar falha em batch
                pacienteDao.batchUpdateSyncStatus(
                    batch.map { it.localId },
                    SyncStatus.UPLOAD_FAILED
                )
                Result.failure(Exception("Falha HTTP: ${response.code()}"))
            }
        } catch (e: Exception) {
            // Incrementar tentativas em batch
            pacienteDao.batchIncrementSyncAttempts(
                batch.map { Triple(it.localId, System.currentTimeMillis(), e.message) }
            )
            Result.failure(e)
        }
    }

    // ==================== RELACIONAMENTOS OTIMIZADOS ====================
    private suspend fun processarCriacaoRelacionamentosOtimizado(relations: List<PacienteEspecialidadeEntity>) = coroutineScope {
        try {
            Log.d(TAG, "=== CRIAÇÃO OTIMIZADA DE RELACIONAMENTOS ===")

            // Validar fichas em batch
            val validRelations = validateFichasBatch(relations)
            if (validRelations.isEmpty()) {
                Log.w(TAG, "Nenhum relacionamento válido")
                return@coroutineScope
            }

            // Cache para evitar consultas repetidas
            val pacientesCache = getCachedPacientes()
            val especialidadesCache = getCachedEspecialidades()

            // Processar em paralelo com limite de concorrência
            val chunks = validRelations.chunked(CONCURRENT_REQUESTS)

            chunks.forEach { chunk ->
                val jobs = chunk.map { relation ->
                    async {
                        processarRelacionamentoIndividualOtimizado(
                            relation,
                            pacientesCache,
                            especialidadesCache
                        )
                    }
                }
                jobs.awaitAll()
            }

            Log.d(TAG, "✅ Criação de relacionamentos concluída")

        } catch (e: Exception) {
            Log.e(TAG, "Erro na criação de relacionamentos", e)
        }
    }

    private suspend fun validateFichasBatch(relations: List<PacienteEspecialidadeEntity>): List<PacienteEspecialidadeEntity> {
        // Buscar fichas de todas as especialidades de uma vez
        val especialidadeIds = relations.map { it.especialidadeLocalId }.distinct()
        val fichasList = especialidadeDao.getFichasByIds(especialidadeIds)
        val fichasMap = fichasList.associate { it.localId to it.fichas }

        val validRelations = relations.filter { relation ->
            val hasFichas = (fichasMap[relation.especialidadeLocalId] ?: 0) > 0
            if (!hasFichas) {
                // Marcar como falha
                pacienteEspecialidadeDao.updateSyncStatus(
                    relation.pacienteLocalId,
                    relation.especialidadeLocalId,
                    SyncStatus.UPLOAD_FAILED
                )
            }
            hasFichas
        }

        Log.d(TAG, "Relacionamentos validados: ${validRelations.size}/${relations.size}")
        return validRelations
    }

    private suspend fun processarRelacionamentoIndividualOtimizado(
        entity: PacienteEspecialidadeEntity,
        pacientesCache: Map<Long, PacienteEntity>,
        especialidadesCache: Map<Long, EspecialidadeEntity>
    ) {
        try {
            // Usar cache em vez de consultas individuais
            val paciente = pacientesCache.values.find { it.localId == entity.pacienteLocalId }
            val especialidade = especialidadesCache.values.find { it.localId == entity.especialidadeLocalId }

            if (paciente?.serverId != null && especialidade?.serverId != null) {
                val response = apiService.create(
                    pacienteId = paciente.serverId.toInt(),
                    especialidadeId = especialidade.serverId.toInt(),
                    dataAtendimento = entity.dataAtendimento?.let {
                        java.time.LocalDate.ofEpochDay(it / 86400000)
                    }
                )

                if (response.isSuccessful) {
                    // Operações em batch quando possível
                    pacienteEspecialidadeDao.updateSyncStatusWithServerIds(
                        entity.pacienteLocalId,
                        entity.especialidadeLocalId,
                        SyncStatus.SYNCED,
                        paciente.serverId,
                        especialidade.serverId
                    )

                    especialidadeDao.decrementarFichas(especialidade.localId)
                    Log.d(TAG, "✅ Relacionamento criado")
                } else {
                    pacienteEspecialidadeDao.updateSyncStatus(
                        entity.pacienteLocalId,
                        entity.especialidadeLocalId,
                        SyncStatus.UPLOAD_FAILED
                    )
                    Log.e(TAG, "❌ Falha: ${response.code()}")
                }
            } else {
                Log.w(TAG, "⚠️ ServerIds faltando")
            }

        } catch (e: Exception) {
            Log.e(TAG, "💥 Erro no relacionamento", e)
            pacienteEspecialidadeDao.incrementSyncAttempts(
                entity.pacienteLocalId,
                entity.especialidadeLocalId,
                System.currentTimeMillis(),
                e.message
            )
        }
    }

    // ==================== DOWNLOAD OTIMIZADO ====================
    private suspend fun downloadUpdatedPacientes(): Result<Unit> {
        return try {
            Log.d(TAG, "Download otimizado de pacientes")

            val lastSync = getLastSyncTimestamp()
            val response = apiService.getUpdatedPacientes(lastSync)

            if (!response.isSuccessful || response.body()?.success != true) {
                return Result.failure(Exception("Erro API: ${response.code()}"))
            }

            val pacientesDto = response.body()?.data ?: emptyList()
            Log.d(TAG, "Recebidos ${pacientesDto.size} pacientes")

            if (pacientesDto.isNotEmpty()) {
                processarPacientesDoServidorOtimizado(pacientesDto)
            }

            clearCache() // Invalidar após atualização
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Erro no download", e)
            Result.failure(e)
        }
    }

    private suspend fun processarPacientesDoServidorOtimizado(pacientesDto: List<PacienteDto>) {
        // Buscar todos os pacientes locais de uma vez
        val pacientesLocaisByServerId = getCachedPacientes()
        val pacientesLocaisByCpf = pacienteDao.getAllPacientesList()
            .filter { !it.cpf.isNullOrBlank() }
            .associateBy { it.cpf }

        val paraInserir = mutableListOf<PacienteEntity>()
        val paraAtualizar = mutableListOf<PacienteEntity>()

        pacientesDto.forEach { dto ->
            try {
                val existingByServerId = dto.serverId?.let { pacientesLocaisByServerId[it] }
                val existingByCpf = dto.cpf.let { pacientesLocaisByCpf[it] }

                when {
                    existingByServerId != null -> {
                        if (shouldUpdatePaciente(existingByServerId, dto)) {
                            paraAtualizar.add(createUpdatedPaciente(existingByServerId, dto))
                        }
                    }
                    existingByCpf != null && existingByCpf.serverId == null -> {
                        paraAtualizar.add(linkLocalPacienteToServer(existingByCpf, dto))
                    }
                    else -> {
                        paraInserir.add(createNewPacienteFromDto(dto))
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao processar paciente ${dto.nome}", e)
            }
        }

        // Executar em batch
        if (paraInserir.isNotEmpty()) {
            pacienteDao.insertPacientes(paraInserir)
            Log.d(TAG, "✅ ${paraInserir.size} pacientes inseridos")
        }

        if (paraAtualizar.isNotEmpty()) {
            pacienteDao.updatePacientes(paraAtualizar)
            Log.d(TAG, "✅ ${paraAtualizar.size} pacientes atualizados")
        }
    }

    // ==================== MÉTODOS AUXILIARES OTIMIZADOS ====================
    private fun shouldUpdatePaciente(existing: PacienteEntity, dto: PacienteDto): Boolean {
        val dtoUpdatedAt = dto.updatedAt.toDateLong()
        return dtoUpdatedAt > existing.updatedAt
    }

    private fun createUpdatedPaciente(existing: PacienteEntity, dto: PacienteDto): PacienteEntity {
        return dto.toPacienteEntity().copy(
            localId = existing.localId,
            syncStatus = SyncStatus.SYNCED,
            lastSyncTimestamp = System.currentTimeMillis()
        )
    }

    private fun linkLocalPacienteToServer(existing: PacienteEntity, dto: PacienteDto): PacienteEntity {
        return existing.copy(
            serverId = dto.serverId,
            syncStatus = SyncStatus.SYNCED,
            updatedAt = dto.updatedAt.toDateLong(),
            lastSyncTimestamp = System.currentTimeMillis()
        )
    }

    private fun createNewPacienteFromDto(dto: PacienteDto): PacienteEntity {
        return dto.toPacienteEntity().copy(
            syncStatus = SyncStatus.SYNCED,
            lastSyncTimestamp = System.currentTimeMillis()
        )
    }

    // ==================== IMPLEMENTAÇÕES RESTANTES ====================
    // [Manter implementações existentes dos métodos da interface,
    //  aplicando os mesmos princípios de otimização onde aplicável]

    override suspend fun startSyncPacientes(): Flow<SyncState> = flow {
        updateSyncState("Sincronizando pacientes...", isLoading = true)
        emit(_syncState.value)

        try {
            if (!isNetworkAvailable()) {
                emitErrorState("Sem conexão com a internet")
                return@flow
            }

            coroutineScope {
                val (uploadResult, downloadResult) = awaitAll(
                    async { uploadPendingPacientes() },
                    async { downloadUpdatedPacientes() }
                )

                if (downloadResult.isSuccess) {
                    emitSuccessState("Pacientes sincronizados com sucesso!")
                } else {
                    emitErrorState(downloadResult.exceptionOrNull()?.message ?: "Erro ao sincronizar pacientes")
                }
            }
        } catch (e: Exception) {
            emitErrorState(e.message ?: "Erro inesperado")
        }
    }

    override suspend fun startSyncEspecialidades(): Flow<SyncState> = flow {
        updateSyncState("Sincronizando especialidades...", isLoading = true)
        emit(_syncState.value)

        try {
            val result = syncEspecialidades()
            if (result.isSuccess) {
                emitSuccessState("Especialidades sincronizadas com sucesso!")
            } else {
                emitErrorState(result.exceptionOrNull()?.message ?: "Erro ao sincronizar especialidades")
            }
        } catch (e: Exception) {
            emitErrorState(e.message ?: "Erro inesperado")
        }
    }

    // [Demais métodos mantidos com otimizações similares...]

    // ==================== MÉTODOS DE REDE E ESTADO ====================
    private fun isNetworkAvailable(): Boolean {
        return networkManager.checkConnection()
    }

    private suspend fun isNetworkAndServerAvailable(): Boolean {
        // Verifica se tem rede (local)
        val hasNetwork = networkManager.checkConnection()
        if (!hasNetwork) {
            Log.d(TAG, "❌ Sem rede disponível")
            return false
        }

        // Testa o servidor
        val hasServer = networkManager.testServerConnection()
        Log.d(TAG, "🔍 Rede: $hasNetwork, Servidor: $hasServer")
        return hasServer
    }

    private fun updateSyncState(
        message: String,
        currentStep: Int = 0,
        totalSteps: Int = 0,
        isLoading: Boolean = false
    ) {
        _syncState.value = _syncState.value.copy(
            isLoading = isLoading,
            message = message,
            totalItems = totalSteps,
            processedItems = currentStep,
            error = null
        )
    }

    private suspend fun emitErrorState(message: String) {
        val errorState = SyncState(
            isLoading = false,
            error = message,
            message = "Erro na sincronização"
        )
        _syncState.value = errorState
        _syncState.emit(errorState)
    }

    private suspend fun emitSuccessState(message: String, processedItems: Int = 0) {
        val successState = SyncState(
            isLoading = false,
            message = message,
            lastSyncTime = System.currentTimeMillis(),
            totalItems = processedItems,
            processedItems = processedItems
        )
        _syncState.value = successState
        _syncState.emit(successState)
    }

    // ==================== IMPLEMENTAÇÕES COMPLETAS DOS MÉTODOS DA INTERFACE ====================

    override suspend fun syncAll(): Result<Unit> = coroutineScope {
        try {
            Log.d(TAG, "🚀 Iniciando sincronização completa otimizada")

            // Executar em paralelo quando possível
            val especialidadesResult = async { syncEspecialidades() }

            // Aguardar especialidades antes de continuar (dependência crítica)
            val espResult = especialidadesResult.await()
            if (espResult.isFailure) return@coroutineScope espResult

            // Sincronizar pacientes e relacionamentos em paralelo
            val (pacientesResult, relacionamentosUpload, relacionamentosDownload) = awaitAll(
                async { syncPacientes() },
                async { uploadPendingPacienteEspecialidades() },
                async { downloadUpdatedPacienteEspecialidades() }
            )

            // Log de resultados
            if (pacientesResult.isFailure) {
                Log.w(TAG, "Falha parcial em pacientes: ${pacientesResult.exceptionOrNull()?.message}")
            }
            if (relacionamentosUpload.isFailure) {
                Log.w(TAG, "Falha parcial em upload relacionamentos: ${relacionamentosUpload.exceptionOrNull()?.message}")
            }
            if (relacionamentosDownload.isFailure) {
                Log.w(TAG, "Falha parcial em download relacionamentos: ${relacionamentosDownload.exceptionOrNull()?.message}")
            }

            updateLastSyncTimestamp(System.currentTimeMillis())
            clearCache()

            Log.d(TAG, "✅ Sincronização completa finalizada")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "💥 Erro na sincronização completa", e)
            Result.failure(e)
        }
    }

    override suspend fun syncPacientes(): Result<Unit> = coroutineScope {
        try {
            Log.d(TAG, "Iniciando sincronização otimizada de pacientes")

            if (!isNetworkAndServerAvailable()) {
                return@coroutineScope Result.failure(Exception("Conectividade não disponível"))
            }

            // Upload e download em paralelo
            val (uploadResult, downloadResult) = awaitAll(
                async { uploadPendingPacientes() },
                async { downloadUpdatedPacientes() }
            )

            if (uploadResult.isFailure) {
                Log.w(TAG, "Falha no upload: ${uploadResult.exceptionOrNull()?.message}")
            }

            if (downloadResult.isFailure) {
                Log.e(TAG, "Falha no download: ${downloadResult.exceptionOrNull()?.message}")
                return@coroutineScope downloadResult
            }

            Log.d(TAG, "✅ Sincronização de pacientes concluída")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Erro em syncPacientes", e)
            Result.failure(e)
        }
    }

    override suspend fun hasPendingChanges(): Boolean {
        return try {
            coroutineScope {
                val checks = awaitAll(
                    async { pacienteDao.getUnsyncedCount() > 0 },
                    async { especialidadeRepository.hasPendingChanges() },
                    async { pacienteEspecialidadeDao.countPendingUploads() > 0 }
                )
                checks.any { it }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao verificar mudanças pendentes", e)
            false
        }
    }

    // ==================== GERENCIAMENTO DE TIMESTAMP ====================
    @Volatile
    private var lastSyncTimestamp: Long = 0L

    override suspend fun getLastSyncTimestamp(): Long {
        if (lastSyncTimestamp == 0L) {
            // Carregar do SharedPreferences ou banco de dados
            // lastSyncTimestamp = sharedPreferences.getLong("last_sync_timestamp", 0L)
            lastSyncTimestamp = 0L // Placeholder
        }
        return lastSyncTimestamp
    }

    override suspend fun updateLastSyncTimestamp(timestamp: Long) {
        lastSyncTimestamp = timestamp
        // Salvar no SharedPreferences ou banco de dados
        // sharedPreferences.edit().putLong("last_sync_timestamp", timestamp).apply()
    }

    override fun clearError() {
        _syncState.value = _syncState.value.copy(error = null)
    }

    // ==================== MÉTODOS ESPECÍFICOS OTIMIZADOS ====================

    override suspend fun syncPacienteEspecialidadesOnly(): Result<Unit> = coroutineScope {
        try {
            Log.d(TAG, "Sincronização otimizada apenas de relacionamentos")

            if (!networkManager.checkConnection()) {
                return@coroutineScope Result.failure(Exception("Sem conexão com internet"))
            }

            val (uploadResult, downloadResult) = awaitAll(
                async { uploadPendingPacienteEspecialidades() },
                async { downloadUpdatedPacienteEspecialidades() }
            )

            if (uploadResult.isFailure) {
                Log.w(TAG, "Falha no upload: ${uploadResult.exceptionOrNull()?.message}")
            }

            if (downloadResult.isFailure) {
                Log.w(TAG, "Falha no download: ${downloadResult.exceptionOrNull()?.message}")
                return@coroutineScope downloadResult
            }

            Log.d(TAG, "✅ Sincronização de relacionamentos concluída")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Erro na sincronização de relacionamentos", e)
            Result.failure(e)
        }
    }

    override suspend fun uploadPacienteEspecialidadesPending(): Result<Unit> =
        uploadPendingPacienteEspecialidades()

    override suspend fun downloadPacienteEspecialidadesUpdated(): Result<Unit> =
        downloadUpdatedPacienteEspecialidades()

    override suspend fun syncPacientesUpdated(): Result<Unit> = coroutineScope {
        try {
            Log.d(TAG, "Sincronização otimizada de pacientes atualizados")

            if (!networkManager.checkConnection()) {
                return@coroutineScope Result.failure(Exception("Sem conexão com internet"))
            }

            val pacientesParaAtualizar = pacienteDao.getPacientesParaAtualizar(SyncStatus.PENDING_UPLOAD)
            Log.d(TAG, "Encontrados ${pacientesParaAtualizar.size} pacientes para atualizar")

            if (pacientesParaAtualizar.isEmpty()) {
                return@coroutineScope  Result.success(Unit)
            }

            val result = processarAtualizacaoPacientesOtimizado(pacientesParaAtualizar)

            Log.d(TAG, "✅ Sincronização de pacientes atualizados concluída")
            result

        } catch (e: Exception) {
            Log.e(TAG, "Erro na sincronização de pacientes atualizados", e)
            Result.failure(e)
        }
    }

    override suspend fun syncNovosPacientes(): Result<Unit> = coroutineScope {
        try {
            Log.d(TAG, "Sincronização otimizada de novos pacientes")

            if (!networkManager.checkConnection()) {
                return@coroutineScope Result.failure(Exception("Sem conexão com internet"))
            }

            val novosPacientes = pacienteDao.getNovosPacientes(SyncStatus.PENDING_UPLOAD)
            Log.d(TAG, "Encontrados ${novosPacientes.size} novos pacientes")

            if (novosPacientes.isEmpty()) {
                return@coroutineScope Result.success(Unit)
            }

            val result = processarCriacaoPacientesOtimizado(novosPacientes)

            Log.d(TAG, "✅ Sincronização de novos pacientes concluída")
            result

        } catch (e: Exception) {
            Log.e(TAG, "Erro na sincronização de novos pacientes", e)
            Result.failure(e)
        }
    }

    override suspend fun syncAllPendingPacientes(): Result<Unit> = uploadPendingPacientes()

    override suspend fun syncPacienteRelationshipsOnly(pacienteLocalId: String): Result<Unit> = coroutineScope {
        try {
            Log.d(TAG, "🎯 Sincronização otimizada de relacionamentos para: $pacienteLocalId")

            if (!isNetworkAvailable()) {
                return@coroutineScope Result.failure(Exception("Sem conexão com a internet"))
            }

            val allPendingRelations = pacienteEspecialidadeDao.getItemsNeedingSync()
            val pacientePendingRelations = allPendingRelations.filter {
                it.pacienteLocalId == pacienteLocalId
            }

            if (pacientePendingRelations.isEmpty()) {
                Log.d(TAG, "Nenhum relacionamento pendente")
                return@coroutineScope Result.success(Unit)
            }

            val forDelete = pacientePendingRelations.filter { it.syncStatus == SyncStatus.PENDING_DELETE }
            val forCreate = pacientePendingRelations.filter {
                it.syncStatus == SyncStatus.PENDING_UPLOAD && !it.isDeleted
            }

            // Executar operações em paralelo
            if (forDelete.isNotEmpty() || forCreate.isNotEmpty()) {
                val results = awaitAll(
                    async {
                        if (forDelete.isNotEmpty()) processarDelecaoRelacionamentosOtimizado(forDelete)
                        else Result.success(Unit)
                    },
                    async {
                        if (forCreate.isNotEmpty()) processarCriacaoRelacionamentosOtimizado(forCreate)
                        else Result.success(Unit)
                    }
                )
            }

            Log.d(TAG, "✅ Relacionamentos sincronizados para: $pacienteLocalId")
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "💥 Erro na sincronização de relacionamentos: $pacienteLocalId", e)
            Result.failure(e)
        }
    }

    override suspend fun auditFichas(): Result<Unit> {
        return try {
            auditarFichas()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== MÉTODOS AUXILIARES COMPLEMENTARES ====================

    private suspend fun processarAtualizacaoPacientesOtimizado(pacientes: List<PacienteEntity>): Result<Unit> = coroutineScope {
        try {
            // Processar em chunks paralelos
            val chunks = pacientes.chunked(CONCURRENT_REQUESTS)

            chunks.forEach { chunk ->
                val jobs = chunk.map { paciente ->
                    async { processarAtualizacaoIndividual(paciente) }
                }
                jobs.awaitAll()
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Erro na atualização otimizada", e)
            Result.failure(e)
        }
    }

    private suspend fun processarAtualizacaoIndividual(entity: PacienteEntity) {
        try {
            val result = tentarAtualizarPaciente(entity)

            if (result.isSuccess) {
                pacienteDao.updateSyncStatus(entity.localId, SyncStatus.SYNCED)

                // Sincronizar relacionamentos se necessário
                try {
                    syncPacienteRelationships(entity.localId)
                } catch (relationshipError: Exception) {
                    Log.w(TAG, "⚠️ Erro nos relacionamentos", relationshipError)
                }

                Log.d(TAG, "✅ Paciente ${entity.localId} atualizado")
            } else {
                pacienteDao.updateSyncStatus(entity.localId, SyncStatus.UPLOAD_FAILED)
                Log.e(TAG, "❌ Falha: ${result.exceptionOrNull()?.message}")
            }

        } catch (e: Exception) {
            pacienteDao.incrementSyncAttempts(entity.localId, System.currentTimeMillis(), e.message)
            Log.e(TAG, "💥 Erro na atualização individual", e)
        }
    }

    private suspend fun processarDelecaoPacientesOtimizado(pacientes: List<PacienteEntity>): Result<Unit> = coroutineScope {
        try {
            // Processar deleções em paralelo
            val jobs = pacientes.map { paciente ->
                async { processarDelecaoIndividual(paciente) }
            }

            jobs.awaitAll()
            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Erro na deleção otimizada", e)
            Result.failure(e)
        }
    }

    private suspend fun processarDelecaoIndividual(entity: PacienteEntity) {
        try {
            val response = apiService.deletePaciente(entity.serverId!!)

            if (response.isSuccessful) {
                pacienteDao.deletePacientePermanently(entity.localId)
                Log.d(TAG, "✅ Paciente ${entity.localId} deletado")
            } else {
                pacienteDao.updateSyncStatus(entity.localId, SyncStatus.DELETE_FAILED)
                Log.e(TAG, "❌ Falha na deleção: ${response.message()}")
            }
        } catch (e: Exception) {
            pacienteDao.incrementSyncAttempts(entity.localId, System.currentTimeMillis(), e.message)
            Log.e(TAG, "💥 Erro na deleção individual", e)
        }
    }

    private suspend fun processarDelecaoRelacionamentosOtimizado(relations: List<PacienteEspecialidadeEntity>) = coroutineScope {
        try {
            Log.d(TAG, "=== DELEÇÃO OTIMIZADA DE RELACIONAMENTOS ===")

            // Cache para reduzir consultas
            val pacientesCache = getCachedPacientes()
            val especialidadesCache = getCachedEspecialidades()

            // Processar em paralelo com limite
            val chunks = relations.chunked(CONCURRENT_REQUESTS)

            chunks.forEach { chunk ->
                val jobs = chunk.map { relation ->
                    async {
                        processarDelecaoRelacionamentoIndividual(
                            relation,
                            pacientesCache,
                            especialidadesCache
                        )
                    }
                }
                jobs.awaitAll()
            }

            Log.d(TAG, "✅ Deleção de relacionamentos concluída")

        } catch (e: Exception) {
            Log.e(TAG, "Erro na deleção de relacionamentos", e)
        }
    }

    private suspend fun processarDelecaoRelacionamentoIndividual(
        entity: PacienteEspecialidadeEntity,
        pacientesCache: Map<Long, PacienteEntity>,
        especialidadesCache: Map<Long, EspecialidadeEntity>
    ) {
        try {
            val paciente = pacientesCache.values.find { it.localId == entity.pacienteLocalId }
            val especialidade = especialidadesCache.values.find { it.localId == entity.especialidadeLocalId }

            if (paciente?.serverId != null && especialidade?.serverId != null) {
                val response = apiService.deleteByPathVariables(
                    pacienteId = paciente.serverId.toInt(),
                    especialidadeId = especialidade.serverId.toInt()
                )

                if (response.isSuccessful) {
                    pacienteEspecialidadeDao.deletePermanently(
                        entity.pacienteLocalId,
                        entity.especialidadeLocalId
                    )
                    especialidadeDao.incrementarFichas(especialidade.localId)
                    Log.d(TAG, "✅ Relacionamento deletado e fichas incrementadas")
                } else {
                    pacienteEspecialidadeDao.updateSyncStatus(
                        entity.pacienteLocalId,
                        entity.especialidadeLocalId,
                        SyncStatus.DELETE_FAILED
                    )
                    Log.e(TAG, "❌ Falha na deleção: ${response.code()}")
                }
            } else {
                // Deletar local se não tem serverIds
                pacienteEspecialidadeDao.deletePermanently(
                    entity.pacienteLocalId,
                    entity.especialidadeLocalId
                )
                if (especialidade != null) {
                    especialidadeDao.incrementarFichas(especialidade.localId)
                }
                Log.d(TAG, "✅ Relacionamento local deletado")
            }

        } catch (e: Exception) {
            Log.e(TAG, "💥 Erro na deleção individual", e)
            pacienteEspecialidadeDao.incrementSyncAttempts(
                entity.pacienteLocalId,
                entity.especialidadeLocalId,
                System.currentTimeMillis(),
                e.message
            )
        }
    }

    // ==================== MÉTODOS DE PROCESSAMENTO E DOWNLOAD ====================

    private suspend fun downloadUpdatedPacienteEspecialidades(): Result<Unit> {
        return try {
            Log.d(TAG, "Download otimizado de relacionamentos")

            val lastSync = getLastSyncTimestamp()
            val response = apiService.getUpdatedPacienteEspecialidades(lastSync)

            if (!response.isSuccessful || response.body()?.success != true) {
                return Result.failure(Exception("Erro API: ${response.code()}"))
            }

            val relationsDto = response.body()?.data ?: emptyList()
            Log.d(TAG, "Recebidos ${relationsDto.size} relacionamentos")

            if (relationsDto.isNotEmpty()) {
                processarRelacionamentosDoServidorOtimizado(relationsDto)
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Erro no download de relacionamentos", e)
            Result.failure(e)
        }
    }

    private suspend fun processarRelacionamentosDoServidorOtimizado(relationsDto: List<PacienteEspecialidadeDTO>) {
        Log.d(TAG, "Processamento otimizado de ${relationsDto.size} relacionamentos")

        // Cache para reduzir consultas
        val pacientesCache = getCachedPacientes()
        val especialidadesCache = getCachedEspecialidades()

        // Separar operações em lotes
        val paraInserir = mutableListOf<PacienteEspecialidadeEntity>()
        val paraAtualizar = mutableListOf<PacienteEspecialidadeEntity>()
        val paraDeletar = mutableListOf<Pair<String, String>>() // pacienteLocalId, especialidadeLocalId

        relationsDto.forEach { dto ->
            try {
                if (dto.pacienteServerId == null || dto.especialidadeServerId == null) {
                    return@forEach
                }

                val paciente = pacientesCache[dto.pacienteServerId]
                val especialidade = especialidadesCache[dto.especialidadeServerId]

                if (paciente != null && especialidade != null) {
                    if (dto.isDeleted) {
                        paraDeletar.add(Pair(paciente.localId, especialidade.localId))
                    } else {
                        val existing = pacienteEspecialidadeDao.getById(paciente.localId, especialidade.localId)

                        if (existing == null) {
                            paraInserir.add(createNewRelationshipFromDto(dto, paciente, especialidade))
                        } else {
                            val dtoUpdatedAt = dto.updatedAt.toIsoDateLong()
                            if (dtoUpdatedAt > existing.updatedAt) {
                                paraAtualizar.add(updateExistingRelationship(existing, dto))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao processar relacionamento individual", e)
            }
        }

        // Executar operações em batch
        if (paraDeletar.isNotEmpty()) {
            pacienteEspecialidadeDao.deleteBatch(paraDeletar)
            Log.d(TAG, "✅ ${paraDeletar.size} relacionamentos deletados")
        }

        if (paraInserir.isNotEmpty()) {
            pacienteEspecialidadeDao.insertBatch(paraInserir)
            Log.d(TAG, "✅ ${paraInserir.size} relacionamentos inseridos")
        }

        if (paraAtualizar.isNotEmpty()) {
            pacienteEspecialidadeDao.updateBatch(paraAtualizar)
            Log.d(TAG, "✅ ${paraAtualizar.size} relacionamentos atualizados")
        }
    }

    private fun createNewRelationshipFromDto(
        dto: PacienteEspecialidadeDTO,
        paciente: PacienteEntity,
        especialidade: EspecialidadeEntity
    ): PacienteEspecialidadeEntity {
        return dto.toEntity(
            pacienteLocalId = paciente.localId,
            especialidadeLocalId = especialidade.localId,
            deviceId = "server",
            syncStatus = SyncStatus.SYNCED
        )
    }

    private fun updateExistingRelationship(
        existing: PacienteEspecialidadeEntity,
        dto: PacienteEspecialidadeDTO
    ): PacienteEspecialidadeEntity {
        return existing.copy(
            dataAtendimento = dto.dataAtendimento?.toDateLong(),
            pacienteServerId = dto.pacienteServerId,
            especialidadeServerId = dto.especialidadeServerId,
            syncStatus = SyncStatus.SYNCED,
            updatedAt = dto.updatedAt.toIsoDateLong(),
            lastSyncTimestamp = System.currentTimeMillis(),
            isDeleted = false
        )
    }

    // ==================== MÉTODOS AUXILIARES FINAIS ====================

    private suspend fun tentarAtualizarPaciente(entity: PacienteEntity): Result<Any> {
        return try {
            val pacienteDto = entity.toPacienteDto()
            val response = apiService.updatePaciente(entity.serverId!!, pacienteDto)

            if (response.isSuccessful) {
                val responseBody = response.body()
                if (responseBody?.success == true) {
                    Result.success(responseBody)
                } else {
                    Result.failure(Exception(responseBody?.error ?: "API indica falha"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(Exception("HTTP ${response.code()}: ${response.message()} - $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun syncPacienteRelationships(pacienteLocalId: String): Result<Unit> {
        return syncPacienteRelationshipsOnly(pacienteLocalId)
    }

    private suspend fun getPacientesPendentes(): List<PacienteEntity> {
        val statusPendentes = listOf(
            SyncStatus.PENDING_UPLOAD,
            SyncStatus.PENDING_DELETE,
            SyncStatus.UPLOAD_FAILED,
            SyncStatus.DELETE_FAILED
        )
        return pacienteDao.getItemsNeedingSync(statusPendentes)
    }

    private fun categorizarPacientesPorOperacao(pacientes: List<PacienteEntity>): Map<String, List<PacienteEntity>> {
        return mapOf(
            "create" to pacientes.filter {
                it.syncStatus == SyncStatus.PENDING_UPLOAD && it.serverId == null && !it.isDeleted
            },
            "update" to pacientes.filter {
                it.syncStatus == SyncStatus.PENDING_UPLOAD && it.serverId != null && !it.isDeleted
            },
            "delete" to pacientes.filter {
                (it.syncStatus == SyncStatus.PENDING_DELETE || it.isDeleted) && it.serverId != null
            }
        )
    }

    private suspend fun uploadPendingPacienteEspecialidades(): Result<Unit> {
        return try {
            Log.d(TAG, "Upload otimizado de relacionamentos pendentes")

            val pendingRelations = pacienteEspecialidadeDao.getItemsNeedingSync()
            if (pendingRelations.isEmpty()) {
                return Result.success(Unit)
            }

            val forCreate = pendingRelations.filter {
                it.syncStatus == SyncStatus.PENDING_UPLOAD && !it.isDeleted
            }
            val forDelete = pendingRelations.filter {
                it.syncStatus == SyncStatus.PENDING_DELETE
            }

            // Executar em paralelo
            coroutineScope {
                val results = awaitAll(
                    async {
                        if (forCreate.isNotEmpty()) processarCriacaoRelacionamentosOtimizado(forCreate)
                        else Result.success(Unit)
                    },
                    async {
                        if (forDelete.isNotEmpty()) processarDelecaoRelacionamentosOtimizado(forDelete)
                        else Result.success(Unit)
                    }
                )
            }

            Result.success(Unit)

        } catch (e: Exception) {
            Log.e(TAG, "Erro no upload de relacionamentos", e)
            Result.failure(e)
        }
    }

    private suspend fun auditarFichas() {
        try {
            Log.d(TAG, "=== AUDITORIA OTIMIZADA DE FICHAS ===")

            val auditoria = pacienteEspecialidadeDao.getAuditoriaFichas()

            auditoria.forEach { item ->
                Log.d(TAG, "📊 ${item.nome}: ${item.fichas} fichas, ${item.relacionamentosAtivos} relacionamentos")

                when {
                    item.fichas <= 0 -> Log.w(TAG, "⚠️ ESGOTADA: ${item.nome}")
                    item.fichas <= 5 -> Log.w(TAG, "⚠️ POUCAS FICHAS: ${item.nome} (${item.fichas})")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Erro na auditoria", e)
        }
    }
}
