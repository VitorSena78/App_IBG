package com.example.projeto_ibg3.data.mappers

import java.text.SimpleDateFormat
import java.time.Instant
import java.util.*

/**
 * Utilitários centralizados para conversão de datas
 * Evita duplicação de código entre diferentes mappers
 */
object DateMapperUtils {

    private val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * Converte Date para formato ISO string
     */
    fun formatDateToIso(date: Date): String {
        return try {
            val instant = Instant.ofEpochMilli(date.time)
            instant.toString()
        } catch (e: Exception) {
            Instant.now().toString()
        }
    }

    /**
     * Converte timestamp (Long) para formato ISO string
     */
    fun formatTimestampToIso(timestamp: Long): String {
        return try {
            val instant = Instant.ofEpochMilli(timestamp)
            instant.toString()
        } catch (e: Exception) {
            Instant.now().toString()
        }
    }

    /**
     * Converte string ISO para Date
     */
    fun parseIsoDate(isoString: String?): Date {
        return try {
            if (isoString.isNullOrBlank()) {
                Date()
            } else {
                // Tenta primeiro formato completo ISO
                val instant = Instant.parse(isoString)
                Date.from(instant)
            }
        } catch (e: Exception) {
            // Tenta formato alternativo (apenas data)
            try {
                if (isoString != null && isoString.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                    simpleDateFormat.parse(isoString) ?: Date()
                } else {
                    Date()
                }
            } catch (e2: Exception) {
                Date()
            }
        }
    }

    /**
     * Converte string ISO para timestamp (Long)
     */
    fun parseIsoToTimestamp(isoString: String?): Long? {
        return try {
            parseIsoDate(isoString).time
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Converte Date para timestamp (Long)
     */
    fun dateToTimestamp(date: Date?): Long {
        return date?.time ?: System.currentTimeMillis()
    }

    /**
     * Converte timestamp (Long) para Date
     */
    fun timestampToDate(timestamp: Long): Date {
        return if (timestamp > 0) Date(timestamp) else Date()
    }

    /**
     * Obtém timestamp atual
     */
    fun getCurrentTimestamp(): Long = System.currentTimeMillis()

    /**
     * Obtém data atual
     */
    fun getCurrentDate(): Date = Date()
}