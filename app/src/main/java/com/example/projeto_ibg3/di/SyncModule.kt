package com.example.projeto_ibg3.di

import android.content.Context
import com.example.projeto_ibg3.data.local.database.AppDatabase
import com.example.projeto_ibg3.data.mappers.PacienteEspecialidadeMapper
import com.example.projeto_ibg3.data.remote.api.ApiService
import com.example.projeto_ibg3.sync.service.SyncService
import com.google.android.datatransport.runtime.dagger.Provides
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SyncModule {

    @Provides
    @Singleton
    fun provideSyncService(
        database: AppDatabase,
        apiService: ApiService,
        @ApplicationContext context: Context,
        pacienteEspecialidadeMapper: PacienteEspecialidadeMapper
    ): SyncService {
        return SyncService(database, apiService, context, pacienteEspecialidadeMapper)
    }
}