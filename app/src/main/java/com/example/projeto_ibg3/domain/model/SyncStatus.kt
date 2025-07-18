package com.example.projeto_ibg3.domain.model

// Enum para controlar status de sincronização
enum class SyncStatus {
    PENDING_UPLOAD,   // Pendente para enviar ao servidor
    PENDING_DELETE,   // Pendente para deletar no servidor
    SYNCED,           // Sincronizado com sucesso
    UPLOAD_FAILED,    // Falha no upload (para retry)
    DELETE_FAILED,    // Falha na deleção (para retry)
    CONFLICT,         // Conflito entre dados locais e remotos
    SYNCING;          // Em processo de sincronização

    // Métodos úteis para lógica de sync
    fun isPending(): Boolean = this in listOf(PENDING_UPLOAD, PENDING_DELETE)
    fun isFailed(): Boolean = this in listOf(UPLOAD_FAILED, DELETE_FAILED)
    fun needsSync(): Boolean = isPending() || isFailed() || this == CONFLICT
    fun isInProgress(): Boolean = this == SYNCING
}