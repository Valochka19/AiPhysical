package com.example.aiphysical.data.model

import com.example.aiphysical.presentation.auth.AppLanguage
import com.example.aiphysical.presentation.student.StudentTestType

data class StudentTestCatalogItem(
    val type: StudentTestType,
    val testId: String,
    val emoji: String,
    val title: String,
    val description: String,
    val accentColorHex: Long
)

object AppStudentTestCatalog {
    val items: List<StudentTestCatalogItem> = StudentTestType.entries.map { type ->
        val definition = studentTestDefinitionFor(type)
        StudentTestCatalogItem(
            type = type,
            testId = type.testId,
            emoji = type.emoji,
            title = definition.testName,
            description = definition.purpose,
            accentColorHex = type.colorStartHex
        )
    }
}

fun StudentTestCatalogItem.displayTitle(language: AppLanguage): String = when (language) {
    AppLanguage.RU -> title
    AppLanguage.EN -> when (type) {
        StudentTestType.BURNOUT -> "Burnout"
        StudentTestType.STRESS -> "Stress"
        StudentTestType.EMOTION -> "Condition"
        StudentTestType.MOTIVATION -> "Motivation"
        StudentTestType.ANXIETY -> "Anxiety"
    }
    AppLanguage.KZ -> when (type) {
        StudentTestType.BURNOUT -> "Күйіп кету"
        StudentTestType.STRESS -> "Стресс"
        StudentTestType.EMOTION -> "Жағдай"
        StudentTestType.MOTIVATION -> "Мотивация"
        StudentTestType.ANXIETY -> "Мазасыздық"
    }
}

fun StudentTestCatalogItem.displayDescription(language: AppLanguage): String = when (language) {
    AppLanguage.RU -> description
    AppLanguage.EN -> when (type) {
        StudentTestType.BURNOUT -> "Assesses signs of emotional burnout, fatigue, and resource depletion in study and workload."
        StudentTestType.STRESS -> "Assesses the current level of physical and mental tension caused by workload and deadlines."
        StudentTestType.EMOTION -> "Assesses the overall emotional background, life satisfaction, and connection with everyday joy."
        StudentTestType.MOTIVATION -> "Assesses engagement in studies and projects, inner meaning, and the level of procrastination."
        StudentTestType.ANXIETY -> "Assesses anxiety, excessive worrying, and tension related to the future."
    }
    AppLanguage.KZ -> when (type) {
        StudentTestType.BURNOUT -> "Оқу мен жүктеме аясындағы эмоциялық күйіп кету, қажу және ресурс жоғалту белгілерін бағалау."
        StudentTestType.STRESS -> "Жүктеме мен дедлайндарға байланысты қазіргі физикалық және психикалық кернеу деңгейін бағалау."
        StudentTestType.EMOTION -> "Жалпы эмоциялық фонды, өмірге қанағаттануды және күнделікті қуанышпен байланысты бағалау."
        StudentTestType.MOTIVATION -> "Оқу мен жобаларға тартылуды, ішкі мағынаны және кейінге қалдыру деңгейін бағалау."
        StudentTestType.ANXIETY -> "Мазасыздық, шамадан тыс уайымдау және болашақ алдындағы кернеу деңгейін бағалау."
    }
}

