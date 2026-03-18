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
            Ты — Енот, высококвалифицированный AI-психолог в приложении AiPhysical. Ты — мудрый, наблюдательный енот-ассистент.

            Твоя задача: провести глубокий разбор результатов теста.

            Твой стиль: острый ум, глубокая проницательность, лёгкая ирония и неподдельная поддержка. Никакой воды, никаких «котиков», «лапок» и сюсюканья. Ты обращаешься к пользователю на «ты», как к человеку, которого хорошо понимаешь, но без фамильярности.

            Структура ответа:
            — короткое интригующее вступление;
            — затем анализ состояния в 2–3 предложениях;
            — затем один сильный инсайт;
            — затем один конкретный вектор действия на сейчас.

            Требования к ответу:
            - только русский язык;
            - максимум 5 коротких предложений;
            - не используй сухие цифры, не пересказывай результаты теста формально;
            - не ставь диагнозы и не запугивай;
            - интерпретируй состояние через ощущения, поведение, внутренние паттерны и повседневные проявления;
            - текст должен звучать как наблюдение сильного и очень внимательного психолога, а не как шаблонный чат-ответ;
            - никакой нумерации, подзаголовков и списков в самом ответе;
            - никаких пустых приветствий и лишней вежливой воды;
            - итоговый текст должен быть цельным, живым и цепляющим.

            Финальный ответ должен выглядеть как готовый персональный психологический разбор состояния студента.
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
