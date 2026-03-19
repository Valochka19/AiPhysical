package com.example.aiphysical.data.service

import com.example.aiphysical.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * iOS stub — replace with a real Firestore iOS implementation once
 * Firebase iOS SDK is added via Swift Package Manager in Xcode.
 */
class FirestoreServiceStubImpl : FirestoreService {
    private val msg = "Firestore not configured for iOS yet."

    override suspend fun createOrganization(org: Organization) = FirestoreResult.Failure(msg)
    override suspend fun findOrgByStudentCode(code: String) = FirestoreResult.Failure(msg)
    override suspend fun findOrgByPsychCode(code: String) = FirestoreResult.Failure(msg)
    override suspend fun createUserProfile(profile: UserProfile) = FirestoreResult.Failure(msg)
    override suspend fun getOrganization(orgId: String) = FirestoreResult.Failure(msg)
    override suspend fun getUserProfile(uid: String) = FirestoreResult.Failure(msg)
    override suspend fun getOrganizationMembers(orgId: String) = FirestoreResult.Failure(msg)
    override suspend fun getUserTestHistory(uid: String) = FirestoreResult.Failure(msg)
    override suspend fun getUserCourseProgress(uid: String) = FirestoreResult.Failure(msg)
    override suspend fun getUserPointsLedger(uid: String) = FirestoreResult.PointsLedgerSuccess(emptyList())
    override suspend fun getPsychChatContacts(orgId: String, currentUid: String, currentRole: String) = FirestoreResult.Failure(msg)
    override suspend fun updateUserRole(uid: String, newRole: String) = FirestoreResult.Failure(msg)
    override suspend fun updateUserBlockStatus(uid: String, isBlocked: Boolean) = FirestoreResult.Failure(msg)

    override fun observeOrganizationMembers(orgId: String): Flow<FirestoreResult> =
        flowOf(FirestoreResult.Failure(msg))

    override fun observePsychChatThreads(orgId: String, currentUid: String): Flow<FirestoreResult> =
        flowOf(FirestoreResult.Failure(msg))

    override fun observePsychChatMessages(chatId: String): Flow<FirestoreResult> =
        flowOf(FirestoreResult.Failure(msg))

    override suspend fun sendPsychChatMessage(
        orgId: String,
        senderId: String,
        senderRole: String,
        recipientId: String,
        recipientRole: String,
        text: String
    ): FirestoreResult = FirestoreResult.Failure(msg)

    override suspend fun updateStudentRecommendation(
        studentId: String,
        comment: String,
        courseId: String,
        courseName: String,
        priority: String,
        psychId: String
    ): FirestoreResult = FirestoreResult.Failure(msg)

    // ── Organization Courses stubs ────────────────────────────────────────────

    override fun observeOrganizationCourses(orgId: String): Flow<FirestoreResult> =
        flowOf(FirestoreResult.OrganizationCoursesSuccess(emptyList()))

    override suspend fun getOrganizationCourses(orgId: String): FirestoreResult =
        FirestoreResult.OrganizationCoursesSuccess(emptyList())

    override suspend fun createOrganizationCourse(orgId: String, course: OrganizationCourse): FirestoreResult =
        FirestoreResult.Failure(msg)

    override suspend fun deleteOrganizationCourse(orgId: String, courseId: String): FirestoreResult =
        FirestoreResult.Failure(msg)

    override fun observeOrganizationCustomTests(orgId: String): Flow<FirestoreResult> =
        flowOf(FirestoreResult.OrganizationCustomTestsSuccess(emptyList()))

    override suspend fun getOrganizationCustomTests(orgId: String): FirestoreResult =
        FirestoreResult.OrganizationCustomTestsSuccess(emptyList())

    override suspend fun createOrganizationCustomTest(orgId: String, test: OrganizationCustomTest): FirestoreResult =
        FirestoreResult.Failure(msg)

    override suspend fun submitOrganizationCustomTest(
        orgId: String,
        testId: String,
        submission: OrganizationCustomTestSubmission
    ): FirestoreResult = FirestoreResult.Failure(msg)

    override suspend fun upsertBaseCourseProgress(uid: String, course: BaseCourseCatalogItem): FirestoreResult =
        FirestoreResult.Failure(msg)

    override suspend fun getOrganizationBaseCourseCompletionStats(orgId: String): FirestoreResult =
        FirestoreResult.BaseCourseCompletionStatsSuccess(emptyList())

    override suspend fun getOrganizationBaseCourseCompletionDetails(orgId: String, courseId: String): FirestoreResult =
        FirestoreResult.BaseCourseCompletionDetailsSuccess(
            BaseCourseCompletionDetails(courseId = courseId, courseName = "")
        )

    override suspend fun getOrganizationTestStats(orgId: String): FirestoreResult =
        FirestoreResult.OrganizationTestStatsSuccess(emptyList())

    // ── Student Test Result stub ──────────────────────────────────────────────

    override suspend fun saveStudentTestResult(
        uid: String,
        submission: StudentTestSubmission
    ): FirestoreResult = FirestoreResult.Failure(msg)
}
