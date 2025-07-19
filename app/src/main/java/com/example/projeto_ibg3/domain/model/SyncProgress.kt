package com.example.projeto_ibg3.domain.model

data class SyncProgress(
    val progress: Float = 0f,
    val currentStep: String = "",
    val totalSteps: Int = 0,
    val currentStepIndex: Int = 0
)
