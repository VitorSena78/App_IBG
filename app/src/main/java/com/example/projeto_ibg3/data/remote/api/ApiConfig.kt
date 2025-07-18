package com.example.projeto_ibg3.data.remote.api

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

// Configuração do Retrofit
object ApiConfig {
    // FORMATO URL:
    // Para servidor local: "http://192.168.1.100:8080/api/"
    // Para servidor remoto: "https://meudominio.com/api/"
    // Para emulador Android: "http://10.0.2.2:8080/api/"

    private const val BASE_URL = "http://192.168.1.100:8080/api/" // Substitua pelo seu IP

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    fun getApiService(): PacienteApiService {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(PacienteApiService::class.java)
    }
}