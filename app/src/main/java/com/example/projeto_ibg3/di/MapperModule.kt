package com.example.projeto_ibg3.di

import com.example.projeto_ibg3.data.local.database.dao.EspecialidadeDao
import com.example.projeto_ibg3.data.local.database.dao.PacienteDao
import com.example.projeto_ibg3.data.mappers.PacienteEspecialidadeMapper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MapperModule {

    @Provides
    @Singleton
    fun providePacienteEspecialidadeMapper(
        pacienteDao: PacienteDao,
        especialidadeDao: EspecialidadeDao
    ): PacienteEspecialidadeMapper {
        return PacienteEspecialidadeMapper(pacienteDao, especialidadeDao)
    }
}