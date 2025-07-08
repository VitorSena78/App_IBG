package com.example.projeto_ibg3.data.remote

import com.example.projeto_ibg3.model.Paciente
import retrofit2.Response
import retrofit2.http.*

// Interface da API
interface PacienteApiService {

    // ENDPOINTS ESSENCIAIS PARA SINCRONIZAÇÃO

    /**
     * Busca pacientes modificados após um timestamp específico
     * Crucial para sincronização incremental (evita baixar todos os dados sempre)
     */
    @GET("pacientes")
    suspend fun getUpdatedPacientes(@Query("since") timestamp: Long): Response<List<Paciente>>

    //Busca todos os pacientes (usar apenas na primeira sincronização)
    @GET("pacientes")
    suspend fun getAllPacientes(): Response<List<Paciente>>

    //Busca paciente por ID do servidor
    @GET("pacientes/{id}")
    suspend fun getPacienteById(@Path("id") id: Long): Response<Paciente>

    /**
     * Cria novo paciente no servidor
     * Retorna o paciente com ID do servidor preenchido
     */
    @POST("pacientes")
    suspend fun createPaciente(@Body paciente: Paciente): Response<Paciente>

    //Atualiza paciente existente no servidor
    @PUT("pacientes/{id}")
    suspend fun updatePaciente(@Path("id") id: Long, @Body paciente: Paciente): Response<Paciente>

    //Remove paciente do servidor
    @DELETE("pacientes/{id}")
    suspend fun deletePaciente(@Path("id") id: Long): Response<Unit>

    // ENDPOINTS PARA SINCRONIZAÇÃO EM LOTE (OPCIONAL MAS RECOMENDADO)

    //Envia múltiplos pacientes de uma vez (mais eficiente)
    @POST("pacientes/batch")
    suspend fun createPacientesBatch(@Body pacientes: List<Paciente>): Response<List<Paciente>>

    //Atualiza múltiplos pacientes de uma vez
    @PUT("pacientes/batch")
    suspend fun updatePacientesBatch(@Body pacientes: List<Paciente>): Response<List<Paciente>>

    //Remove múltiplos pacientes de uma vez
    @DELETE("pacientes/batch")
    suspend fun deletePacientesBatch(@Body pacienteIds: List<Long>): Response<Unit>

    // ENDPOINTS PARA FUNCIONALIDADES ESPECÍFICAS (MANTER OS EXISTENTES)

    // Busca pacientes por nome (funcionalidade de pesquisa)
    @GET("pacientes/search")
    suspend fun searchPacientes(@Query("nome") nome: String): Response<List<Paciente>>

    //Busca pacientes por CPF
    @GET("pacientes/search")
    suspend fun searchPacientesByCpf(@Query("cpf") cpf: String): Response<List<Paciente>>

    // ENDPOINTS PARA METADADOS DE SINCRONIZAÇÃO

    //Verifica se há conflitos antes de sincronizar
    @POST("pacientes/check-conflicts")
    suspend fun checkConflicts(@Body localPacientes: List<Paciente>): Response<List<ConflictInfo>>

    //Obtém informações sobre a última modificação do servidor
    @GET("pacientes/sync-info")
    suspend fun getSyncInfo(): Response<SyncInfo>
}