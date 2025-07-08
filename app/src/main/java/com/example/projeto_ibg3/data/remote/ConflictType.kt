package com.example.projeto_ibg3.data.remote

enum class ConflictType {
    MODIFIED_BOTH,    // Modificado tanto localmente quanto no servidor
    DELETED_LOCALLY,  // Deletado localmente mas modificado no servidor
    DELETED_REMOTELY  // Deletado no servidor mas modificado localmente
}