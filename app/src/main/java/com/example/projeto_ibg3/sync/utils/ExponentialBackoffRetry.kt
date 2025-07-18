package com.example.projeto_ibg3.sync.utils


import com.example.projeto_ibg3.sync.extension.ExceptionHandler
import kotlinx.coroutines.delay
import kotlin.math.pow
import kotlin.random.Random

class ExponentialBackoffRetry(
    private val maxRetries: Int = 5,
    private val baseDelayMs: Long = 1000,
    private val maxDelayMs: Long = 60000,
    private val jitterFactor: Double = 0.1
) {

    suspend fun <T> execute(operation: suspend () -> T): T {
        var lastException: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                return operation()
            } catch (e: Exception) {
                lastException = e

                // Loga a exceção para debug
                ExceptionHandler.logException(e, "ExponentialBackoffRetry attempt ${attempt + 1}")

                // Verifica se deve fazer retry baseado no tipo de exceção
                if (!ExceptionHandler.shouldRetry(e)) {
                    throw e
                }

                // Se não é a última tentativa, aguarda antes de tentar novamente
                if (attempt < maxRetries - 1) {
                    delay(calculateDelay(attempt))
                }
            }
        }

        throw lastException ?: Exception("Retry failed after $maxRetries attempts")
    }

    private fun calculateDelay(attempt: Int): Long {
        // Delay exponencial: baseDelay * (2^attempt)
        val exponentialDelay = baseDelayMs * (2.0.pow(attempt)).toLong()

        // Aplica limite máximo
        val cappedDelay = minOf(exponentialDelay, maxDelayMs)

        // Adiciona jitter para evitar thundering herd
        val jitter = cappedDelay * jitterFactor * Random.nextDouble()

        return cappedDelay + jitter.toLong()
    }

    /**
     * Versão que permite configurar quais exceções não devem ser retriadas
     */
    suspend fun <T> executeWithCustomRetryLogic(
        operation: suspend () -> T,
        shouldRetry: (Exception) -> Boolean = ExceptionHandler::shouldRetry
    ): T {
        var lastException: Exception? = null

        repeat(maxRetries) { attempt ->
            try {
                return operation()
            } catch (e: Exception) {
                lastException = e

                ExceptionHandler.logException(e, "CustomRetryLogic attempt ${attempt + 1}")

                if (!shouldRetry(e)) {
                    throw e
                }

                if (attempt < maxRetries - 1) {
                    delay(calculateDelay(attempt))
                }
            }
        }

        throw lastException ?: Exception("Retry failed after $maxRetries attempts")
    }
}