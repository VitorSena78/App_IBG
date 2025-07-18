package com.example.projeto_ibg3.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.projeto_ibg3.data.local.database.converters.SyncStatusConverter
import com.example.projeto_ibg3.data.local.database.dao.EspecialidadeDao
import com.example.projeto_ibg3.data.local.database.dao.PacienteDao
import com.example.projeto_ibg3.data.local.database.dao.PacienteEspecialidadeDao
import com.example.projeto_ibg3.data.local.database.dao.SyncMetadataDao
import com.example.projeto_ibg3.data.local.database.dao.SyncQueueDao
import com.example.projeto_ibg3.data.local.database.entities.EspecialidadeEntity
import com.example.projeto_ibg3.data.local.database.entities.PacienteEntity
import com.example.projeto_ibg3.data.local.database.entities.PacienteEspecialidadeEntity
import com.example.projeto_ibg3.data.local.database.entities.SyncQueue
import com.example.projeto_ibg3.data.local.database.entities.SyncMetadataConfigEntity
import com.example.projeto_ibg3.data.local.database.entities.SyncMetadataEntity

@Database(
    entities = [
        PacienteEntity::class,
        EspecialidadeEntity::class,
        PacienteEspecialidadeEntity::class,
        SyncMetadataEntity::class,
        SyncMetadataConfigEntity::class,
        SyncQueue::class // Adicione esta linha
    ],
    version = 6,
    exportSchema = false
)
@TypeConverters(SyncStatusConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun pacienteDao(): PacienteDao
    abstract fun especialidadeDao(): EspecialidadeDao
    abstract fun pacienteEspecialidadeDao(): PacienteEspecialidadeDao
    abstract fun syncMetadataDao(): SyncMetadataDao
    abstract fun syncQueueDao(): SyncQueueDao // Adicione esta linha

    companion object {
        const val DATABASE_NAME = "medical_database"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}