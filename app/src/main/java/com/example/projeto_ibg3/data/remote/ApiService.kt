package com.example.projeto_ibg3.data.remote

import com.example.projeto_ibg3.data.remote.dto.EspecialidadeDto
import com.example.projeto_ibg3.data.remote.dto.PacienteDto
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    // PACIENTES
    @GET("pacientes")
    suspend fun getAllPacientes(): Response<List<PacienteDto>>

    @GET("pacientes")
    suspend fun getUpdatedPacientes(@Query("updated_since") timestamp: Long): Response<List<PacienteDto>>

    @GET("pacientes/{id}")
    suspend fun getPacienteById(@Path("id") id: Long): Response<PacienteDto>

    @POST("pacientes")
    suspend fun createPaciente(@Body paciente: PacienteDto): Response<PacienteDto>

    @PUT("pacientes/{id}")
    suspend fun updatePaciente(@Path("id") id: Long, @Body paciente: PacienteDto): Response<PacienteDto>

    @DELETE("pacientes/{id}")
    suspend fun deletePaciente(@Path("id") id: Long): Response<Unit>

    // ESPECIALIDADES
    @GET("especialidades")
    suspend fun getAllEspecialidades(): Response<List<EspecialidadeDto>>

    @POST("especialidades")
    suspend fun createEspecialidade(@Body especialidade: EspecialidadeDto): Response<EspecialidadeDto>

    @PUT("especialidades/{id}")
    suspend fun updateEspecialidade(@Path("id") id: Long, @Body especialidade: EspecialidadeDto): Response<EspecialidadeDto>

    @DELETE("especialidades/{id}")
    suspend fun deleteEspecialidade(@Path("id") id: Long): Response<Unit>

    // RELACIONAMENTOS
    @POST("pacientes/{pacienteId}/especialidades/{especialidadeId}")
    suspend fun vincularEspecialidade(
        @Path("pacienteId") pacienteId: Long,
        @Path("especialidadeId") especialidadeId: Long,
        @Body dataAtendimento: Map<String, String>? = null
    ): Response<Unit>

    @DELETE("pacientes/{pacienteId}/especialidades/{especialidadeId}")
    suspend fun desvincularEspecialidade(
        @Path("pacienteId") pacienteId: Long,
        @Path("especialidadeId") especialidadeId: Long
    ): Response<Unit>
}