package com.example.aiphysical.data.model

import com.example.aiphysical.presentation.auth.AppLanguage

// ── Course content type ───────────────────────────────────────────────────────

enum class CourseContentType { TEXT, VIDEO }

// ── Organization-level course added by psychologist ───────────────────────────

data class OrganizationCourse(
    val id: String = "",
    val orgId: String = "",
    val title: String = "",
    val description: String = "",
    val type: CourseContentType = CourseContentType.TEXT,
    val contentText: String = "",
    val videoUrl: String = "",
    val createdBy: String = "",
    val createdByName: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isPublished: Boolean = true
)

// ── Base platform course (shared catalog) ─────────────────────────────────────

data class BaseCourseCatalogItem(
    val id: String,
    val title: String,
    val description: String,
    val durationLabel: String,
    val emoji: String,
    val accentColorHex: Long,
    val courseUrl: String,
    val isAiRecommended: Boolean = false
)

// ── Single source of truth for the 5 canonical base courses ──────────────────

object AppCourseCatalog {
    val baseCourses: List<BaseCourseCatalogItem> = listOf(
        BaseCourseCatalogItem(
            id = "base_stress_management",
            title = "Управление стрессом",
            description = "Техники релаксации и снижения тревожности",
            durationLabel = "6 уроков",
            emoji = "🧘",
            accentColorHex = 0xFF00CED1,
            courseUrl = "https://youtu.be/aysI7KtC41E",
            isAiRecommended = true
        ),
        BaseCourseCatalogItem(
            id = "base_burnout_prevention",
            title = "Профилактика выгорания",
            description = "Диагностика и профилактика профессионального выгорания",
            durationLabel = "6 уроков",
            emoji = "🛡️",
            accentColorHex = 0xFFFF5370,
            courseUrl = "https://youtu.be/itqzMEq-TRk",
            isAiRecommended = true
        ),
        BaseCourseCatalogItem(
            id = "base_emotional_intelligence",
            title = "Эмоциональный интеллект",
            description = "Управляй своими эмоциями и реакциями",
            durationLabel = "7 уроков",
            emoji = "💡",
            accentColorHex = 0xFFFF8C00,
            courseUrl = "https://youtu.be/ParxvOOsjsk",
            isAiRecommended = true
        ),
        BaseCourseCatalogItem(
            id = "base_motivation_boost",
            title = "Повышение мотивации",
            description = "Как восстановить ресурс и найти внутреннюю цель",
            durationLabel = "5 уроков",
            emoji = "🚀",
            accentColorHex = 0xFF4FD18A,
            courseUrl = "https://youtu.be/JzGDsl0DpAY",
            isAiRecommended = false
        ),
        BaseCourseCatalogItem(
            id = "base_team_communication",
            title = "Коммуникации в коллективе",
            description = "Улучшение командного взаимодействия и общения",
            durationLabel = "5 уроков",
            emoji = "🤝",
            accentColorHex = 0xFF9D5FF5,
            courseUrl = "https://youtu.be/9w6YkGc2nDc",
            isAiRecommended = false
        )
    )

    fun baseCourseById(courseId: String): BaseCourseCatalogItem? =
        baseCourses.firstOrNull { it.id == courseId }

    fun computeBaseCourseProgressPercent(progressByCourseId: Map<String, Float>): Float {
        if (baseCourses.isEmpty()) return 0f
        return (baseCourses
            .map { progressByCourseId[it.id]?.coerceIn(0f, 1f) ?: 0f }
            .average()
            .toFloat() * 100f)
            .coerceIn(0f, 100f)
    }
}

fun BaseCourseCatalogItem.displayTitle(language: AppLanguage): String = when (language) {
    AppLanguage.RU -> title
    AppLanguage.EN -> when (id) {
        "base_stress_management" -> "Stress Management"
        "base_burnout_prevention" -> "Burnout Prevention"
        "base_emotional_intelligence" -> "Emotional Intelligence"
        "base_motivation_boost" -> "Motivation Boost"
        "base_team_communication" -> "Team Communication"
        else -> title
    }
    AppLanguage.KZ -> when (id) {
        "base_stress_management" -> "Стрессті басқару"
        "base_burnout_prevention" -> "Күйіп кетудің алдын алу"
        "base_emotional_intelligence" -> "Эмоциялық интеллект"
        "base_motivation_boost" -> "Мотивацияны күшейту"
        "base_team_communication" -> "Ұжымдағы коммуникация"
        else -> title
    }
}

fun BaseCourseCatalogItem.displayDescription(language: AppLanguage): String = when (language) {
    AppLanguage.RU -> description
    AppLanguage.EN -> when (id) {
        "base_stress_management" -> "Relaxation techniques and anxiety reduction"
        "base_burnout_prevention" -> "Diagnosing and preventing professional burnout"
        "base_emotional_intelligence" -> "Manage your emotions and reactions"
        "base_motivation_boost" -> "How to restore energy and find inner purpose"
        "base_team_communication" -> "Improve teamwork and communication"
        else -> description
    }
    AppLanguage.KZ -> when (id) {
        "base_stress_management" -> "Босаңсу және мазасыздықты азайту техникалары"
        "base_burnout_prevention" -> "Кәсіби күйіп кетуді анықтау және алдын алу"
        "base_emotional_intelligence" -> "Эмоцияларың мен реакцияларыңды басқар"
        "base_motivation_boost" -> "Ресурсты қалпына келтіріп, ішкі мақсатты табу"
        "base_team_communication" -> "Командалық өзара әрекет пен қарым-қатынасты жақсарту"
        else -> description
    }
}

fun BaseCourseCatalogItem.displayDurationLabel(language: AppLanguage): String = when (language) {
    AppLanguage.RU -> durationLabel
    AppLanguage.EN -> when (id) {
        "base_emotional_intelligence" -> "7 lessons"
        "base_stress_management", "base_burnout_prevention" -> "6 lessons"
        "base_motivation_boost", "base_team_communication" -> "5 lessons"
        else -> durationLabel
    }
    AppLanguage.KZ -> when (id) {
        "base_emotional_intelligence" -> "7 сабақ"
        "base_stress_management", "base_burnout_prevention" -> "6 сабақ"
        "base_motivation_boost", "base_team_communication" -> "5 сабақ"
        else -> durationLabel
    }
}

fun CourseContentType.displayLabel(language: AppLanguage): String = when (language) {
    AppLanguage.RU -> if (this == CourseContentType.VIDEO) "Видео" else "Текстовый"
    AppLanguage.EN -> if (this == CourseContentType.VIDEO) "Video" else "Text"
    AppLanguage.KZ -> if (this == CourseContentType.VIDEO) "Видео" else "Мәтіндік"
}

