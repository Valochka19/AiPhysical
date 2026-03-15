package com.example.aiphysical.data.model

data class CourseProgress(
    val courseId: String = "",
    val courseName: String = "",
    val progress: Float = 0f,        // 0.0 – 1.0
    val lastAccessMillis: Long = 0L
)

