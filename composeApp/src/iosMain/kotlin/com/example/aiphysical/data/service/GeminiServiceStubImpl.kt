package com.example.aiphysical.data.service

import com.example.aiphysical.data.model.ChatMessage

class GeminiServiceStubImpl : GeminiService {
    override suspend fun sendMessage(
        history: List<ChatMessage>,
        systemInstruction: String?
    ): Result<String> =
        Result.failure(Exception("Gemini не поддерживается на iOS"))
}

