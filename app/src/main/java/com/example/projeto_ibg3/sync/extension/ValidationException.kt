package com.example.projeto_ibg3.sync.extension

/**
 * Exceção lançada quando há erros de validação nos dados
 * Esta exceção não deve ser retriada automaticamente
 */
class ValidationException(
    message: String,
    cause: Throwable? = null,
    val field: String? = null,
    val validationErrors: List<String> = emptyList()
) : Exception(message, cause) {

    constructor(field: String, message: String) : this(message, null, field)

    constructor(validationErrors: List<String>) : this(
        "Validation failed: ${validationErrors.joinToString(", ")}",
        null,
        null,
        validationErrors
    )
}