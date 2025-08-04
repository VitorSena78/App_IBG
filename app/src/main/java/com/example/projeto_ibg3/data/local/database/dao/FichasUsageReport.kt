package com.example.projeto_ibg3.data.local.database.dao

data class FichasUsageReport(
    val totalEspecialidades: Int,
    val especialidadesDisponiveis: Int,
    val especialidadesEsgotadas: Int,
    val totalFichas: Int,
    val totalAtendimentos: Int,
    val especialidadesComPoucasFichas: Int
) {
    fun getResumo(): String = buildString {
        appendLine("RELATÓRIO DE USO DE FICHAS")
        appendLine("================================")
        appendLine("Total de especialidades: $totalEspecialidades")
        appendLine("Disponíveis: $especialidadesDisponiveis")
        appendLine("Esgotadas: $especialidadesEsgotadas")
        appendLine("Com poucas fichas (≤5): $especialidadesComPoucasFichas")
        appendLine("Total de fichas restantes: $totalFichas")
        appendLine("Total de atendimentos ativos: $totalAtendimentos")

        val percentualEsgotadas = if (totalEspecialidades > 0) {
            (especialidadesEsgotadas * 100.0 / totalEspecialidades).toInt()
        } else 0
        appendLine("Percentual esgotado: $percentualEsgotadas%")
    }
}
