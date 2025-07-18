package com.example.projeto_ibg3.sync.model

enum class SyncState {
    IDLE,
    SYNCING,
    UPLOADING,
    DOWNLOADING,
    RESOLVING_CONFLICTS,
    COMPLETED,
    ERROR
}