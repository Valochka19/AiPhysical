package com.example.aiphysical.data.model

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

