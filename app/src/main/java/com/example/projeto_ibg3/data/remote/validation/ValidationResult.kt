package com.example.projeto_ibg3.data.remote.validation

// Resultado de validação
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<ValidationError> = emptyList(),
    val unavailableEspecialidades: List<String>,
    val warningEspecialidades: List<String>
)
