package com.example.aiphysical.data.model

data class ChatMessage(
    val role: String,        // "user" or "model"
    val text: String,
    val isError: Boolean = false
)

