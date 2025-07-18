package com.example.projeto_ibg3.data.remote.conflict

enum class ConflictResolution {
    KEEP_LOCAL,
    KEEP_SERVER,
    MERGE_AUTOMATIC,
    MERGE_MANUAL,
    MANUAL
}