package com.example.aiphysical.data.service

import com.example.aiphysical.data.model.ChatMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class GeminiServiceImpl : GeminiService {

    companion object {
        private const val API_KEY = "AIzaSyDcg2x3u494QTBDKs5Y8TaNqTBDYkkbZgY"
        private const val BASE_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"
    }

    override suspend fun sendMessage(history: List<ChatMessage>): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val url        = URL("$BASE_URL?key=$API_KEY")
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
                val body = JSONObject().put("contents", contentsArray).toString()

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
                val content = candidates.getJSONObject(0).getJSONObject("content")
                val parts   = content.getJSONArray("parts")
                val text    = parts.getJSONObject(0).getString("text")

                Result.success(text)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}
