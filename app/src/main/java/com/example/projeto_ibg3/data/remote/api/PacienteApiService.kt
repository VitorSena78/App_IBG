package com.example.projeto_ibg3.data.remote.api

import com.example.projeto_ibg3.data.remote.conflict.ConflictInfo
import com.example.projeto_ibg3.data.remote.dto.PacienteDto
import com.example.projeto_ibg3.sync.model.SyncInfo
import retrofit2.Response
import retrofit2.http.*
import com.example.projeto_ibg3.data.remote.dto.EspecialidadeDto
import com.example.projeto_ibg3.data.remote.dto.PacienteEspecialidadeDTO

interface PacienteApiService {

    // ========== HEALTH CHECK ==========
    @GET("health")
    suspend fun healthCheck(): Response<ApiResponse<String>>

    // ========== PACIENTES - OPERAÇÕES BÁSICAS ==========

    @GET("pacientes")
    suspend fun getAllPacientes(): Response<ApiResponse<List<PacienteDto>>>

    @GET("pacientes/updated")
    suspend fun getUpdatedPacientes(@Query("since") timestamp: Long): Response<ApiResponse<List<PacienteDto>>>

    @GET("pacientes/{serverId}")
    suspend fun getPacienteById(@Path("serverId") serverId: Long): Response<ApiResponse<PacienteDto>>

    @POST("pacientes")
    suspend fun createPaciente(@Body paciente: PacienteDto): Response<ApiResponse<PacienteDto>>

    @PUT("pacientes/{serverId}")
    suspend fun updatePaciente(
        @Path("serverId") serverId: Long,
        @Body paciente: PacienteDto
    ): Response<ApiResponse<PacienteDto>>

    @DELETE("pacientes/{serverId}")
    suspend fun deletePaciente(@Path("serverId") serverId: Long): Response<ApiResponse<Unit>>

    // ========== OPERAÇÕES EM LOTE (BATCH) ==========

    @POST("pacientes/batch")
    suspend fun createPacientesBatch(@Body pacientes: List<PacienteDto>): Response<ApiResponse<List<PacienteDto>>>

    @PUT("pacientes/batch")
    suspend fun updatePacientesBatch(@Body pacientes: List<PacienteDto>): Response<ApiResponse<List<PacienteDto>>>

    @HTTP(method = "DELETE", path = "pacientes/batch", hasBody = true)
    suspend fun deletePacientesBatch(@Body pacienteIds: List<Long>): Response<ApiResponse<Unit>>

    // ========== BUSCA E PESQUISA ==========

    @GET("pacientes/search")
    suspend fun searchPacientes(@Query("nome") nome: String): Response<ApiResponse<List<PacienteDto>>>

    @GET("pacientes/search/cpf")
    suspend fun searchPacientesByCpf(@Query("cpf") cpf: String): Response<ApiResponse<List<PacienteDto>>>

    @GET("pacientes/search/sus")
    suspend fun searchPacientesBySus(@Query("sus") sus: String): Response<ApiResponse<List<PacienteDto>>>

    // ========== SINCRONIZAÇÃO AVANÇADA ==========

    @POST("pacientes/check-conflicts")
    suspend fun checkConflicts(@Body localPacientes: List<PacienteDto>): Response<ApiResponse<List<ConflictInfo>>>

    @GET("sync-info")
    suspend fun getSyncInfo(): Response<ApiResponse<SyncInfo>>

    // ========== ESPECIALIDADES ==========

    @GET("especialidades")
    suspend fun getAllEspecialidades(): Response<ApiResponse<List<EspecialidadeDto>>>

    @GET("especialidades/{id}")
    suspend fun getEspecialidadeById(@Path("id") id: Long): Response<ApiResponse<EspecialidadeDto>>

    @POST("especialidades")
    suspend fun createEspecialidade(@Body especialidade: EspecialidadeDto): Response<ApiResponse<EspecialidadeDto>>

    @PUT("especialidades/{id}")
    suspend fun updateEspecialidade(@Path("id") id: Long, @Body especialidade: EspecialidadeDto): Response<ApiResponse<EspecialidadeDto>>

    @DELETE("especialidades/{id}")
    suspend fun deleteEspecialidade(@Path("id") id: Long): Response<ApiResponse<Unit>>

    // ========== RELACIONAMENTO PACIENTE-ESPECIALIDADE ==========

    @GET("pacientes/{pacienteId}/especialidades")
    suspend fun getPacienteEspecialidades(@Path("pacienteId") pacienteId: Long): Response<ApiResponse<List<PacienteEspecialidadeDTO>>>

    @POST("pacientes/{pacienteId}/especialidades")
    suspend fun addEspecialidadeToPaciente(
        @Path("pacienteId") pacienteId: Long,
        @Body especialidadeIds: List<Long>
    ): Response<ApiResponse<List<PacienteEspecialidadeDTO>>>

    @DELETE("pacientes/{pacienteId}/especialidades/{especialidadeId}")
    suspend fun removeEspecialidadeFromPaciente(
        @Path("pacienteId") pacienteId: Long,
        @Path("especialidadeId") especialidadeId: Long
    ): Response<ApiResponse<Unit>>

    @POST("pacientes/especialidades/sync")
    suspend fun syncPacienteEspecialidades(@Body relations: List<PacienteEspecialidadeDTO>): Response<ApiResponse<List<PacienteEspecialidadeDTO>>>

    // ------------------------------
    // RELACIONAMENTO PACIENTE/ESPECIALIDADE
    // ------------------------------

    @POST("pacientes/{pacienteLocalId}/especialidades/{especialidadeLocalId}")
    suspend fun vincularEspecialidade(
        @Path("pacienteLocalId") pacienteId: Long,
        @Path("especialidadeLocalId") especialidadeId: Long,
        @Body dataAtendimento: Map<String, String>? = null
    ): Response<Unit>

    @DELETE("pacientes/{pacienteLocalId}/especialidades/{especialidadeLocalId}")
    suspend fun desvincularEspecialidade(
        @Path("pacienteLocalId") pacienteId: Long,
        @Path("especialidadeLocalId") especialidadeId: Long
    ): Response<Unit>

    // Sincronização incremental
    @GET("pacientes/especialidades/updated")
    suspend fun getUpdatedPacienteEspecialidades(@Query("since") timestamp: Long): Response<List<PacienteEspecialidadeDTO>>
}