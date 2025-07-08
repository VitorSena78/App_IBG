package com.example.projeto_ibg3.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.example.projeto_ibg3.data.converters.SyncStatusConverter
import com.example.projeto_ibg3.data.dao.EspecialidadeDao
import com.example.projeto_ibg3.data.dao.PacienteDao
import com.example.projeto_ibg3.data.dao.PacienteEspecialidadeDao
import com.example.projeto_ibg3.data.dao.SyncMetadataDao
import com.example.projeto_ibg3.data.entity.EspecialidadeEntity
import com.example.projeto_ibg3.data.entity.PacienteEntity
import com.example.projeto_ibg3.data.entity.PacienteEspecialidadeEntity
import com.example.projeto_ibg3.data.entity.SyncMetadataEntity

@Database(
    entities = [
        PacienteEntity::class,
        EspecialidadeEntity::class,
        PacienteEspecialidadeEntity::class,
        SyncMetadataEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(SyncStatusConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun pacienteDao(): PacienteDao
    abstract fun especialidadeDao(): EspecialidadeDao
    abstract fun pacienteEspecialidadeDao(): PacienteEspecialidadeDao
    abstract fun syncMetadataDao(): SyncMetadataDao

    companion object {
        const val DATABASE_NAME = "medical_database"
    }
}