package com.example.aiphysical.presentation.student

import com.example.aiphysical.data.model.AnswerType
import com.example.aiphysical.data.model.OrganizationCourse
import com.example.aiphysical.presentation.auth.AppLanguage

sealed class StudentEvent {
    object LoadData : StudentEvent()
    object Refresh : StudentEvent()
    data class NavigateToTab(val tab: StudentTab) : StudentEvent()
    data class StartTest(val testType: StudentTestType) : StudentEvent()
    object GenerateReport : StudentEvent()
    object DismissError : StudentEvent()
    object Logout : StudentEvent()
    data class ChangeLanguage(val language: AppLanguage) : StudentEvent()
    // ── Added courses ─────────────────────────────────────────────────────────
    object OpenAddedCourses : StudentEvent()
    object CloseAddedCourses : StudentEvent()
    data class OpenAddedCourse(val course: OrganizationCourse) : StudentEvent()
    object CloseSelectedAddedCourse : StudentEvent()
    data class OpenTextCourse(val course: OrganizationCourse) : StudentEvent()
    object CloseTextCourse : StudentEvent()
    // ── AI Chat ───────────────────────────────────────────────────────────────
    object OpenAiChat : StudentEvent()
    object CloseAiChat : StudentEvent()
    data class SendChatMessage(val message: String) : StudentEvent()
    data class UpdateChatInput(val text: String) : StudentEvent()
    object ClearChatError : StudentEvent()
    object ClearChatHistory : StudentEvent()
    // ── Generic Student Test ──────────────────────────────────────────────────
    object CloseActiveTest : StudentEvent()
    data class AnswerCurrentTestQuestion(val answerType: AnswerType) : StudentEvent()
    object RetryCurrentTestGemini : StudentEvent()
    object ResetCurrentTest : StudentEvent()
    // ── Burnout compatibility wrappers ────────────────────────────────────────
    object OpenBurnoutTest : StudentEvent()
    object CloseBurnoutTest : StudentEvent()
    data class AnswerBurnoutQuestion(val answerType: AnswerType) : StudentEvent()
    object RetryBurnoutGemini : StudentEvent()
    object ResetBurnoutTest : StudentEvent()
}
