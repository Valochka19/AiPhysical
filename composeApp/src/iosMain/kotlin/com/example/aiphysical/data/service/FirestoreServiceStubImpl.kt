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
    override suspend fun updateUserRole(uid: String, newRole: String) = FirestoreResult.Failure(msg)
    override suspend fun updateUserBlockStatus(uid: String, isBlocked: Boolean) = FirestoreResult.Failure(msg)

    override fun observeOrganizationMembers(orgId: String): Flow<FirestoreResult> =
        flowOf(FirestoreResult.Failure(msg))

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
}
