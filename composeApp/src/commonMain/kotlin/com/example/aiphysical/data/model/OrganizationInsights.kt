package com.example.aiphysical.data.model

import com.example.aiphysical.presentation.auth.AppLanguage
import com.example.aiphysical.presentation.student.StudentTestType

data class OrganizationTestStats(
    val testType: StudentTestType,
    val testId: String,
    val testName: String,
    val totalAttempts: Int = 0,
    val mostFrequentAssessment: String = "unknown"
)

data class CourseCompletionMember(
    val userId: String,
    val fullName: String,
    val progress: Float = 0f,
    val lastAccessMillis: Long = 0L
)

data class BaseCourseCompletionStats(
    val courseId: String,
    val courseName: String,
    val completedCount: Int = 0,
    val inProgressCount: Int = 0,
    val notStartedCount: Int = 0
)

data class BaseCourseCompletionDetails(
    val courseId: String,
    val courseName: String,
    val completedMembers: List<CourseCompletionMember> = emptyList(),
    val inProgressMembers: List<CourseCompletionMember> = emptyList(),
    val notStartedMembers: List<CourseCompletionMember> = emptyList()
)

fun assessmentLabel(assessment: String, language: AppLanguage = AppLanguage.RU): String = when (assessment) {
    "normal" -> when (language) {
        AppLanguage.RU -> "Норма"
        AppLanguage.EN -> "Normal"
        AppLanguage.KZ -> "Қалыпты"
    }
    "stress" -> when (language) {
        AppLanguage.RU -> "Стресс"
        AppLanguage.EN -> "Stress"
        AppLanguage.KZ -> "Стресс"
    }
    "critical" -> when (language) {
        AppLanguage.RU -> "Критично"
        AppLanguage.EN -> "Critical"
        AppLanguage.KZ -> "Критикалық"
    }
    else -> when (language) {
        AppLanguage.RU -> "Нет данных"
        AppLanguage.EN -> "No data"
        AppLanguage.KZ -> "Дерек жоқ"
    }
}

