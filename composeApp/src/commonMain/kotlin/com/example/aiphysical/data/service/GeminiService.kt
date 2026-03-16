package com.example.aiphysical.data.service

import com.example.aiphysical.data.model.ChatMessage

interface GeminiService {
    suspend fun sendMessage(history: List<ChatMessage>): Result<String>
}

