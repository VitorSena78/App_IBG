package com.example.projeto_ibg3.data.local.database.entities

import androidx.room.ColumnInfo

data class EspecialidadeFichas(
    @ColumnInfo(name = "local_id") val localId: String,
    @ColumnInfo(name = "fichas") val fichas: Int
)
