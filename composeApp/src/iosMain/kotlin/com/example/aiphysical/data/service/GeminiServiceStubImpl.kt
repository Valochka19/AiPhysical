package com.example.aiphysical.data.service

import com.example.aiphysical.data.model.ChatMessage
import com.example.aiphysical.presentation.auth.AppLanguage
import com.example.aiphysical.presentation.auth.pick

class GeminiServiceStubImpl : GeminiService {
    override suspend fun sendMessage(
        history: List<ChatMessage>,
        systemInstruction: String?,
        language: AppLanguage
    ): Result<String> =
        Result.failure(
            Exception(
                language.pick(
                    ru = "Gemini не поддерживается на iOS",
                    en = "Gemini is not supported on iOS",
                    kz = "Gemini iOS жүйесінде қолдау таппайды"
                )
            )
        )
}

