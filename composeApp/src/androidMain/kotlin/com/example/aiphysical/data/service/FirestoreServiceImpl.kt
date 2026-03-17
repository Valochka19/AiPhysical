package com.example.aiphysical.data.service

import com.example.aiphysical.data.model.*
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreServiceImpl : FirestoreService {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // ── Existing registration methods ─────────────────────────────────────────

    override suspend fun createOrganization(org: Organization): FirestoreResult {
        return try {
            val data = mapOf(
                "id" to org.id, "name" to org.name, "directorId" to org.directorId,
                "inviteCodeStudent" to org.inviteCodeStudent, "inviteCodePsych" to org.inviteCodePsych
            )
            db.collection("organizations").document(org.id).set(data).await()
            FirestoreResult.GenericSuccess
        } catch (e: Exception) { FirestoreResult.Failure(e.localizedMessage ?: "Ошибка создания организации") }
    }

    override suspend fun findOrgByStudentCode(code: String): FirestoreResult {
        return try {
            val snap = db.collection("organizations").whereEqualTo("inviteCodeStudent", code).get().await()
            if (snap.isEmpty) FirestoreResult.NotFound
            else FirestoreResult.OrgSuccess(snap.documents.first().toOrganization())
        } catch (e: Exception) { FirestoreResult.Failure(e.localizedMessage ?: "Ошибка запроса Firestore") }
    }

    override suspend fun findOrgByPsychCode(code: String): FirestoreResult {
        return try {
            val snap = db.collection("organizations").whereEqualTo("inviteCodePsych", code).get().await()
            if (snap.isEmpty) FirestoreResult.NotFound
            else FirestoreResult.OrgSuccess(snap.documents.first().toOrganization())
        } catch (e: Exception) { FirestoreResult.Failure(e.localizedMessage ?: "Ошибка запроса Firestore") }
    }

    override suspend fun createUserProfile(profile: UserProfile): FirestoreResult {
        return try {
            val data = mapOf(
                "uid" to profile.uid, "fullName" to profile.fullName, "email" to profile.email,
                "role" to profile.role, "orgId" to profile.orgId, "ageGroup" to profile.ageGroup,
                "latestAiStatus" to profile.latestAiStatus,
                "stressScore" to profile.stressScore,
                "courseProgressPercent" to profile.courseProgressPercent,
                "burnoutScore" to profile.burnoutScore,
                "emotionScore" to profile.emotionScore,
                "motivationScore" to profile.motivationScore,
                "anxietyScore" to profile.anxietyScore,
                "isBlocked" to profile.isBlocked,
                "psychComment" to profile.psychComment,
                "assignedCourseId" to profile.assignedCourseId,
                "assignedCourseName" to profile.assignedCourseName,
                "psychPriority" to profile.psychPriority,
                "psychCommentDate" to profile.psychCommentDate
            )
            db.collection("users").document(profile.uid).set(data).await()
            FirestoreResult.GenericSuccess
        } catch (e: Exception) { FirestoreResult.Failure(e.localizedMessage ?: "Ошибка создания профиля пользователя") }
    }

    // ── Director Dashboard methods ─────────────────────────────────────────────

    override suspend fun getOrganization(orgId: String): FirestoreResult {
        return try {
            val doc = db.collection("organizations").document(orgId).get().await()
            if (!doc.exists()) FirestoreResult.NotFound
            else FirestoreResult.OrgSuccess(doc.toOrganization())
        } catch (e: Exception) { FirestoreResult.Failure(e.localizedMessage ?: "Ошибка запроса Firestore") }
    }

    override suspend fun getUserProfile(uid: String): FirestoreResult {
        return try {
            val doc = db.collection("users").document(uid).get().await()
            if (!doc.exists()) FirestoreResult.NotFound
            else FirestoreResult.UserProfileSuccess(doc.toUserProfile())
        } catch (e: Exception) { FirestoreResult.Failure(e.localizedMessage ?: "Ошибка запроса Firestore") }
    }

    override suspend fun getOrganizationMembers(orgId: String): FirestoreResult {
        return try {
            val snap = db.collection("users").whereEqualTo("orgId", orgId).get().await()
            val members = snap.documents.map { it.toUserProfile() }
            FirestoreResult.MembersSuccess(members)
        } catch (e: Exception) { FirestoreResult.Failure(e.localizedMessage ?: "Ошибка запроса Firestore") }
    }

    // ── REAL-TIME: слушаем коллекцию users только для своей организации ───────
    //
    // Firestore query:  users WHERE orgId == orgId
    //
    // addSnapshotListener срабатывает при КАЖДОМ изменении документа:
    //   • новый участник зарегистрировался
    //   • поле stressScore / latestAiStatus обновилось
    //   • участник заблокирован / удалён
    //
    // callbackFlow превращает callback-based API в корутинный Flow.
    // awaitClose { listener.remove() } — гарантирует, что слушатель Firestore
    // будет отменён, когда coroutine-scope (viewModelScope) закрывается.
    override fun observeOrganizationMembers(orgId: String): Flow<FirestoreResult> = callbackFlow {
        val listenerRegistration = db.collection("users")
            .whereEqualTo("orgId", orgId)             // фильтр по организации
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Ошибка сети или прав доступа — отправляем Failure, но НЕ закрываем Flow
                    // (Firestore сам переподключится при восстановлении сети)
                    trySend(FirestoreResult.Failure(error.localizedMessage ?: "Ошибка слушателя"))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val members = snapshot.documents.map { it.toUserProfile() }
                    trySend(FirestoreResult.MembersSuccess(members))
                }
            }

        // Отменяем Firestore listener при отмене coroutine
        awaitClose { listenerRegistration.remove() }
    }

    override suspend fun getUserTestHistory(uid: String): FirestoreResult {
        return try {
            val snap = db.collection("users").document(uid)
                .collection("testResults").orderBy("dateMillis").get().await()
            val results = snap.documents.map { doc ->
                TestResult(
                    // Read testId from stored field first; fall back to doc.id for legacy records
                    testId       = doc.getString("testId") ?: doc.id,
                    attemptId    = doc.id,
                    testName     = doc.getString("testName") ?: "—",
                    dateMillis   = doc.getLong("dateMillis") ?: 0L,
                    score        = (doc.getDouble("score") ?: 0.0).toFloat(),
                    aiAssessment = doc.getString("aiAssessment") ?: "unknown",
                    feedbackText = doc.getString("feedbackText") ?: ""
                )
            }
            FirestoreResult.TestHistorySuccess(results)
        } catch (e: Exception) { FirestoreResult.Failure(e.localizedMessage ?: "Ошибка запроса Firestore") }
    }

    override suspend fun getUserCourseProgress(uid: String): FirestoreResult {
        return try {
            val snap = db.collection("users").document(uid)
                .collection("courseProgress").get().await()
            val progressList = snap.documents.map { doc ->
                CourseProgress(
                    courseId = doc.id,
                    courseName = doc.getString("courseName") ?: "—",
                    progress = (doc.getDouble("progress") ?: 0.0).toFloat(),
                    lastAccessMillis = doc.getLong("lastAccessMillis") ?: 0L
                )
            }
            FirestoreResult.CourseProgressSuccess(progressList)
        } catch (e: Exception) { FirestoreResult.Failure(e.localizedMessage ?: "Ошибка запроса Firestore") }
    }

    override suspend fun updateUserRole(uid: String, newRole: String): FirestoreResult {
        return try {
            db.collection("users").document(uid).update("role", newRole).await()
            FirestoreResult.GenericSuccess
        } catch (e: Exception) { FirestoreResult.Failure(e.localizedMessage ?: "Ошибка изменения роли") }
    }

    override suspend fun updateUserBlockStatus(uid: String, isBlocked: Boolean): FirestoreResult {
        return try {
            db.collection("users").document(uid).update("isBlocked", isBlocked).await()
            FirestoreResult.GenericSuccess
        } catch (e: Exception) { FirestoreResult.Failure(e.localizedMessage ?: "Ошибка блокировки") }
    }

    override suspend fun updateStudentRecommendation(
        studentId: String,
        comment: String,
        courseId: String,
        courseName: String,
        priority: String,
        psychId: String
    ): FirestoreResult {
        return try {
            val data = mapOf(
                "psychComment"       to comment,
                "assignedCourseId"   to courseId,
                "assignedCourseName" to courseName,
                "psychPriority"      to priority,
                "psychCommentDate"   to System.currentTimeMillis()
            )
            db.collection("users").document(studentId).update(data).await()
            FirestoreResult.GenericSuccess
        } catch (e: Exception) {
            FirestoreResult.Failure(e.localizedMessage ?: "Ошибка обновления рекомендации")
        }
    }

    // ── Organization Courses ──────────────────────────────────────────────────

    override fun observeOrganizationCourses(orgId: String): Flow<FirestoreResult> = callbackFlow {
        val ref = db.collection("organizations").document(orgId).collection("courses")
        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(FirestoreResult.Failure(error.localizedMessage ?: "Ошибка слушателя курсов"))
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val courses = snapshot.documents.map { it.toOrganizationCourse() }
                trySend(FirestoreResult.OrganizationCoursesSuccess(courses))
            }
        }
        awaitClose { listener.remove() }
    }

    override suspend fun getOrganizationCourses(orgId: String): FirestoreResult {
        return try {
            val snap = db.collection("organizations").document(orgId).collection("courses").get().await()
            FirestoreResult.OrganizationCoursesSuccess(snap.documents.map { it.toOrganizationCourse() })
        } catch (e: Exception) { FirestoreResult.Failure(e.localizedMessage ?: "Ошибка загрузки курсов") }
    }

    override suspend fun createOrganizationCourse(orgId: String, course: OrganizationCourse): FirestoreResult {
        return try {
            val ref = if (course.id.isBlank())
                db.collection("organizations").document(orgId).collection("courses").document()
            else
                db.collection("organizations").document(orgId).collection("courses").document(course.id)
            val data = mapOf(
                "id"            to ref.id,
                "orgId"         to orgId,
                "title"         to course.title,
                "description"   to course.description,
                "type"          to course.type.name,
                "contentText"   to course.contentText,
                "videoUrl"      to course.videoUrl,
                "createdBy"     to course.createdBy,
                "createdByName" to course.createdByName,
                "createdAt"     to course.createdAt,
                "updatedAt"     to course.updatedAt,
                "isPublished"   to course.isPublished
            )
            ref.set(data).await()
            FirestoreResult.GenericSuccess
        } catch (e: Exception) { FirestoreResult.Failure(e.localizedMessage ?: "Ошибка публикации курса") }
    }

    override suspend fun deleteOrganizationCourse(orgId: String, courseId: String): FirestoreResult {
        return try {
            db.collection("organizations").document(orgId).collection("courses").document(courseId).delete().await()
            FirestoreResult.GenericSuccess
        } catch (e: Exception) { FirestoreResult.Failure(e.localizedMessage ?: "Ошибка удаления курса") }
    }

    // ── Student Test Result ───────────────────────────────────────────────────

    override suspend fun saveStudentTestResult(
        uid: String,
        submission: StudentTestSubmission
    ): FirestoreResult {
        return try {
            val now = System.currentTimeMillis()
            val userRef = db.collection("users").document(uid)
            val attemptRef = userRef.collection("testResults").document()

            val answersData = submission.answers.map { a ->
                mapOf(
                    "questionId"   to a.questionId,
                    "questionText" to a.questionText,
                    "catEmotion"   to a.catEmotion.name,
                    "answerType"   to a.answerType.name,
                    "answerLabel"  to a.answerType.label,
                    "weight"       to a.answerType.weight,
                    "polarity"     to a.polarity.name,
                    "isPositive"   to (a.polarity == QuestionPolarity.POSITIVE)
                )
            }
            val docData = mapOf(
                "testId"        to submission.definition.testId,
                "testName"      to submission.definition.testName,
                "dateMillis"    to now,
                "score"         to submission.score.toDouble(),
                "aiAssessment"  to submission.aiAssessment,
                "feedbackText"  to submission.feedbackText,
                "questionCount" to submission.answers.size,
                "version"       to submission.version,
                "answers"       to answersData
            )
            attemptRef.set(docData).await()

            val currentProfile = userRef.get().await().let { doc ->
                if (doc.exists()) doc.toUserProfile() else UserProfile(uid = uid)
            }
            val updatedProfile = currentProfile.withUpdatedMetric(
                submission.definition.testId,
                submission.score.toFloat()
            )
            val completedTestIds = userRef.collection("testResults").get().await()
                .documents
                .mapNotNull { it.getString("testId") }
                .toSet() + submission.definition.testId
            val aggregatedStatus = computeAggregatedAiStatus(updatedProfile, completedTestIds)

            val userUpdate = mutableMapOf<String, Any>(
                submission.definition.profileField to submission.score.toDouble(),
                "latestAiStatus" to aggregatedStatus
            )
            lastTestAtFieldNameFor(submission.definition.testId)?.let { fieldName ->
                userUpdate[fieldName] = now
            }
            userRef.set(userUpdate, SetOptions.merge()).await()

            FirestoreResult.GenericSuccess
        } catch (e: Exception) {
            FirestoreResult.Failure(e.localizedMessage ?: "Ошибка сохранения результата теста")
        }
    }

    // ── Extension functions ───────────────────────────────────────────────────

    private fun DocumentSnapshot.toOrganization() = Organization(
        id = getString("id") ?: this.id,
        name = getString("name") ?: "",
        directorId = getString("directorId") ?: "",
        inviteCodeStudent = getString("inviteCodeStudent") ?: "",
        inviteCodePsych = getString("inviteCodePsych") ?: ""
    )

    private fun DocumentSnapshot.toUserProfile() = UserProfile(
        uid = getString("uid") ?: this.id,
        fullName = getString("fullName") ?: "",
        email = getString("email") ?: "",
        role = getString("role") ?: "",
        orgId = getString("orgId") ?: "",
        ageGroup = getString("ageGroup") ?: "",
        latestAiStatus = getString("latestAiStatus") ?: "unknown",
        stressScore = (getDouble("stressScore") ?: 0.0).toFloat(),
        courseProgressPercent = (getDouble("courseProgressPercent") ?: 0.0).toFloat(),
        burnoutScore = (getDouble("burnoutScore") ?: 0.0).toFloat(),
        emotionScore = (getDouble("emotionScore") ?: 50.0).toFloat(),
        motivationScore = (getDouble("motivationScore") ?: 50.0).toFloat(),
        anxietyScore = (getDouble("anxietyScore") ?: 0.0).toFloat(),
        isBlocked = getBoolean("isBlocked") ?: false,
        psychComment = getString("psychComment") ?: "",
        assignedCourseId = getString("assignedCourseId") ?: "",
        assignedCourseName = getString("assignedCourseName") ?: "",
        psychPriority = getString("psychPriority") ?: "",
        psychCommentDate = getLong("psychCommentDate") ?: 0L
    )

    private fun DocumentSnapshot.toOrganizationCourse() = OrganizationCourse(
        id            = getString("id") ?: this.id,
        orgId         = getString("orgId") ?: "",
        title         = getString("title") ?: "",
        description   = getString("description") ?: "",
        type          = try { CourseContentType.valueOf(getString("type") ?: "TEXT") } catch (_: Exception) { CourseContentType.TEXT },
        contentText   = getString("contentText") ?: "",
        videoUrl      = getString("videoUrl") ?: "",
        createdBy     = getString("createdBy") ?: "",
        createdByName = getString("createdByName") ?: "",
        createdAt     = getLong("createdAt") ?: 0L,
        updatedAt     = getLong("updatedAt") ?: 0L,
        isPublished   = getBoolean("isPublished") ?: true
    )
}
