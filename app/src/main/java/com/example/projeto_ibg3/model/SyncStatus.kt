package com.example.projeto_ibg3.model

// Enum para controlar status de sincronização
enum class SyncStatus {
    SYNCED,           // Sincronizado com sucesso
    PENDING_UPLOAD,   // Pendente para enviar ao servidor
    PENDING_DELETE,   // Pendente para deletar no servidor
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