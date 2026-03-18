package com.example.aiphysical.data.service

import com.example.aiphysical.BuildConfig
import com.example.aiphysical.data.model.ChatMessage
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
        private const val DEFAULT_SYSTEM_PROMPT =
            "Ты — Уми, эмпатичный и теплый ИИ-помощник. Твоя миссия — анализировать психологические тесты и поддерживать пользователей. Твой тон: уютный, неформальный, поддерживающий. Ты не врач, но ты рядом."
        private const val TEST_ANALYSIS_MARKER = "Ты анализируешь результат мини-теста студента в приложении AiPhysical."
        private val TEST_SYSTEM_PROMPT = """
            Ты — Енот, внимательный AI-психолог в приложении AiPhysical.

            Твоя задача — дать студенту короткий, тёплый и понятный разбор его состояния по результатам теста.

            Твой стиль:
            спокойный, человечный, точный, поддерживающий.
            Не пиши высокопарно, не используй тяжёлые метафоры, книжные сравнения, пафосные формулировки и «умничание».
            Текст должен звучать как честное и бережное наблюдение, а не как литературный монолог.

            Формат ответа:
            - ровно 3 коротких абзаца;
            - абзацы разделяй одной пустой строкой;
            - в каждом абзаце 1–2 коротких предложения;
            - без нумерации, без подзаголовков, без списков;
            - без markdown-разметки.

            Смысл абзацев:
            1) коротко и по-человечески обозначь текущее состояние;
            2) объясни, как это состояние может проявляться в ощущениях, мыслях или поведении;
            3) дай один точный вывод о человеке и один очень конкретный шаг на ближайшее время.

            Ограничения:
            - только русский язык;
            - максимум 5 коротких предложений суммарно;
            - никаких сухих цифр;
            - никаких диагнозов и запугивания;
            - не пересказывай результаты теста формально;
            - не используй фразы вроде «картина вырисовывается», «редкая точка баланса», «нервная система как швейцарские часы»;
            - не пиши как коуч, мотиватор или литературный narrator;
            - лучше проще, короче и живее, чем красивее и тяжелее.

            Финальный текст должен быть лёгким для чтения на экране телефона и ощущаться как личный, живой отклик.
            """.trimIndent()
    }

    override suspend fun sendMessage(
        history: List<ChatMessage>,
        systemInstruction: String?
    ): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey.isBlank()) {
                    return@withContext Result.failure(
                        Exception("Gemini API key не настроен. Добавь geminiApiKey в local.properties.")
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
                val isTestAnalysis = history.firstOrNull()?.text?.contains(TEST_ANALYSIS_MARKER) == true
                val contextualInstruction = when {
                    !systemInstruction.isNullOrBlank() -> systemInstruction
                    isTestAnalysis -> TEST_SYSTEM_PROMPT
                    else -> null
                }
                val resolvedSystemInstruction = buildString {
                    append(DEFAULT_SYSTEM_PROMPT)
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
                        Exception("Ошибка API ($responseCode): $responseText")
                    )
                }

                // Parse response
                val json       = JSONObject(responseText)
                val candidates = json.getJSONArray("candidates")
                if (candidates.length() == 0) {
                    return@withContext Result.failure(Exception("Модель не вернула ответ"))
                }
                val candidate = candidates.getJSONObject(0)
                val finishReason = candidate.optString("finishReason")
                val content = candidate.optJSONObject("content")
                    ?: return@withContext Result.failure(Exception("Модель вернула пустой ответ"))
                val parts = content.optJSONArray("parts")
                    ?: return@withContext Result.failure(Exception("Модель вернула ответ без текста"))
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
                    return@withContext Result.failure(Exception("Модель не вернула текстовый ответ"))
                }

                if (finishReason == "SAFETY") {
                    return@withContext Result.failure(Exception("Ответ был остановлен настройками безопасности"))
                }

                Result.success(text)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
