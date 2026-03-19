package com.example.aiphysical.data.model

data class OrganizationCustomTest(
    val id: String = "",
    val orgId: String = "",
    val title: String = "",
    val description: String = "",
    val questions: List<OrganizationCustomTestQuestion> = emptyList(),
    val createdBy: String = "",
    val createdByName: String = "",
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val isPublished: Boolean = true
)

data class OrganizationCustomTestQuestion(
    val id: String = "",
    val order: Int = 0,
    val text: String = "",
    val options: List<OrganizationCustomTestOption> = emptyList()
)

data class OrganizationCustomTestOption(
    val id: String = "",
    val order: Int = 0,
    val text: String = ""
)

data class OrganizationCustomTestSubmission(
    val id: String = "",
    val orgId: String = "",
    val testId: String = "",
    val testTitle: String = "",
    val studentId: String = "",
    val studentName: String = "",
    val submittedAt: Long = 0L,
    val answers: List<OrganizationCustomTestAnswer> = emptyList()
)

data class OrganizationCustomTestAnswer(
    val questionId: String = "",
    val questionText: String = "",
    val selectedOptionId: String = "",
    val selectedOptionText: String = "",
    val order: Int = 0
)

data class OrganizationCustomTestSessionState(
    val test: OrganizationCustomTest,
    val currentQuestionIndex: Int = 0,
    val selectedOptionId: String? = null,
    val answers: List<OrganizationCustomTestAnswer> = emptyList(),
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
) {
    val currentQuestion: OrganizationCustomTestQuestion?
        get() = test.questions.getOrNull(currentQuestionIndex)

    val isLastQuestion: Boolean
        get() = currentQuestionIndex >= test.questions.lastIndex
}
