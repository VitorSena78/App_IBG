package com.example.projeto_ibg3.sync.core

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConflictResolver @Inject constructor() {

    fun <T> resolveConflict(local: T, remote: T): T {
        // Implementar lógica de resolução de conflitos
        // Por exemplo, sempre usar o remoto ou comparar timestamps
        return remote
    }
}