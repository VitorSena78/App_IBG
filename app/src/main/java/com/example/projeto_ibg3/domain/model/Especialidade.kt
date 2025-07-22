package com.example.projeto_ibg3.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Especialidade(
    val localId: String, // Adicionar localId
    val serverId: Long? = null, // Renomear de 'id' para 'serverId' para consistência
    val nome: String
): Parcelable
