package com.example.aiphysical.data.service

import com.example.aiphysical.data.model.*
import kotlinx.coroutines.flow.Flow

sealed class FirestoreResult {
    data class OrgSuccess(val org: Organization) : FirestoreResult()
    data class UserProfileSuccess(val profile: UserProfile) : FirestoreResult()
    data class MembersSuccess(val members: List<UserProfile>) : FirestoreResult()
    data class ChatContactsSuccess(val contacts: List<UserProfile>) : FirestoreResult()
    data class TestHistorySuccess(val results: List<TestResult>) : FirestoreResult()
    data class CourseProgressSuccess(val progressList: List<CourseProgress>) : FirestoreResult()
    data class OrganizationCoursesSuccess(val courses: List<OrganizationCourse>) : FirestoreResult()
    data class PsychChatThreadsSuccess(val threads: List<PsychChatThread>) : FirestoreResult()
    data class PsychChatMessagesSuccess(val messages: List<PsychChatMessage>) : FirestoreResult()
    object GenericSuccess : FirestoreResult()
    object NotFound : FirestoreResult()
    data class Failure(val message: String) : FirestoreResult()
}

interface FirestoreService {
    // ── Registration flow ─────────────────────────────────────────────────────

    /** Шаг 1 (Директор): создаём документ организации с двумя invite-кодами. */
    suspend fun createOrganization(org: Organization): FirestoreResult

    /**
     * Шаг 1 (Психолог): ищем organizations, где inviteCodePsych == [code].
     * Возвращает [FirestoreResult.OrgSuccess] c orgId, если нашли,
     * или [FirestoreResult.NotFound] если такого кода не существует.
     */
    suspend fun findOrgByPsychCode(code: String): FirestoreResult

    /**
     * Шаг 1 (Студент): ищем organizations, где inviteCodeStudent == [code].
     */
    suspend fun findOrgByStudentCode(code: String): FirestoreResult

    /**
     * Шаг 3 (все роли): создаём документ в коллекции users.
     * Обязательно содержит organizationId, чтобы директор мог
     * отфильтровать участников своей организации.
     */
    suspend fun createUserProfile(profile: UserProfile): FirestoreResult

    // ── Director Dashboard — one-shot ─────────────────────────────────────────
    suspend fun getOrganization(orgId: String): FirestoreResult
    suspend fun getUserProfile(uid: String): FirestoreResult

    /**
     * Одноразовый запрос участников (используется для первоначальной загрузки
     * если real-time listener ещё не подключён).
     */
    suspend fun getOrganizationMembers(orgId: String): FirestoreResult

    suspend fun getUserTestHistory(uid: String): FirestoreResult
    suspend fun getUserCourseProgress(uid: String): FirestoreResult

    // ── Support chat with psychologist ─────────────────────────────────────────
    suspend fun getPsychChatContacts(orgId: String, currentUid: String, currentRole: String): FirestoreResult
    fun observePsychChatThreads(orgId: String, currentUid: String): Flow<FirestoreResult>
    fun observePsychChatMessages(chatId: String): Flow<FirestoreResult>
    suspend fun sendPsychChatMessage(
        orgId: String,
        senderId: String,
        senderRole: String,
        recipientId: String,
        recipientRole: String,
        text: String
    ): FirestoreResult

    // ── Director Dashboard — real-time listener ───────────────────────────────

    /**
     * Возвращает холодный [Flow], который слушает коллекцию `users`
     * с фильтром `orgId == [orgId]` через [addSnapshotListener].
     * Каждый раз, когда любой участник организации изменяется (добавление,
     * удаление, обновление поля), Flow эмитирует новый [FirestoreResult.MembersSuccess].
     *
     * Подписка на Firestore отменяется автоматически при отмене coroutine-scope
     * (viewModelScope.cancel() или Job.cancel()).
     */
    fun observeOrganizationMembers(orgId: String): Flow<FirestoreResult>

    // ── Member Management ─────────────────────────────────────────────────────
    suspend fun updateUserRole(uid: String, newRole: String): FirestoreResult
    suspend fun updateUserBlockStatus(uid: String, isBlocked: Boolean): FirestoreResult

    // ── Psychologist: Student Recommendation ──────────────────────────────────
    /**
     * Updates the student's Firestore document with a psychologist recommendation.
     * The student will see this in their "Results" section.
     */
    suspend fun updateStudentRecommendation(
        studentId: String,
        comment: String,
        courseId: String,
        courseName: String,
        priority: String,
        psychId: String
    ): FirestoreResult

    // ── Organization Courses (added by psychologist) ──────────────────────────

    /** Real-time observer on organizations/{orgId}/courses */
    fun observeOrganizationCourses(orgId: String): Flow<FirestoreResult>

    /** One-shot fetch of courses */
    suspend fun getOrganizationCourses(orgId: String): FirestoreResult

    /** Create / publish a new organization-level course */
    suspend fun createOrganizationCourse(orgId: String, course: OrganizationCourse): FirestoreResult

    /** Delete an organization-level course by id */
    suspend fun deleteOrganizationCourse(orgId: String, courseId: String): FirestoreResult

    // ── Student Test Results ───────────────────────────────────────────────────
    /**
     * Saves any student test attempt to `users/{uid}/testResults/{attemptId}`,
     * updates the corresponding top-level metric and recalculates latestAiStatus
     * from the aggregate picture across completed tests.
     */
    suspend fun saveStudentTestResult(
        uid: String,
        submission: StudentTestSubmission
    ): FirestoreResult

    /** Backward-compatible wrapper for the legacy burnout flow. */
    suspend fun saveBurnoutTestResult(
        uid: String,
        score: Int,
        aiAssessment: String,
        feedbackText: String,
        answers: List<com.example.aiphysical.data.model.BurnoutAnswer>
    ): FirestoreResult = saveStudentTestResult(
        uid = uid,
        submission = StudentTestSubmission(
            definition = studentTestDefinitionFor(com.example.aiphysical.presentation.student.StudentTestType.BURNOUT),
            score = score,
            aiAssessment = aiAssessment,
            feedbackText = feedbackText,
            answers = answers
        )
    )
}
