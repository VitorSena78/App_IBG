package com.example.projeto_ibg3.domain.model

// Data class para estatísticas
data class EspecialidadeStats(
    val total: Int,
    val disponiveis: Int,
    val esgotadas: Int,
    val totalFichas: Int,
    val comPoucasFichas: Int
) {
    fun getResumo(): String = buildString {
        appendLine("📊 Resumo das Especialidades:")
        appendLine("• Total: $total")
        appendLine("• Disponíveis: $disponiveis")
        appendLine("• Esgotadas: $esgotadas")
        appendLine("• Total de fichas: $totalFichas")
        if (comPoucasFichas > 0) {
            appendLine("• ⚠️ Com poucas fichas (≤5): $comPoucasFichas")
        }
    }
}

