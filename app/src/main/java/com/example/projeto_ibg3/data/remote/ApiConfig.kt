package com.example.projeto_ibg3.data.remote

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

// Configuração do Retrofit
object ApiConfig {
    private const val BASE_URL = "http://SEU_IP_DO_SERVIDOR:8080/api/" // Altere para o IP do seu servidor

    fun getApiService(): PacienteApiService {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(PacienteApiService::class.java)
    }
}