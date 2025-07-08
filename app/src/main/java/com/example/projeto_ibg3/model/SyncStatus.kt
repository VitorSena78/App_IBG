package com.example.projeto_ibg3.model

// Enum para controlar status de sincronização
enum class SyncStatus {
    SYNCED,          // Sincronizado
    PENDING_UPLOAD,  // Pendente para enviar ao servidor
    PENDING_DELETE,   // Pendente para deletar no servidor
    CONFLICT,          // Conflito entre dados locais e remotos
    PENDING_DELETION
}