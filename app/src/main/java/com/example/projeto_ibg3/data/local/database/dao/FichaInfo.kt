package com.example.projeto_ibg3.data.local.database.dao

import androidx.room.ColumnInfo

data class FichaInfo(
    @ColumnInfo(name = "local_id") val localId: String,
    val fichas: Int
)
