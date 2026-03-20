package com.example.aiphysical.data.model

import com.example.aiphysical.presentation.auth.AppLanguage
import com.example.aiphysical.presentation.student.StudentTestType
import com.example.aiphysical.presentation.student.displayLabel

data class TestResult(
    val testId: String = "",
    /** Firestore document id — unique per attempt, needed when one test is taken multiple times */
    val attemptId: String = "",
    val testName: String = "",
    val dateMillis: Long = 0L,
    val score: Float = 0f,             // 0–100
    val aiAssessment: String = "unknown", // "normal" | "stress" | "critical"
    /** Short AI feedback text stored with the result */
    val feedbackText: String = ""
)

fun TestResult.displayTitle(language: AppLanguage): String {
    val builtInType = StudentTestType.entries.firstOrNull { type ->
        type.testId.equals(testId, ignoreCase = true) ||
            type.label.equals(testName, ignoreCase = true)
    }
    return builtInType?.displayLabel(language)
        ?: testName.ifBlank { testId.ifBlank { "—" } }
}

