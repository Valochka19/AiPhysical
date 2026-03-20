package com.example.aiphysical.data.service

import com.example.aiphysical.data.model.ChatMessage
import com.example.aiphysical.presentation.auth.AppLanguage

interface GeminiService {
    suspend fun sendMessage(
        history: List<ChatMessage>,
        systemInstruction: String? = null,
        language: AppLanguage = AppLanguage.RU
    ): Result<String>
}

