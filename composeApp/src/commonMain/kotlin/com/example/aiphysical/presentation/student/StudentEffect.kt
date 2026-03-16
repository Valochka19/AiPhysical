package com.example.aiphysical.presentation.student

sealed class StudentEffect {
    data class ShowSnackbar(val message: String) : StudentEffect()
    data class NavigateToTest(val testType: StudentTestType) : StudentEffect()
    data class OpenUrl(val url: String) : StudentEffect()
}
