package com.example.aiphysical.data.model

// ── Answer options (5 variants with weight 0..4) ──────────────────────────────
enum class AnswerType(val label: String, val weight: Int) {
    EXACTLY_ME(   "Да, это точно про меня",          4),
    SIMILAR(      "Схоже со мной, но не настолько",  3),
    NEUTRAL(      "Я нахожусь между таким состоянием", 2),
    PROBABLY_NOT( "Скорее всего — нет",               1),
    NOT_ME(       "Нет, это точно не про меня",       0)
}

// ── Cat emotional states ───────────────────────────────────────────────────────
enum class CatState {
    IDLE, HAPPY, NERVOUS, PROUD, TIRED, ENERGETIC,
    STRESS, PEACEFUL, OVERWHELMED, LAUGHING, IN_BOX
}

// ── Single question ────────────────────────────────────────────────────────────
data class BurnoutQuestion(
    val id: Int,
    val text: String,
    val catEmotion: CatState
)

// ── Single recorded answer ─────────────────────────────────────────────────────
data class BurnoutAnswer(
    val questionId: Int,
    val questionText: String,
    val catEmotion: CatState,
    val answerType: AnswerType
)

// ── Screen lifecycle step ──────────────────────────────────────────────────────
sealed class BurnoutTestStep {
    object Questions    : BurnoutTestStep()
    object LoadingResult: BurnoutTestStep()
    data class Result(
        val feedbackText: String,
        val score: Int,            // 0..100 wellness score
        val aiAssessment: String   // "normal" | "stress" | "critical"
    ) : BurnoutTestStep()
}

// ── Whole test UI state ────────────────────────────────────────────────────────
data class BurnoutTestUiState(
    val step: BurnoutTestStep = BurnoutTestStep.Questions,
    val currentQuestionIndex: Int = 0,
    val answers: List<BurnoutAnswer> = emptyList(),
    /** guard against double-tap while animating */
    val isAnswering: Boolean = false,
    val errorMessage: String? = null
)

// ── Scoring logic ──────────────────────────────────────────────────────────────
object BurnoutScoring {
    /** Score >= this → "normal" */
    const val NORMAL_THRESHOLD = 60
    /** Score >= this (but < NORMAL_THRESHOLD) → "stress" */
    const val STRESS_THRESHOLD = 35
    // score < STRESS_THRESHOLD → "critical"

    /**
     * Positive-framing questions (high weight = NOT burnt out):
     * Q1 (Утром бодрый), Q3 (учёба приносит пользу),
     * Q5 (хватает сил), Q7 (легко компромиссы), Q9 (улыбаюсь/смеюсь).
     *
     * Negative-framing questions (high weight = MORE burnt out):
     * Q2, Q4, Q6, Q8, Q10.
     *
     * Returns a wellness score 0..100 (higher = healthier).
     */
    fun computeScore(answers: List<BurnoutAnswer>): Int {
        if (answers.isEmpty()) return 0
        val positiveIds = setOf(1, 3, 5, 7, 9)
        var totalWeight = 0
        for (answer in answers) {
            totalWeight += if (answer.questionId in positiveIds) {
                4 - answer.answerType.weight  // reverse: 0→4, 4→0
            } else {
                answer.answerType.weight      // direct: higher = more burnout
            }
        }
        // totalWeight range: 0 (perfect) .. 40 (maximum burnout)
        val maxWeight = answers.size * 4
        val wellnessScore = ((maxWeight - totalWeight) * 100f / maxWeight).toInt()
        return wellnessScore.coerceIn(0, 100)
    }

    fun computeAssessment(score: Int): String = when {
        score >= NORMAL_THRESHOLD -> "normal"
        score >= STRESS_THRESHOLD -> "stress"
        else                      -> "critical"
    }
}

// ── Built-in question bank ─────────────────────────────────────────────────────
val BURNOUT_QUESTIONS: List<BurnoutQuestion> = listOf(
    BurnoutQuestion(1,  "Утром я чувствую себя бодрым и готовым к задачам",          CatState.HAPPY),
    BurnoutQuestion(2,  "Меня начали бесить люди, которые раньше не раздражали",      CatState.NERVOUS),
    BurnoutQuestion(3,  "Я чувствую, что моя учёба приносит реальную пользу",         CatState.PROUD),
    BurnoutQuestion(4,  "В конце дня я чувствую себя как пустой стакан",              CatState.TIRED),
    BurnoutQuestion(5,  "У меня хватает сил на хобби и друзей после учёбы",           CatState.ENERGETIC),
    BurnoutQuestion(6,  "Мысли о делах не дают мне расслабиться на отдыхе",           CatState.STRESS),
    BurnoutQuestion(7,  "Я легко нахожу компромиссы и не злюсь на людей",             CatState.PEACEFUL),
    BurnoutQuestion(8,  "Любая мелкая задача кажется мне огромной горой",             CatState.OVERWHELMED),
    BurnoutQuestion(9,  "Я часто улыбаюсь или смеюсь при общении с друзьями",        CatState.LAUGHING),
    BurnoutQuestion(10, "Я хочу, чтобы меня просто все оставили в покое",            CatState.IN_BOX)
)

