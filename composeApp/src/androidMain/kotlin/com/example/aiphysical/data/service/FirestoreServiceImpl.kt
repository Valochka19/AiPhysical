package com.example.aiphysical.data.service

import com.example.aiphysical.data.model.*
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreServiceImpl : FirestoreService {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private companion object {
        const val TEST_COMPLETION_POINTS = 20
    }

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
                "pointsTotal" to profile.pointsTotal,
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

    override suspend fun getUserPointsLedger(uid: String): FirestoreResult {
        return try {
            val snap = db.collection("users").document(uid)
                .collection("pointsLedger")
                .orderBy("awardedAt", Query.Direction.DESCENDING)
                .get()
                .await()
            FirestoreResult.PointsLedgerSuccess(snap.documents.map { it.toPointsLedgerEntry() })
        } catch (e: Exception) {
            FirestoreResult.Failure(e.localizedMessage ?: "Ошибка загрузки истории баллов")
        }
    }

    override suspend fun getPsychChatContacts(orgId: String, currentUid: String, currentRole: String): FirestoreResult {
        return try {
            val allowedRoles = when (currentRole.normalizedPsychChatRole()) {
                "psychologist" -> setOf("user", "teacher")
                "user", "teacher" -> setOf("psychologist")
                else -> emptySet()
            }
            if (allowedRoles.isEmpty()) return FirestoreResult.ChatContactsSuccess(emptyList())

            val snap = db.collection("users")
                .whereEqualTo("orgId", orgId)
                .get()
                .await()

            val contacts = snap.documents
                .map { it.toUserProfile() }
                .filter { profile ->
                    profile.uid != currentUid &&
                        !profile.isBlocked &&
                        profile.role.normalizedPsychChatRole() in allowedRoles
                }
                .sortedBy { it.fullName.lowercase() }

            FirestoreResult.ChatContactsSuccess(contacts)
        } catch (e: Exception) {
            FirestoreResult.Failure(e.localizedMessage ?: "Ошибка загрузки контактов чата")
        }
    }

    override fun observePsychChatThreads(orgId: String, currentUid: String): Flow<FirestoreResult> = callbackFlow {
        val listenerRegistration = db.collection("psychChats")
            .whereArrayContains("participantIds", currentUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(FirestoreResult.Failure(error.localizedMessage ?: "Ошибка слушателя чатов"))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val threads = snapshot.documents
                        .map { it.toPsychChatThread() }
                        .filter { it.orgId == orgId }
                        .sortedByDescending { it.updatedAt }
                    trySend(FirestoreResult.PsychChatThreadsSuccess(threads))
                }
            }

        awaitClose { listenerRegistration.remove() }
    }

    override fun observePsychChatMessages(chatId: String): Flow<FirestoreResult> = callbackFlow {
        val listenerRegistration = db.collection("psychChats")
            .document(chatId)
            .collection("messages")
            .orderBy("createdAt")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(FirestoreResult.Failure(error.localizedMessage ?: "Ошибка слушателя сообщений"))
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val messages = snapshot.documents
                        .map { it.toPsychChatMessage() }
                        .sortedBy { it.createdAt }
                    trySend(FirestoreResult.PsychChatMessagesSuccess(messages))
                }
            }

        awaitClose { listenerRegistration.remove() }
    }

    override suspend fun sendPsychChatMessage(
        orgId: String,
        senderId: String,
        senderRole: String,
        recipientId: String,
        recipientRole: String,
        text: String
    ): FirestoreResult {
        return try {
            if (!isAllowedPsychChatPair(senderRole, recipientRole)) {
                return FirestoreResult.Failure("Недопустимая пара ролей для личного чата")
            }

            val safeText = text.trimmedSupportMessage()
            if (safeText.isBlank()) {
                return FirestoreResult.Failure("Нельзя отправить пустое сообщение")
            }

            val chatId = buildPsychChatId(orgId, senderId, recipientId)
            val participants = listOf(
                senderId.normalizedPsychChatRoleAwarePair(senderRole),
                recipientId.normalizedPsychChatRoleAwarePair(recipientRole)
            ).sortedBy { it.first }
            val now = System.currentTimeMillis()
            val chatRef = db.collection("psychChats").document(chatId)
            val messageRef = chatRef.collection("messages").document()
            val batch = db.batch()

            batch.set(
                chatRef,
                mapOf(
                    "chatId" to chatId,
                    "orgId" to orgId,
                    "participantIds" to participants.map { it.first },
                    "participantRoles" to participants.map { it.second },
                    "lastMessageText" to safeText,
                    "lastMessageAt" to now,
                    "createdAt" to now,
                    "updatedAt" to now,
                ),
                SetOptions.merge()
            )
            batch.set(
                messageRef,
                mapOf(
                    "messageId" to messageRef.id,
                    "chatId" to chatId,
                    "senderId" to senderId,
                    "senderRole" to senderRole.normalizedPsychChatRole(),
                    "text" to safeText,
                    "createdAt" to now,
                )
            )
            batch.commit().await()
            FirestoreResult.GenericSuccess
        } catch (e: Exception) {
            FirestoreResult.Failure(e.localizedMessage ?: "Ошибка отправки сообщения")
        }
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

    override fun observeOrganizationCustomTests(orgId: String): Flow<FirestoreResult> = callbackFlow {
        val ref = db.collection("organizations")
            .document(orgId)
            .collection("customTests")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
        val listener = ref.addSnapshotListener { snapshot, error ->
            if (error != null) {
                trySend(FirestoreResult.Failure(error.localizedMessage ?: "Ошибка слушателя тестов"))
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val tests = snapshot.documents
                    .map { it.toOrganizationCustomTest() }
                    .sortedByDescending { it.updatedAt }
                trySend(FirestoreResult.OrganizationCustomTestsSuccess(tests))
            }
        }
        awaitClose { listener.remove() }
    }

    override suspend fun getOrganizationCustomTests(orgId: String): FirestoreResult {
        return try {
            val snap = db.collection("organizations")
                .document(orgId)
                .collection("customTests")
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .get()
                .await()
            FirestoreResult.OrganizationCustomTestsSuccess(
                snap.documents.map { it.toOrganizationCustomTest() }.sortedByDescending { it.updatedAt }
            )
        } catch (e: Exception) {
            FirestoreResult.Failure(e.localizedMessage ?: "Ошибка загрузки тестов")
        }
    }

    override suspend fun createOrganizationCustomTest(orgId: String, test: OrganizationCustomTest): FirestoreResult {
        return try {
            val ref = if (test.id.isBlank()) {
                db.collection("organizations").document(orgId).collection("customTests").document()
            } else {
                db.collection("organizations").document(orgId).collection("customTests").document(test.id)
            }
            ref.set(test.copy(id = ref.id, orgId = orgId).toOrganizationCustomTestMap()).await()
            FirestoreResult.GenericSuccess
        } catch (e: Exception) {
            FirestoreResult.Failure(e.localizedMessage ?: "Ошибка публикации теста")
        }
    }

    override suspend fun submitOrganizationCustomTest(
        orgId: String,
        testId: String,
        submission: OrganizationCustomTestSubmission
    ): FirestoreResult {
        return try {
            val ref = if (submission.id.isBlank()) {
                db.collection("organizations").document(orgId)
                    .collection("customTests").document(testId)
                    .collection("submissions").document()
            } else {
                db.collection("organizations").document(orgId)
                    .collection("customTests").document(testId)
                    .collection("submissions").document(submission.id)
            }
            ref.set(submission.copy(id = ref.id, orgId = orgId, testId = testId).toOrganizationCustomTestSubmissionMap()).await()
            try {
                awardPointsIfNeeded(
                    uid = submission.studentId,
                    eventKey = "custom_test_completed_$testId",
                    title = "Кастомный тест пройден",
                    description = submission.testTitle.ifBlank { "Тест организации" },
                    points = TEST_COMPLETION_POINTS,
                    sourceType = "custom_test",
                    sourceId = testId
                )
            } catch (_: Exception) {
                // Никогда не ломаем отправку результатов теста из-за баллов
            }
            FirestoreResult.GenericSuccess
        } catch (e: Exception) {
            FirestoreResult.Failure(e.localizedMessage ?: "Ошибка отправки ответов по тесту")
        }
    }

    override suspend fun upsertBaseCourseProgress(uid: String, course: BaseCourseCatalogItem): FirestoreResult {
        return try {
            val now = System.currentTimeMillis()
            val userRef = db.collection("users").document(uid)
            val progressRef = userRef.collection("courseProgress").document(course.id)

            progressRef.set(
                mapOf(
                    "courseId" to course.id,
                    "courseName" to course.title,
                    "progress" to 1.0,
                    "lastAccessMillis" to now
                ),
                SetOptions.merge()
            ).await()

            val progressSnapshot = userRef.collection("courseProgress").get().await()
            val progressByCourseId = progressSnapshot.documents.associate { doc ->
                doc.id to (doc.getDouble("progress") ?: 0.0).toFloat()
            }
            val courseProgressPercent = AppCourseCatalog.computeBaseCourseProgressPercent(progressByCourseId)
            userRef.set(
                mapOf("courseProgressPercent" to courseProgressPercent.toDouble()),
                SetOptions.merge()
            ).await()

            FirestoreResult.GenericSuccess
        } catch (e: Exception) {
            FirestoreResult.Failure(e.localizedMessage ?: "Ошибка обновления прогресса курса")
        }
    }

    override suspend fun getOrganizationBaseCourseCompletionStats(orgId: String): FirestoreResult {
        return try {
            val detailsByCourseId = computeOrganizationBaseCourseCompletionDetails(orgId)
            val stats = AppCourseCatalog.baseCourses.map { course ->
                val details = detailsByCourseId[course.id] ?: BaseCourseCompletionDetails(
                    courseId = course.id,
                    courseName = course.title
                )
                BaseCourseCompletionStats(
                    courseId = course.id,
                    courseName = course.title,
                    completedCount = details.completedMembers.size,
                    inProgressCount = details.inProgressMembers.size,
                    notStartedCount = details.notStartedMembers.size
                )
            }
            FirestoreResult.BaseCourseCompletionStatsSuccess(stats)
        } catch (e: Exception) {
            FirestoreResult.Failure(e.localizedMessage ?: "Ошибка загрузки статистики курсов")
        }
    }

    override suspend fun getOrganizationBaseCourseCompletionDetails(orgId: String, courseId: String): FirestoreResult {
        return try {
            val course = AppCourseCatalog.baseCourseById(courseId)
                ?: return FirestoreResult.Failure("Базовый курс не найден")
            val details = computeOrganizationBaseCourseCompletionDetails(orgId)[courseId]
                ?: BaseCourseCompletionDetails(courseId = course.id, courseName = course.title)
            FirestoreResult.BaseCourseCompletionDetailsSuccess(details)
        } catch (e: Exception) {
            FirestoreResult.Failure(e.localizedMessage ?: "Ошибка загрузки статуса прохождения курса")
        }
    }

    override suspend fun getOrganizationTestStats(orgId: String): FirestoreResult {
        return try {
            val students = loadOrganizationStudents(orgId)
            val attemptsByTestId = mutableMapOf<String, MutableList<Pair<String, String>>>()

            students.forEach { student ->
                val testSnapshot = db.collection("users").document(student.uid)
                    .collection("testResults")
                    .get()
                    .await()

                testSnapshot.documents.forEach { doc ->
                    val testId = doc.getString("testId") ?: return@forEach
                    val testName = doc.getString("testName") ?: testId
                    val assessment = doc.getString("aiAssessment") ?: "unknown"
                    attemptsByTestId.getOrPut(testId) { mutableListOf() }
                        .add(testName to assessment)
                }
            }

            val stats = com.example.aiphysical.presentation.student.StudentTestType.entries.map { type ->
                val attempts = attemptsByTestId[type.testId].orEmpty()
                val dominantAssessment = attempts
                    .groupingBy { it.second }
                    .eachCount()
                    .maxWithOrNull(compareBy<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                    ?.key
                    ?: "unknown"

                OrganizationTestStats(
                    testType = type,
                    testId = type.testId,
                    testName = studentTestDefinitionFor(type).testName,
                    totalAttempts = attempts.size,
                    mostFrequentAssessment = dominantAssessment
                )
            }

            FirestoreResult.OrganizationTestStatsSuccess(stats)
        } catch (e: Exception) {
            FirestoreResult.Failure(e.localizedMessage ?: "Ошибка загрузки статистики тестов")
        }
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

            try {
                awardPointsIfNeeded(
                    uid = uid,
                    eventKey = "builtin_test_completed_${submission.definition.testId}",
                    title = "Платформенный тест пройден",
                    description = submission.definition.testName,
                    points = TEST_COMPLETION_POINTS,
                    sourceType = "builtin_test",
                    sourceId = submission.definition.testId
                )
            } catch (_: Exception) {
                // Не позволяем геймификации ломать test flow
            }

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
        pointsTotal = ((getDouble("pointsTotal") ?: getLong("pointsTotal")?.toDouble() ?: 0.0)).toInt(),
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

    private fun DocumentSnapshot.toOrganizationCustomTest() = OrganizationCustomTest(
        id = getString("id") ?: this.id,
        orgId = getString("orgId") ?: "",
        title = getString("title") ?: "",
        description = getString("description") ?: "",
        questions = getNestedList("questions").mapIndexedNotNull { index, raw ->
            raw.toOrganizationCustomTestQuestion(index)
        }.sortedBy { it.order },
        createdBy = getString("createdBy") ?: "",
        createdByName = getString("createdByName") ?: "",
        createdAt = getLong("createdAt") ?: 0L,
        updatedAt = getLong("updatedAt") ?: 0L,
        isPublished = getBoolean("isPublished") ?: true
    )

    private fun DocumentSnapshot.toPsychChatThread() = PsychChatThread(
        chatId = getString("chatId") ?: this.id,
        orgId = getString("orgId") ?: "",
        participantIds = (get("participantIds") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
        participantRoles = (get("participantRoles") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
        lastMessageText = getString("lastMessageText") ?: "",
        lastMessageAt = getLong("lastMessageAt") ?: 0L,
        createdAt = getLong("createdAt") ?: 0L,
        updatedAt = getLong("updatedAt") ?: 0L,
    )

    private fun DocumentSnapshot.toPsychChatMessage() = PsychChatMessage(
        messageId = getString("messageId") ?: this.id,
        chatId = getString("chatId") ?: "",
        senderId = getString("senderId") ?: "",
        senderRole = getString("senderRole") ?: "",
        text = getString("text") ?: "",
        createdAt = getLong("createdAt") ?: 0L,
    )

    private fun DocumentSnapshot.toPointsLedgerEntry() = PointsLedgerEntry(
        eventKey = getString("eventKey") ?: this.id,
        title = getString("title") ?: "Начисление баллов",
        description = getString("description") ?: "",
        points = ((getDouble("points") ?: getLong("points")?.toDouble() ?: 0.0)).toInt(),
        awardedAt = getLong("awardedAt") ?: 0L,
        sourceType = getString("sourceType") ?: "",
        sourceId = getString("sourceId") ?: ""
    )

    private suspend fun loadOrganizationStudents(orgId: String): List<UserProfile> {
        val snapshot = db.collection("users")
            .whereEqualTo("orgId", orgId)
            .get()
            .await()
        return snapshot.documents
            .map { it.toUserProfile() }
            .filter { it.role == "user" }
            .sortedBy { it.fullName.lowercase() }
    }

    private suspend fun computeOrganizationBaseCourseCompletionDetails(
        orgId: String
    ): Map<String, BaseCourseCompletionDetails> {
        val students = loadOrganizationStudents(orgId)
        val detailsMap = AppCourseCatalog.baseCourses.associate { course ->
            course.id to BaseCourseCompletionDetails(
                courseId = course.id,
                courseName = course.title
            )
        }.toMutableMap()

        students.forEach { student ->
            val progressByCourseId = db.collection("users").document(student.uid)
                .collection("courseProgress")
                .get()
                .await()
                .documents
                .associate { doc ->
                    doc.id to CourseProgress(
                        courseId = doc.getString("courseId") ?: doc.id,
                        courseName = doc.getString("courseName") ?: doc.id,
                        progress = (doc.getDouble("progress") ?: 0.0).toFloat(),
                        lastAccessMillis = doc.getLong("lastAccessMillis") ?: 0L
                    )
                }

            AppCourseCatalog.baseCourses.forEach { course ->
                val progress = progressByCourseId[course.id]
                val member = CourseCompletionMember(
                    userId = student.uid,
                    fullName = student.fullName,
                    progress = progress?.progress ?: 0f,
                    lastAccessMillis = progress?.lastAccessMillis ?: 0L
                )
                val current = detailsMap.getValue(course.id)
                detailsMap[course.id] = when {
                    progress == null || progress.progress <= 0f -> current.copy(
                        notStartedMembers = current.notStartedMembers + member
                    )

                    progress.progress >= 1f -> current.copy(
                        completedMembers = current.completedMembers + member
                    )

                    else -> current.copy(
                        inProgressMembers = current.inProgressMembers + member
                    )
                }
            }
        }

        return detailsMap.mapValues { (_, details) ->
            details.copy(
                completedMembers = details.completedMembers.sortedBy { it.fullName.lowercase() },
                inProgressMembers = details.inProgressMembers.sortedBy { it.fullName.lowercase() },
                notStartedMembers = details.notStartedMembers.sortedBy { it.fullName.lowercase() }
            )
        }
    }

    private fun String.normalizedPsychChatRoleAwarePair(role: String): Pair<String, String> =
        this to role.normalizedPsychChatRole()

    private suspend fun awardPointsIfNeeded(
        uid: String,
        eventKey: String,
        title: String,
        description: String,
        points: Int,
        sourceType: String,
        sourceId: String
    ) {
        val userRef = db.collection("users").document(uid)
        val ledgerRef = userRef.collection("pointsLedger").document(eventKey)
        val now = System.currentTimeMillis()

        db.runTransaction { transaction ->
            val existingLedger = transaction.get(ledgerRef)
            if (existingLedger.exists()) {
                return@runTransaction Unit
            }
            val userSnapshot = transaction.get(userRef)
            val currentPoints = ((userSnapshot.getDouble("pointsTotal")
                ?: userSnapshot.getLong("pointsTotal")?.toDouble()
                ?: 0.0)).toInt()

            transaction.set(
                ledgerRef,
                mapOf(
                    "eventKey" to eventKey,
                    "title" to title,
                    "description" to description,
                    "points" to points,
                    "awardedAt" to now,
                    "sourceType" to sourceType,
                    "sourceId" to sourceId
                )
            )
            transaction.set(
                userRef,
                mapOf("pointsTotal" to (currentPoints + points)),
                SetOptions.merge()
            )
            Unit
        }.await()
    }

    private fun OrganizationCustomTest.toOrganizationCustomTestMap(): Map<String, Any> = mapOf(
        "id" to id,
        "orgId" to orgId,
        "title" to title,
        "description" to description,
        "questions" to questions.sortedBy { it.order }.map { question ->
            mapOf(
                "id" to question.id,
                "order" to question.order,
                "text" to question.text,
                "options" to question.options.sortedBy { it.order }.map { option ->
                    mapOf(
                        "id" to option.id,
                        "order" to option.order,
                        "text" to option.text
                    )
                }
            )
        },
        "createdBy" to createdBy,
        "createdByName" to createdByName,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "isPublished" to isPublished
    )

    private fun OrganizationCustomTestSubmission.toOrganizationCustomTestSubmissionMap(): Map<String, Any> = mapOf(
        "id" to id,
        "orgId" to orgId,
        "testId" to testId,
        "testTitle" to testTitle,
        "studentId" to studentId,
        "studentName" to studentName,
        "submittedAt" to submittedAt,
        "answers" to answers.sortedBy { it.order }.map { answer ->
            mapOf(
                "questionId" to answer.questionId,
                "questionText" to answer.questionText,
                "selectedOptionId" to answer.selectedOptionId,
                "selectedOptionText" to answer.selectedOptionText,
                "order" to answer.order
            )
        }
    )

    private fun Any?.toOrganizationCustomTestQuestion(index: Int): OrganizationCustomTestQuestion? {
        val map = this as? Map<*, *> ?: return null
        val order = map.intValue("order") ?: (index + 1)
        return OrganizationCustomTestQuestion(
            id = map.stringValue("id").ifBlank { "q_$order" },
            order = order,
            text = map.stringValue("text"),
            options = map.listValue("options").mapIndexedNotNull { optionIndex, option ->
                option.toOrganizationCustomTestOption(optionIndex)
            }.sortedBy { it.order }
        )
    }

    private fun Any?.toOrganizationCustomTestOption(index: Int): OrganizationCustomTestOption? {
        val map = this as? Map<*, *> ?: return null
        val order = map.intValue("order") ?: (index + 1)
        return OrganizationCustomTestOption(
            id = map.stringValue("id").ifBlank { "o_$order" },
            order = order,
            text = map.stringValue("text")
        )
    }

    private fun DocumentSnapshot.getNestedList(field: String): List<Any?> =
        (get(field) as? List<*>)?.toList() ?: emptyList()

    private fun Map<*, *>.stringValue(key: String): String = this[key] as? String ?: ""

    private fun Map<*, *>.listValue(key: String): List<Any?> =
        (this[key] as? List<*>)?.toList() ?: emptyList()

    private fun Map<*, *>.intValue(key: String): Int? = when (val value = this[key]) {
        is Int -> value
        is Long -> value.toInt()
        is Double -> value.toInt()
        else -> null
    }
}
