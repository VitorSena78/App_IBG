package com.example.projeto_ibg3.presentation.ui.lista

//Estatísticas da lista
data class ListStats(
    val totalPacientes: Int = 0,
    val pacientesComConflito: Int = 0,
    val pacientesPendentes: Int = 0,
    val pacientesSincronizados: Int = 0
) {
    val syncPercentage: Float
        get() = if (totalPacientes > 0) {
            (pacientesSincronizados.toFloat() / totalPacientes) * 100
        } else 0f

    val hasIssues: Boolean
        get() = pacientesComConflito > 0 || pacientesPendentes > 0
}
