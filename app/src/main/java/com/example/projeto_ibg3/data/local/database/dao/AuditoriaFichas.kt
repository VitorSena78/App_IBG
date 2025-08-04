package com.example.projeto_ibg3.data.local.database.dao

import androidx.room.ColumnInfo

// Classe para auditoria
data class AuditoriaFichas(
    val nome: String,
    val fichas: Int,
    @ColumnInfo(name = "relacionamentos_ativos")
    val relacionamentosAtivos: Int
)
