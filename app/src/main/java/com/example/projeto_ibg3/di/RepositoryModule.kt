package com.example.projeto_ibg3.di

import com.example.projeto_ibg3.data.repository.impl.EspecialidadeRepositoryImpl
import com.example.projeto_ibg3.domain.repository.PacienteRepository
import com.example.projeto_ibg3.data.repository.impl.PacienteRepositoryImpl
import com.example.projeto_ibg3.data.repository.impl.SyncRepositoryImpl
import com.example.projeto_ibg3.domain.repository.EspecialidadeRepository
import com.example.projeto_ibg3.domain.repository.SyncRepository

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindEspecialidadeRepository(
        impl: EspecialidadeRepositoryImpl
    ): EspecialidadeRepository

    @Binds
    abstract fun bindPacienteRepository(
        impl: PacienteRepositoryImpl
    ): PacienteRepository

    @Binds
    abstract fun bindSyncRepository(
        impl: SyncRepositoryImpl
    ): SyncRepository

}