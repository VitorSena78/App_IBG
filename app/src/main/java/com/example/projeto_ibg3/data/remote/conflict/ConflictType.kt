package com.example.projeto_ibg3.data.remote.conflict

enum class ConflictType {
    MODIFIED_BOTH,    // Modificado tanto localmente quanto no servidor
    DELETED_LOCALLY,  // Deletado localmente mas modificado no servidor
    DELETED_REMOTELY,  // Deletado no servidor mas modificado localmente
    DATA_CONFLICT,      // Dados diferentes
    VERSION_CONFLICT,   // Versões diferentes
    TIMESTAMP_CONFLICT  // Timestamps diferente
}