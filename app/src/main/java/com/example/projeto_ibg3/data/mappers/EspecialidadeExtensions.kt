package com.example.projeto_ibg3.data.mappers

import com.example.projeto_ibg3.data.remote.dto.EspecialidadeDto

// Funções auxiliares para converter datas ISO para timestamp
fun EspecialidadeDto.getCreatedAtTimestamp(): Long {
    return try {
        if (createdAt != null) {
            // Converter "2025-07-12T11:18:55" para timestamp
            val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
            val localDateTime = java.time.LocalDateTime.parse(createdAt, formatter)
            localDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        } else {
            System.currentTimeMillis()
        }
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
}

fun EspecialidadeDto.getUpdatedAtTimestamp(): Long {
    return try {
        if (updatedAt != null) {
            val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
            val localDateTime = java.time.LocalDateTime.parse(updatedAt, formatter)
            localDateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        } else {
            System.currentTimeMillis()
        }
    } catch (e: Exception) {
        System.currentTimeMillis()
    }
}