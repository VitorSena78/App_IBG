package com.example.projeto_ibg3.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
data class Especialidade(
    val localId: String = UUID.randomUUID().toString(), // Adicionar localId
    val serverId: Long? = null, // Renomear de 'id' para 'serverId' para consistência
    val nome: String,
    val fichas: Int = 0,
    val atendimentosRestantesHoje: Int? = null,
    val atendimentosTotaisHoje: Int? = null,
    val isDeleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
): Parcelable {
    // Métodos de conveniência
    fun isAvailable(): Boolean = fichas > 0 && !isDeleted
    fun isEsgotada(): Boolean = fichas <= 0

    // Status textual para UI
    fun getStatusText(): String = when {
        isDeleted -> "Inativa"
        fichas <= 0 -> "Esgotada"
        fichas <= 5 -> "Poucas fichas ($fichas)"
        else -> "$fichas fichas disponíveis"
    }

    // Cor do status para UI
    fun getStatusColor(): StatusColor = when {
        isDeleted -> StatusColor.INACTIVE
        fichas <= 0 -> StatusColor.ESGOTADA
        fichas <= 5 -> StatusColor.WARNING
        else -> StatusColor.AVAILABLE
    }
}
