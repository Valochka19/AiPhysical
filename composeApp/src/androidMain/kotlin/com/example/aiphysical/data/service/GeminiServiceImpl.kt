package com.example.aiphysical.data.service

import com.example.aiphysical.BuildConfig
import com.example.aiphysical.data.model.ChatMessage
import com.example.aiphysical.presentation.auth.AppLanguage
import com.example.aiphysical.presentation.auth.pick
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class GeminiServiceImpl : GeminiService {

    companion object {
        private const val BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent"
        private val TEST_ANALYSIS_MARKERS = listOf(
            "Ты анализируешь результат мини-теста студента в приложении AiPhysical.",
            "You are analyzing a student's mini-test result in AiPhysical.",
            "Сен AiPhysical қолданбасындағы студенттің шағын тест нәтижесін талдап отырсың."
        )
    }

    override suspend fun sendMessage(
        history: List<ChatMessage>,
        systemInstruction: String?,
        language: AppLanguage
    ): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isBlank()) {
                    return@withContext Result.failure(
                        Exception(
                            language.pick(
                                ru = "Gemini API key не настроен. Добавь geminiApiKey в local.properties.",
                                en = "Gemini API key is not configured. Add geminiApiKey to local.properties.",
                                kz = "Gemini API key бапталмаған. local.properties файлына geminiApiKey қосыңыз."
                            )
                        )
                    )
                }

                val url        = URL("$BASE_URL?key=$apiKey")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.connectTimeout = 30_000
                connection.readTimeout   = 60_000
                connection.doOutput      = true

                // Build request JSON — same schema as Google AI Studio
                val contentsArray = JSONArray()
                history.forEach { msg ->
                    val partObj    = JSONObject().put("text", msg.text)
                    val partsArray = JSONArray().put(partObj)
                    val contentObj = JSONObject()
                        .put("role", msg.role)
                        .put("parts", partsArray)
                    contentsArray.put(contentObj)
                }
                val bodyJson = JSONObject().put("contents", contentsArray)
                val isTestAnalysis = history.firstOrNull()?.text?.let(::isTestAnalysisPrompt) == true
                val contextualInstruction = when {
                    !systemInstruction.isNullOrBlank() -> systemInstruction
                    isTestAnalysis -> testSystemPrompt(language)
                    else -> null
                }
                val resolvedSystemInstruction = buildString {
                    append(defaultSystemPrompt(language))
                    append("\n\n")
                    append(languageInstruction(language))
                    if (!contextualInstruction.isNullOrBlank()) {
                        append("\n\n")
                        append(contextualInstruction)
                    }
                }
                if (resolvedSystemInstruction.isNotBlank()) {
                    bodyJson.put(
                        "systemInstruction",
                        JSONObject().put(
                            "parts",
                            JSONArray().put(JSONObject().put("text", resolvedSystemInstruction))
                        )
                    )
                }
                bodyJson.put(
                    "safetySettings",
                    JSONArray()
                        .put(
                            JSONObject()
                                .put("category", "HARM_CATEGORY_HARASSMENT")
                                .put("threshold", "BLOCK_ONLY_HIGH")
                        )
                        .put(
                            JSONObject()
                                .put("category", "HARM_CATEGORY_HATE_SPEECH")
                                .put("threshold", "BLOCK_ONLY_HIGH")
                        )
                )
                bodyJson.put(
                    "generationConfig",
                    JSONObject()
                        .put("maxOutputTokens", 300)
                        .put("temperature", 0.6)
                        .put(
                            "thinkingConfig",
                            JSONObject().put("thinkingBudget", 0)
                        )
                )
                val body = bodyJson.toString()

                connection.outputStream.use { os ->
                    os.write(body.toByteArray(Charsets.UTF_8))
                }

                val responseCode = connection.responseCode
                val responseText = if (responseCode == 200) {
                    connection.inputStream.bufferedReader(Charsets.UTF_8).readText()
                } else {
                    connection.errorStream?.bufferedReader(Charsets.UTF_8)?.readText()
                        ?: "Unknown error"
                }

                if (responseCode != 200) {
                    return@withContext Result.failure(
                        Exception(
                            language.pick(
                                ru = "Ошибка API ($responseCode): $responseText",
                                en = "API error ($responseCode): $responseText",
                                kz = "API қатесі ($responseCode): $responseText"
                            )
                        )
                    )
                }

                // Parse response
                val json       = JSONObject(responseText)
                val candidates = json.getJSONArray("candidates")
                if (candidates.length() == 0) {
                    return@withContext Result.failure(Exception(language.pick("Модель не вернула ответ", "The model returned no response", "Модель жауап қайтармады")))
                }
                val candidate = candidates.getJSONObject(0)
                val finishReason = candidate.optString("finishReason")
                val content = candidate.optJSONObject("content")
                    ?: return@withContext Result.failure(Exception(language.pick("Модель вернула пустой ответ", "The model returned an empty response", "Модель бос жауап қайтарды")))
                val parts = content.optJSONArray("parts")
                    ?: return@withContext Result.failure(Exception(language.pick("Модель вернула ответ без текста", "The model returned a response without text", "Модель мәтінсіз жауап қайтарды")))
                val text = buildString {
                    for (index in 0 until parts.length()) {
                        val partText = parts.optJSONObject(index)?.optString("text").orEmpty()
                        if (partText.isNotBlank()) {
                            if (isNotEmpty()) append('\n')
                            append(partText)
                        }
                    }
                }.trim()

                if (text.isBlank()) {
                    return@withContext Result.failure(Exception(language.pick("Модель не вернула текстовый ответ", "The model returned no text answer", "Модель мәтіндік жауап қайтармады")))
                }

                if (finishReason == "SAFETY") {
                    return@withContext Result.failure(Exception(language.pick("Ответ был остановлен настройками безопасности", "The response was stopped by safety settings", "Жауап қауіпсіздік баптаулары арқылы тоқтатылды")))
                }

                Result.success(text)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun defaultSystemPrompt(language: AppLanguage): String = language.pick(
        ru = "Ты — Уми, эмпатичный и теплый ИИ-помощник. Твоя миссия — анализировать психологические тесты и поддерживать пользователей. Твой тон: уютный, неформальный, поддерживающий. Ты не врач, но ты рядом.",
        en = "You are Umi, an empathetic and warm AI assistant. Your mission is to analyze psychological tests and support users. Your tone is cozy, informal, and supportive. You are not a doctor, but you stay close.",
        kz = "Сен — Уми, эмпатиясы жоғары әрі жылы AI-көмекшісің. Сенің миссияң — психологиялық тесттерді талдау және пайдаланушыларға қолдау көрсету. Тон: жайлы, бейресми, қолдаушы. Сен дәрігер емессің, бірақ жанындасың."
    )

    private fun languageInstruction(language: AppLanguage): String = language.pick(
        ru = "Отвечай только на русском языке. Названия разделов приложения пиши на русском.",
        en = "Reply only in English. Use English names for app sections.",
        kz = "Тек қазақ тілінде жауап бер. Қолданба бөлімдерінің атауын қазақша қолдан."
    )

    private fun testSystemPrompt(language: AppLanguage): String = language.pick(
        ru = """
            Ты — Уми, внимательный AI-помощник в приложении AiPhysical.
            Дай студенту короткий, тёплый и понятный разбор состояния по результатам теста.
            Пиши спокойно, человечно, точно и поддерживающе.
            Формат: ровно 3 коротких абзаца, без markdown, без списков, максимум 5 коротких предложений суммарно.
            Сначала коротко обозначь состояние, потом объясни, как оно может проявляться, и в конце дай один конкретный шаг на ближайшее время.
            Без диагнозов, запугивания и сухих цифр.
        """.trimIndent(),
        en = """
            You are Umi, a careful AI helper in AiPhysical.
            Give the student a short, warm, and clear reflection on their current state based on the test result.
            Write calmly, humanly, precisely, and supportively.
            Format: exactly 3 short paragraphs, no markdown, no lists, maximum 5 short sentences in total.
            First describe the overall state, then explain how it may show up in feelings, thoughts, or behavior, and finish with one specific next step.
            No diagnoses, no fear-based language, and no dry statistics.
        """.trimIndent(),
        kz = """
            Сен — AiPhysical қолданбасындағы мұқият AI-көмекші Умисің.
            Тест нәтижесіне сүйеніп, студентке оның күйі туралы қысқа, жылы және түсінікті талдау бер.
            Сабырлы, адами, нақты және қолдаушы түрде жаз.
            Формат: дәл 3 қысқа абзац, markdown жоқ, тізім жоқ, жалпы 5 қысқа сөйлемнен аспасын.
            Алдымен жалпы күйді айт, кейін оның сезімде, ойда не мінезде қалай көрінуі мүмкін екенін түсіндір, соңында бір нақты келесі қадам ұсын.
            Диагноз қойма, қорқытпа және құрғақ статистика қолданба.
        """.trimIndent()
    )

    private fun isTestAnalysisPrompt(text: String): Boolean =
        TEST_ANALYSIS_MARKERS.any { marker -> text.contains(marker) }
}
