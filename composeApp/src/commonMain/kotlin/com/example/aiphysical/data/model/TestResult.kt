package com.example.aiphysical.data.model

data class TestResult(
    val testId: String = "",
    val testName: String = "",
    val dateMillis: Long = 0L,
    val score: Float = 0f,           // 0–100
    val aiAssessment: String = "unknown" // "normal" | "stress" | "critical"
)

