package com.example.aiphysical.util

import com.example.aiphysical.data.service.FirebaseAuthService
import com.example.aiphysical.data.service.FirestoreService
import com.example.aiphysical.data.service.GeminiService

expect fun createFirebaseAuthService(): FirebaseAuthService
expect fun createFirestoreService(): FirestoreService
expect fun createGeminiService(): GeminiService

