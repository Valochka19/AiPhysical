package com.example.aiphysical

import com.example.aiphysical.data.model.AnswerType
import com.example.aiphysical.data.model.StudentTestAnswer
import com.example.aiphysical.data.model.TestResult
import com.example.aiphysical.data.model.displayDescription
import com.example.aiphysical.data.model.displayLabel
import com.example.aiphysical.data.model.displayTitle
import com.example.aiphysical.data.model.studentTestDefinitionFor
import com.example.aiphysical.data.model.AppStudentTestCatalog
import com.example.aiphysical.presentation.auth.AppLanguage
import com.example.aiphysical.presentation.auth.displayAgeGroup
import com.example.aiphysical.presentation.auth.pick
import com.example.aiphysical.presentation.student.StudentTestType
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class LocalizationBehaviorTest {

    @Test
    fun app_language_pick_returns_value_for_each_language() {
        assertEquals("ru", AppLanguage.RU.pick("ru", "en", "kz"))
        assertEquals("en", AppLanguage.EN.pick("ru", "en", "kz"))
        assertEquals("kz", AppLanguage.KZ.pick("ru", "en", "kz"))
    }

    @Test
    fun student_test_catalog_titles_and_descriptions_are_localized() {
        val item = AppStudentTestCatalog.items.first { it.type == StudentTestType.BURNOUT }

        assertEquals("Burnout", item.displayTitle(AppLanguage.EN))
        assertEquals("Күйіп кету", item.displayTitle(AppLanguage.KZ))
        assertContains(item.displayDescription(AppLanguage.EN), "burnout", ignoreCase = true)
        assertContains(item.displayDescription(AppLanguage.KZ), "күйіп", ignoreCase = true)
    }

    @Test
    fun answer_labels_are_localized_for_english_and_kazakh() {
        assertEquals("Yes, this is exactly about me", AnswerType.EXACTLY_ME.displayLabel(AppLanguage.EN))
        assertEquals("Иә, бұл дәл мен туралы", AnswerType.EXACTLY_ME.displayLabel(AppLanguage.KZ))
    }

    @Test
    fun test_prompt_is_generated_in_selected_language() {
        val definition = studentTestDefinitionFor(StudentTestType.BURNOUT)
        val answers = definition.questions.map { question ->
            StudentTestAnswer(
                questionId = question.id,
                questionText = question.text,
                catEmotion = question.catEmotion,
                answerType = AnswerType.NEUTRAL,
                polarity = question.polarity
            )
        }
        val score = definition.scoreAnswers(answers)
        val assessment = definition.computeAssessment(score)

        val englishPrompt = definition.buildPrompt(answers, score, assessment, AppLanguage.EN)
        val kazakhPrompt = definition.buildPrompt(answers, score, assessment, AppLanguage.KZ)

        assertContains(englishPrompt, "You are analyzing a student's mini-test result in AiPhysical.")
        assertContains(englishPrompt, "Test name:")
        assertContains(kazakhPrompt, "Сен AiPhysical қолданбасындағы студенттің шағын тест нәтижесін талдап отырсың.")
        assertContains(kazakhPrompt, "Тест атауы:")
    }

    @Test
    fun persisted_test_results_display_titles_in_selected_language() {
        val result = TestResult(testId = "burnout", testName = "Выгорание")

        assertEquals("Burnout", result.displayTitle(AppLanguage.EN))
        assertEquals("Күйіп кету", result.displayTitle(AppLanguage.KZ))
    }

    @Test
    fun persisted_age_group_values_display_in_selected_language() {
        assertEquals("Senior High School", "SENIOR".displayAgeGroup(AppLanguage.EN))
        assertEquals("Жоғары мектеп", "SENIOR".displayAgeGroup(AppLanguage.KZ))
    }
}

