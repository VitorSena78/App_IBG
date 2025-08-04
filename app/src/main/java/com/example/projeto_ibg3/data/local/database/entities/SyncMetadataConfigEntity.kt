package com.example.projeto_ibg3.data.local.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_metadata_config")
data class SyncMetadataConfigEntity(
    @PrimaryKey
    val key: String,
    val value: String
)
