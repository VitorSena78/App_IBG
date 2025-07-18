package com.example.projeto_ibg3.domain.model

data class Especialidade(
    val localId: String, // Adicionar localId
    val serverId: Long? = null, // Renomear de 'id' para 'serverId' para consistência
    val nome: String
) {

    // Propriedade para compatibilidade com código legado
    val id: Long
        get() = serverId ?: 0L
}
