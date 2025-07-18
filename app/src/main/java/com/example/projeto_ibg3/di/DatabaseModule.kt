package com.example.projeto_ibg3.di

import android.content.Context
import androidx.room.Room
import com.example.projeto_ibg3.data.local.database.dao.EspecialidadeDao
import com.example.projeto_ibg3.data.local.database.dao.PacienteDao
import com.example.projeto_ibg3.data.local.database.dao.PacienteEspecialidadeDao
import com.example.projeto_ibg3.data.local.database.dao.SyncMetadataDao
import com.example.projeto_ibg3.data.local.database.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration(true)
            .build()
    }

    @Provides
    fun providePacienteDao(database: AppDatabase): PacienteDao {
        return database.pacienteDao()
    }

    @Provides
    fun provideEspecialidadeDao(database: AppDatabase): EspecialidadeDao {
        return database.especialidadeDao()
    }

    @Provides
    fun providePacienteEspecialidadeDao(database: AppDatabase): PacienteEspecialidadeDao {
        return database.pacienteEspecialidadeDao()
    }

    @Provides
    fun provideSyncMetadataDao(database: AppDatabase): SyncMetadataDao {
        return database.syncMetadataDao()
    }
}