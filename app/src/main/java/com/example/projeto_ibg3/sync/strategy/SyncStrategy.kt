package com.example.projeto_ibg3.sync.strategy

import com.example.projeto_ibg3.sync.model.SyncResult

/**
 * Interface base para estratégias de sincronização
 */
interface SyncStrategy {
    /**
     * Sincronização completa da entidade
     */
    suspend fun sync(): SyncResult

    /**
     * Sincronização rápida (apenas itens modificados)
     */
    suspend fun syncQuick(): SyncResult

    /**
     * Upload de mudanças locais
     */
    suspend fun uploadLocalChanges(): SyncResult

    /**
     * Download de mudanças do servidor
     */
    suspend fun downloadServerChanges(): SyncResult

    /**
     * Resolver conflitos
     */
    suspend fun resolveConflicts(): SyncResult

    /**
     * Verificar se há mudanças pendentes
     */
    suspend fun hasPendingChanges(): Boolean

    /**
     * Obter estatísticas da entidade
     */
    suspend fun getStats(): Map<String, Int>
}