package com.example.aiphysical.data.model

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
            isAiRecommended = true
        ),
        BaseCourseCatalogItem(
            id = "base_burnout_prevention",
            title = "Профилактика выгорания",
            description = "Диагностика и профилактика профессионального выгорания",
            durationLabel = "6 уроков",
            emoji = "🛡️",
            accentColorHex = 0xFFFF5370,
            isAiRecommended = true
        ),
        BaseCourseCatalogItem(
            id = "base_emotional_intelligence",
            title = "Эмоциональный интеллект",
            description = "Управляй своими эмоциями и реакциями",
            durationLabel = "7 уроков",
            emoji = "💡",
            accentColorHex = 0xFFFF8C00,
            isAiRecommended = true
        ),
        BaseCourseCatalogItem(
            id = "base_motivation_boost",
            title = "Повышение мотивации",
            description = "Как восстановить ресурс и найти внутреннюю цель",
            durationLabel = "5 уроков",
            emoji = "🚀",
            accentColorHex = 0xFF4FD18A,
            isAiRecommended = false
        ),
        BaseCourseCatalogItem(
            id = "base_team_communication",
            title = "Коммуникации в коллективе",
            description = "Улучшение командного взаимодействия и общения",
            durationLabel = "5 уроков",
            emoji = "🤝",
            accentColorHex = 0xFF9D5FF5,
            isAiRecommended = false
        )
    )
}

