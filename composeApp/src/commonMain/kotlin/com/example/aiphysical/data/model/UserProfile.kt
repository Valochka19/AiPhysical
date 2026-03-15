package com.example.aiphysical.data.model

data class UserProfile(
    val uid: String = "",
    val fullName: String = "",
    val email: String = "",
    val role: String = "",               // "director" | "psychologist" | "user"
    val orgId: String = "",
    val ageGroup: String = "",           // "JUNIOR" | "MIDDLE" | "SENIOR"
    val latestAiStatus: String = "unknown", // "normal" | "stress" | "critical" | "unknown"
    val stressScore: Float = 0f,         // 0–100
    val courseProgressPercent: Float = 0f, // 0–100
    // Extended metrics
    val burnoutScore: Float = 0f,        // 0–100
    val emotionScore: Float = 50f,       // 0–100 (higher = better mood)
    val motivationScore: Float = 50f,    // 0–100 (higher = more motivated)
    val anxietyScore: Float = 0f,        // 0–100
    val isBlocked: Boolean = false
)
