package com.example.projeto_ibg3.sync.service

import android.content.Context
import androidx.work.*
import com.example.projeto_ibg3.data.local.database.AppDatabase
import com.example.projeto_ibg3.data.remote.api.ApiService
import java.util.UUID
import java.util.concurrent.TimeUnit
import com.google.gson.Gson
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.projeto_ibg3.data.local.database.entities.PacienteEntity
import com.example.projeto_ibg3.data.local.database.entities.PacienteEspecialidadeEntity
import com.example.projeto_ibg3.data.remote.dto.*
import com.example.projeto_ibg3.domain.model.Especialidade
import com.example.projeto_ibg3.domain.model.PacienteEspecialidade
import com.example.projeto_ibg3.domain.model.SyncStatus
import com.example.projeto_ibg3.data.local.database.entities.SyncQueue
import com.example.projeto_ibg3.core.extensions.deleteAllEspecialidades
import com.example.projeto_ibg3.data.mappers.toEntityList
import com.example.projeto_ibg3.core.constants.SyncConstants
import com.example.projeto_ibg3.data.mappers.PacienteEspecialidadeMapper
import com.example.projeto_ibg3.data.mappers.dateFormat
import com.example.projeto_ibg3.data.mappers.toDateLong
import com.example.projeto_ibg3.data.mappers.toDateTimeString
import com.example.projeto_ibg3.sync.worker.SyncWorker
import com.example.projeto_ibg3.sync.model.SyncResult
import com.example.projeto_ibg3.sync.extension.toSyncError
import java.util.Calendar
import java.util.Date


class SyncService(
    private val database: AppDatabase,
    private val apiService: ApiService,
    private val context: Context,
    private val pacienteEspecialidadeMapper: PacienteEspecialidadeMapper // ADICIONE ESTA DEPENDÊNCIA

) {

    suspend fun syncData(): SyncResult {
        if (!isNetworkAvailable()) {
            return SyncResult.NO_NETWORK
        }

        try {
            // 1. Sincronizar especialidades primeiro
            syncEspecialidades()

            // 2. Processar fila de sincronização de pacientes
            val pendingItems = database.syncQueueDao().getAllPendingItems()

            if (pendingItems.isEmpty()) {
                return SyncResult.SUCCESS(
                    syncedCount = 0,
                    message = "Nenhum item pendente para sincronizar"
                )
            }

            // 3. Separar operações por tipo
            val (createList, updateList, deleteList) = separateOperationsByType(pendingItems)

            var syncedCount = 0
            var failedCount = 0

            // 4. Executar operações em lote
            syncedCount += processBatchCreate(createList)
            syncedCount += processBatchUpdate(updateList)
            syncedCount += processBatchDelete(deleteList)

            // 5. Baixar dados atualizados do servidor
            downloadServerUpdates()

            // 6. Marcar itens como sincronizados
            markItemsAsSynced(pendingItems)

            // 7. Salvar timestamp da sincronização
            saveLastSyncTimestamp(System.currentTimeMillis())

            return SyncResult.SUCCESS(
                syncedCount = syncedCount,
                failedCount = failedCount,
                message = "Sincronização concluída com sucesso"
            )

        } catch (e: Exception) {
            return SyncResult.ERROR(error = e.toSyncError())
        }
    }

    private suspend fun syncEspecialidades() {
        try {
            val response = apiService.getAllEspecialidades()
            if (response.isSuccessful) {
                response.body()?.let { apiResponse ->
                    if (apiResponse.success && apiResponse.data != null) {
                        updateEspecialidades(apiResponse.data) //  List<EspecialidadeDto>
                    }
                }
            }
        } catch (e: Exception) {
            println("Erro ao sincronizar especialidades: ${e.message}")
        }
    }

    private suspend fun separateOperationsByType(
        pendingItems: List<SyncQueue>
    ): Triple<List<PacienteDto>, List<PacienteDto>, List<Long>> {
        val createList = mutableListOf<PacienteDto>()
        val updateList = mutableListOf<PacienteDto>()
        val deleteList = mutableListOf<Long>()

        for (item in pendingItems) {
            if (item.entityType == SyncConstants.EntityTypes.PACIENTE) {
                val syncData = Gson().fromJson(item.jsonData, SyncPacienteData::class.java)

                when (item.operation) {
                    SyncConstants.Operations.CREATE -> {
                        createList.add(convertToPacienteDto(syncData))
                    }
                    SyncConstants.Operations.UPDATE -> {
                        updateList.add(convertToPacienteDto(syncData))
                    }
                    SyncConstants.Operations.DELETE -> {
                        // Verificar se serverId não é null e é um número válido
                        syncData.serverId?.let { serverIdString ->
                            try {
                                val serverIdLong = serverIdString.toLong()
                                deleteList.add(serverIdLong)
                            } catch (e: NumberFormatException) {
                                println("Erro: serverId não é um número válido: $serverIdString")
                                // Ou usar logging apropriado aqui
                            }
                        } ?: run {
                            println("Aviso: serverId é null para operação DELETE")
                        }
                    }
                }
            }
        }

        return Triple(createList, updateList, deleteList)
    }

    private fun convertToPacienteDto(syncData: SyncPacienteData): PacienteDto {
        return PacienteDto(
            serverId = syncData.serverId,
            nome = syncData.nome,
            dataNascimento = syncData.dataNascimento,
            idade = syncData.idade,
            nomeDaMae = syncData.nomeDaMae,
            cpf = syncData.cpf,
            sus = syncData.sus,
            telefone = syncData.telefone,
            endereco = syncData.endereco,
            lastSyncTimestamp = syncData.lastSyncTimestamp,
            paXMmhg = null,
            fcBpm = null,
            frIbpm = null,
            temperaturaC = null,
            hgtMgld = null,
            spo2 = null,
            peso = null,
            altura = null,
            imc = null,
            createdAt = System.currentTimeMillis().toDateTimeString(),
            updatedAt = System.currentTimeMillis().toDateTimeString(),
            deviceId = getDeviceId()
        )
    }

    private suspend fun processBatchCreate(createList: List<PacienteDto>): Int {
        if (createList.isEmpty()) return 0

        return try {
            val response = apiService.createPacientesBatch(createList)
            if (response.isSuccessful) {
                response.body()?.let { apiResponse ->
                    if (apiResponse.success && apiResponse.data != null) {
                        updateLocalPacientesWithServerIds(apiResponse.data)
                        apiResponse.data.size // List<PacienteDto>.size
                    } else {
                        0
                    }
                } ?: 0
            } else {
                0
            }
        } catch (e: Exception) {
            println("Erro ao criar pacientes em lote: ${e.message}")
            0
        }
    }

    private suspend fun processBatchUpdate(updateList: List<PacienteDto>): Int {
        if (updateList.isEmpty()) return 0

        return try {
            val response = apiService.updatePacientesBatch(updateList)
            if (response.isSuccessful) {
                response.body()?.let { apiResponse ->
                    if (apiResponse.success && apiResponse.data != null) {
                        updateLocalPacientesFromServer(apiResponse.data)
                        apiResponse.data.size
                    } else {
                        0
                    }
                } ?: 0
            } else {
                0
            }
        } catch (e: Exception) {
            println("Erro ao atualizar pacientes em lote: ${e.message}")
            0
        }
    }

    private suspend fun processBatchDelete(deleteList: List<Long>): Int {
        if (deleteList.isEmpty()) return 0

        return try {
            val response = apiService.deletePacientesBatch(deleteList)
            if (response.isSuccessful) {
                // Remover pacientes do banco local
                deleteLocalPacientes(deleteList)
                deleteList.size
            } else {
                0
            }
        } catch (e: Exception) {
            println("Erro ao deletar pacientes em lote: ${e.message}")
            0
        }
    }

    private suspend fun downloadServerUpdates() {
        try {
            val lastSync = getLastSyncTimestamp()
            val response = apiService.getUpdatedPacientes(lastSync)

            if (response.isSuccessful) {
                response.body()?.let { apiResponse ->
                    if (apiResponse.success && apiResponse.data != null) {
                        updateLocalPacientesFromServer(apiResponse.data)
                    }
                }
            }
        } catch (e: Exception) {
            println("Erro ao baixar atualizações do servidor: ${e.message}")
        }
    }

    private suspend fun updateLocalPacientesWithServerIds(createdPacientes: List<PacienteDto>) {
        for (pacienteDto in createdPacientes) {
            // Usar CPF para encontrar o paciente local
            val localPaciente = database.pacienteDao().getPacienteByCpf(pacienteDto.cpf)
            localPaciente?.let { paciente ->
                pacienteDto.serverId?.let { serverId ->
                    database.pacienteDao().updateSyncStatusAndServerId(
                        localId = paciente.localId,
                        status = SyncStatus.SYNCED,
                        serverId = serverId,
                        timestamp = System.currentTimeMillis()
                    )
                }
            }
        }
    }

    private suspend fun updateLocalPacientesFromServer(pacientesServidor: List<PacienteDto>) {
        for (pacienteDto in pacientesServidor) {
            updateLocalPacienteFromServer(pacienteDto)
        }
    }

    private suspend fun deleteLocalPacientes(serverIds: List<Long>) {
        for (serverId in serverIds) {
            // Isso está correto - getPacienteByServerId() espera Long
            val pacienteEntity = database.pacienteDao().getPacienteByServerId(serverId)
            pacienteEntity?.let {
                // Usar o método correto do DAO para marcar como deletado
                database.pacienteDao().markAsDeleted(it.localId)
            }
        }
    }

    private suspend fun updateEspecialidades(especialidades: List<EspecialidadeDto>) {
        val especialidadesLocal = especialidades.map { dto ->
            Especialidade(
                localId = UUID.randomUUID().toString(), // Gerar novo localId
                serverId = dto.serverId,
                nome = dto.nome,
                fichas = dto.fichas ?: 0,
                atendimentosRestantesHoje = dto.atendimentosRestantesHoje,
                atendimentosTotaisHoje = dto.atendimentosTotaisHoje,
                createdAt = dto.createdAt.toDateLong() ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis(),
                isDeleted = false
            )
        }

        val especialidadesEntity = especialidadesLocal.toEntityList(
            deviceId = getDeviceId(),
            syncStatus = SyncStatus.SYNCED,
        )

        database.especialidadeDao().deleteAllEspecialidades()
        database.especialidadeDao().insertEspecialidades(especialidadesEntity)
    }

    private suspend fun updateLocalPacienteFromServer(pacienteDto: PacienteDto) {
        // Primeiro tentar por serverId, depois por CPF
        val existingPaciente = pacienteDto.serverId?.let { serverId ->
            database.pacienteDao().getPacienteByServerId(serverId)
        } ?: database.pacienteDao().getPacienteByCpf(pacienteDto.cpf)

        if (existingPaciente != null) {
            // Atualizar paciente existente
            val updatedPaciente = existingPaciente.copy(
                serverId = pacienteDto.serverId,
                nome = pacienteDto.nome,
                dataNascimento = convertDateStringToTimestamp(pacienteDto.dataNascimento),
                idade = pacienteDto.idade,
                nomeDaMae = pacienteDto.nomeDaMae ?: "",
                cpf = pacienteDto.cpf,
                sus = pacienteDto.sus ?: "",
                telefone = pacienteDto.telefone ?: "",
                endereco = pacienteDto.endereco ?: "",
                syncStatus = SyncStatus.SYNCED,
                lastSyncTimestamp = System.currentTimeMillis()
            )

            database.pacienteDao().updatePaciente(updatedPaciente)
        } else {
            // Criar novo paciente (dados vindos do servidor)
            val newPaciente = PacienteEntity(
                localId = UUID.randomUUID().toString(), // Gerar novo localId
                serverId = pacienteDto.serverId,
                nome = pacienteDto.nome,
                dataNascimento = convertDateStringToTimestamp(pacienteDto.dataNascimento),
                idade = pacienteDto.idade,
                nomeDaMae = pacienteDto.nomeDaMae ?: "",
                cpf = pacienteDto.cpf,
                sus = pacienteDto.sus ?: "",
                telefone = pacienteDto.telefone ?: "",
                endereco = pacienteDto.endereco ?: "",
                syncStatus = SyncStatus.SYNCED,
                lastSyncTimestamp = System.currentTimeMillis(),
                createdAt = pacienteDto.createdAt.toDateLong() ?: System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )

            database.pacienteDao().insertPaciente(newPaciente)
        }

        // Atualizar especialidades do paciente
        updatePacienteEspecialidades(pacienteDto)
    }

    // Método auxiliar para converter data
    private fun convertDateStringToTimestamp(dateString: String?): Long {
        return if (dateString.isNullOrEmpty()) {
            System.currentTimeMillis()
        } else {
            try {
                // Assumindo formato ISO: "2023-01-15"
                val parts = dateString.split("-")
                if (parts.size == 3) {
                    val year = parts[0].toInt()
                    val month = parts[1].toInt() - 1 // Calendar usa 0-based
                    val day = parts[2].toInt()

                    Calendar.getInstance().apply {
                        set(year, month, day)
                    }.timeInMillis
                } else {
                    System.currentTimeMillis()
                }
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        }
    }

    // Método separado para especialidades
    private suspend fun updatePacienteEspecialidades(pacienteDto: PacienteDto) {
        // Buscar o paciente pelo serverId ou CPF para obter o localId
        val paciente = pacienteDto.serverId?.let { serverId ->
            database.pacienteDao().getPacienteByServerId(serverId)
        } ?: database.pacienteDao().getPacienteByCpf(pacienteDto.cpf)

        val pacienteLocalId = paciente?.localId ?: return
        val pacienteServerId = pacienteDto.serverId

        // Se o paciente tem serverId, buscar especialidades do servidor
        if (pacienteServerId != null) {
            syncPacienteEspecialidadesFromServer(pacienteLocalId, pacienteServerId)
        }
    }

    // Sincronizar especialidades do servidor
    private suspend fun syncPacienteEspecialidadesFromServer(pacienteLocalId: String, pacienteServerId: Long) {
        try {
            val response = apiService.getPacienteEspecialidades(pacienteServerId)

            if (response.isSuccessful) {
                response.body()?.let { apiResponse ->
                    if (apiResponse.success && apiResponse.data != null) {
                        updateLocalPacienteEspecialidades(pacienteLocalId, apiResponse.data) // ✅ CORRETO
                    }
                }
            }
        } catch (e: Exception) {
            println("Erro ao sincronizar especialidades do paciente $pacienteServerId: ${e.message}")
        }
    }

    // Atualizar especialidades locais com dados do servidor
    // Método updateLocalPacienteEspecialidades
    private suspend fun updateLocalPacienteEspecialidades(
        pacienteLocalId: String,
        especialidadeRelations: List<PacienteEspecialidadeDTO>
    ) {
        try {
            // Remover especialidades existentes para este paciente
            database.pacienteEspecialidadeDao().deleteByPacienteId(pacienteLocalId)

            // Converter DTOs para entities usando o mapper
            val (entities, errors) = pacienteEspecialidadeMapper.dtoListToEntityListSafe(
                dtoList = especialidadeRelations,
                deviceId = getDeviceId(),
                syncStatus = SyncStatus.SYNCED
            )

            // Log dos erros se houver
            if (errors.isNotEmpty()) {
                println("Erros ao converter especialidades para o paciente $pacienteLocalId:")
                errors.forEach { error -> println("  - $error") }
            }

            // Inserir no banco apenas os que foram convertidos com sucesso
            if (entities.isNotEmpty()) {
                database.pacienteEspecialidadeDao().insertAll(entities)
                println("${entities.size} especialidades inseridas para o paciente $pacienteLocalId")
            }

        } catch (e: Exception) {
            println("Erro ao atualizar especialidades do paciente $pacienteLocalId: ${e.message}")
            throw e
        }
    }


    // Sincronizar especialidades pendentes (CREATE/UPDATE/DELETE)
    private suspend fun syncPacienteEspecialidadesPendentes() {
        try {
            // Usar o método correto do DAO que você já tem
            val pendingRelations = database.pacienteEspecialidadeDao().getPendingSync()

            if (pendingRelations.isNotEmpty()) {
                val relationsDto = pendingRelations.map { entity ->
                    convertEntityToPacienteEspecialidadeDTO(entity)
                }

                // Enviar para o servidor
                val response = apiService.syncPacienteEspecialidades(relationsDto)

                if (response.isSuccessful) {
                    // Marcar como sincronizado usando o método correto
                    pendingRelations.forEach { entity ->
                        database.pacienteEspecialidadeDao().updateSyncStatus(
                            entity.pacienteLocalId,
                            entity.especialidadeLocalId,
                            SyncStatus.SYNCED
                        )
                    }
                }
            }
        } catch (e: Exception) {
            println("Erro ao sincronizar especialidades pendentes: ${e.message}")
        }
    }

    // Para conversão de Entity para DTO
    private suspend fun convertEntityToPacienteEspecialidadeDTO(entity: PacienteEspecialidadeEntity): PacienteEspecialidadeDTO {
        // Buscar serverId do paciente
        val paciente = database.pacienteDao().getPacienteByLocalId(entity.pacienteLocalId)
        val pacienteServerId = paciente?.serverId

        // Buscar serverId da especialidade
        val especialidade = database.especialidadeDao().getEspecialidadeByLocalId(entity.especialidadeLocalId)
        val especialidadeServerId = especialidade?.serverId

        return PacienteEspecialidadeDTO(
            pacienteServerId = pacienteServerId,
            especialidadeServerId = especialidadeServerId,
            dataAtendimento = safeFormatDate(entity.dataAtendimento),
            createdAt = entity.createdAt.toDateTimeString(),
            updatedAt = entity.updatedAt.toDateTimeString(),
            lastSyncTimestamp = entity.lastSyncTimestamp,
            action = if (entity.isDeleted) "DELETE" else null
        )
    }

    // MÉTODO: Converter entidade local para DTO
    private suspend fun convertToPacienteEspecialidadeDTO(relation: PacienteEspecialidade): PacienteEspecialidadeDTO {
        val pacienteLocalId = relation.pacienteLocalId
        val especialidadeLocalId = relation.especialidadeLocalId

        // Buscar serverId do paciente
        val paciente = database.pacienteDao().getPacienteByLocalId(pacienteLocalId)
        val pacienteServerId = paciente?.serverId

        // Buscar serverId da especialidade
        val especialidade = database.especialidadeDao().getEspecialidadeByLocalId(especialidadeLocalId)
        val especialidadeServerId = especialidade?.serverId

        return PacienteEspecialidadeDTO(
            pacienteServerId = pacienteServerId,
            especialidadeServerId = especialidadeServerId,
            dataAtendimento = safeFormatDate(relation.dataAtendimento),
            createdAt = System.currentTimeMillis().toDateTimeString(),
            updatedAt = System.currentTimeMillis().toDateTimeString(),
            lastSyncTimestamp = System.currentTimeMillis(),
            action = null
        )
    }

    private fun safeFormatDate(timestamp: Any?): String? {
        return when (timestamp) {
            is Long -> dateFormat.format(Date(timestamp))
            is String -> timestamp // Já está formatado
            null -> null
            else -> {
                println("Tipo inesperado para data: ${timestamp::class.java}")
                null
            }
        }
    }

    // MÉTODO: Sincronizar todas as especialidades
    private suspend fun syncAllEspecialidades() {
        try {
            val response = apiService.getAllEspecialidades()
            if (response.isSuccessful) {
                response.body()?.let { apiResponse ->
                    if (apiResponse.success && apiResponse.data != null) {
                        updateLocalEspecialidades(apiResponse.data)
                    }
                }
            }
        } catch (e: Exception) {
            println("Erro ao sincronizar especialidades: ${e.message}")
        }
    }

    // MÉTODO: Atualizar especialidades locais
    private suspend fun updateLocalEspecialidades(especialidadesDto: List<EspecialidadeDto>) {
        val especialidadesLocal = especialidadesDto.map { dto ->
            Especialidade(
                localId = UUID.randomUUID().toString(), // Gerar novo localId
                serverId = dto.serverId,
                nome = dto.nome
            )
        }

        val especialidadesEntity = especialidadesLocal.toEntityList(
            deviceId = getDeviceId(),
            syncStatus = SyncStatus.SYNCED
        )

        database.especialidadeDao().deleteAllEspecialidades()
        database.especialidadeDao().insertEspecialidades(especialidadesEntity)
    }

    // MÉTODO: Sincronização completa incluindo especialidades
    suspend fun syncDataWithEspecialidades(): SyncResult {
        if (!isNetworkAvailable()) {
            return SyncResult.NO_NETWORK
        }

        try {
            // 1. Sincronizar especialidades primeiro
            syncAllEspecialidades()

            // 2. Sincronizar pacientes
            val pacientesResult = syncData()

            // 3. Sincronizar relações paciente-especialidade pendentes
            syncPacienteEspecialidadesPendentes()

            return pacientesResult

        } catch (e: Exception) {
            return SyncResult.ERROR(error = e.toSyncError())
        }
    }

    // MÉTODO: Buscar especialidades de um paciente específico
    suspend fun getPacienteEspecialidadesLocal(pacienteLocalId: String): List<EspecialidadeDto> {
        // Usar o método correto do DAO
        val especialidades = database.pacienteEspecialidadeDao().getEspecialidadesByPacienteId(pacienteLocalId)

        return especialidades.map { especialidade ->
            EspecialidadeDto(
                serverId = especialidade.serverId,
                nome = especialidade.nome,
                fichas = especialidade.fichas,
                atendimentosRestantesHoje = especialidade.atendimentosRestantesHoje,
                atendimentosTotaisHoje = especialidade.atendimentosTotaisHoje,
                createdAt = especialidade.createdAt.toDateTimeString(),
                updatedAt = System.currentTimeMillis().toDateTimeString(),
                isDeleted = especialidade.isDeleted
            )
        }
    }

    // MÉTODO: Adicionar especialidade a um paciente
    suspend fun addEspecialidadeToPaciente(pacienteLocalId: String, especialidadeLocalId: String) {
        val entity = PacienteEspecialidadeEntity(
            pacienteLocalId = pacienteLocalId,
            especialidadeLocalId = especialidadeLocalId,
            pacienteServerId = null,
            especialidadeServerId = null,
            syncStatus = SyncStatus.PENDING_UPLOAD,
            deviceId = getDeviceId(),
            dataAtendimento = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            lastSyncTimestamp = System.currentTimeMillis(),
            version = 1,
            isDeleted = false,
            syncAttempts = 0,
            lastSyncAttempt = 0L,
            syncError = null
        )

        // Usar o método correto do DAO
        database.pacienteEspecialidadeDao().insert(entity)
    }

    // MÉTODO: Remover especialidade de um paciente
    suspend fun removeEspecialidadeFromPaciente(pacienteLocalId: String, especialidadeLocalId: String) {
        // Usar o método correto do DAO que você já implementou
        database.pacienteEspecialidadeDao().deleteByPacienteAndEspecialidade(
            pacienteLocalId,
            especialidadeLocalId
        )
    }

    //  Para sincronização de especialidades do servidor
    private suspend fun syncPacienteEspecialidades(pacienteLocalId: String, serverId: Long) {
        try {
            val response = apiService.getPacienteEspecialidades(serverId)

            if (response.isSuccessful) {
                response.body()?.let { apiResponse ->
                    if (apiResponse.success && apiResponse.data != null) {
                        // Usar o método específico para paciente
                        val entities = apiResponse.data.mapNotNull { dto ->
                            try {
                                pacienteEspecialidadeMapper.dtoToEntityForPaciente(
                                    dto = dto,
                                    pacienteLocalId = pacienteLocalId,
                                    deviceId = getDeviceId(),
                                    syncStatus = SyncStatus.SYNCED
                                )
                            } catch (e: Exception) {
                                println("Erro ao converter especialidade: ${e.message}")
                                null
                            }
                        }

                        // Limpar especialidades existentes
                        database.pacienteEspecialidadeDao().deleteByPacienteId(pacienteLocalId)

                        // Inserir novas especialidades
                        if (entities.isNotEmpty()) {
                            database.pacienteEspecialidadeDao().insertAll(entities)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("Erro ao sincronizar especialidades do paciente: ${e.message}")
        }
    }

    private suspend fun markItemsAsSynced(pendingItems: List<SyncQueue>): Int {
        var syncedCount = 0
        for (item in pendingItems) {
            try {
                database.syncQueueDao().deleteItem(item)
                syncedCount++
            } catch (e: Exception) {
                // Marcar como erro se não conseguir deletar
                database.syncQueueDao().markAsError(item.id, e.message ?: "Erro desconhecido")
            }
        }
        return syncedCount
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }

    private fun getDeviceId(): String {
        val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        var deviceId = sharedPrefs.getString("device_id", null)

        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString()
            sharedPrefs.edit().putString("device_id", deviceId).apply()
        }

        return deviceId
    }

    private fun getLastSyncTimestamp(): Long {
        val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return sharedPrefs.getLong("last_sync_timestamp", 0)
    }

    private fun saveLastSyncTimestamp(timestamp: Long) {
        val sharedPrefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        sharedPrefs.edit().putLong("last_sync_timestamp", timestamp).apply()
    }

    // Sincronização automática em background
    suspend fun startPeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "sync_data",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}