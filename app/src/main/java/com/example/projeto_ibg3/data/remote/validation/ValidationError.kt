package com.example.projeto_ibg3.data.remote.validation

//Erro de validação
data class ValidationError(
    val field: String,
    val message: String,
    val code: ValidationErrorCode
)
