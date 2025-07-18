package com.example.projeto_ibg3.presentation.common.utils

import java.util.UUID

// Utilitários para trabalhar com Paciente
object PacienteUtils {

    /**
     * Valida CPF
     */
    fun isValidCpf(cpf: String): Boolean {
        val numbers = cpf.filter { it.isDigit() }
        if (numbers.length != 11) return false

        // Verifica se todos os dígitos são iguais
        if (numbers.all { it == numbers[0] }) return false

        // Validação dos dígitos verificadores
        val digits = numbers.map { it.digitToInt() }

        // Primeiro dígito
        val sum1 = (0..8).sumOf { digits[it] * (10 - it) }
        val digit1 = ((sum1 * 10) % 11).let { if (it == 10) 0 else it }

        // Segundo dígito
        val sum2 = (0..9).sumOf { digits[it] * (11 - it) }
        val digit2 = ((sum2 * 10) % 11).let { if (it == 10) 0 else it }

        return digits[9] == digit1 && digits[10] == digit2
    }

    /**
     * Formata CPF
     */
    fun formatCpf(cpf: String): String {
        val numbers = cpf.filter { it.isDigit() }
        return if (numbers.length == 11) {
            "${numbers.substring(0, 3)}.${numbers.substring(3, 6)}.${numbers.substring(6, 9)}-${numbers.substring(9, 11)}"
        } else {
            cpf
        }
    }

    /**
     * Calcula idade a partir da data de nascimento
     */
    fun calculateAge(dataNascimento: Long): Int {
        val now = System.currentTimeMillis()
        val ageInMillis = now - dataNascimento
        return (ageInMillis / (365.25 * 24 * 60 * 60 * 1000)).toInt()
    }

    /**
     * Calcula IMC
     */
    fun calculateImc(peso: Float?, altura: Float?): Float? {
        return if (peso != null && altura != null && altura > 0) {
            peso / (altura * altura)
        } else {
            null
        }
    }

    /**
     * Gera ID único para dispositivo
     */
    fun generateDeviceId(): String {
        return UUID.randomUUID().toString()
    }
}