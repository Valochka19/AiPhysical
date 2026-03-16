package com.example.aiphysical.presentation.student

sealed class StudentEvent {
    object LoadData : StudentEvent()
    object Refresh : StudentEvent()
    data class NavigateToTab(val tab: StudentTab) : StudentEvent()
    data class StartTest(val testType: StudentTestType) : StudentEvent()
    object GenerateReport : StudentEvent()
    object DismissError : StudentEvent()
    object Logout : StudentEvent()
}

