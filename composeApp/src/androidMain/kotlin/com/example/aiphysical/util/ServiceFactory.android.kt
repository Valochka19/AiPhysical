package com.example.aiphysical.util

import com.example.aiphysical.data.service.FirebaseAuthService
import com.example.aiphysical.data.service.FirebaseAuthServiceImpl
import com.example.aiphysical.data.service.FirestoreService
import com.example.aiphysical.data.service.FirestoreServiceImpl
import com.example.aiphysical.data.service.GeminiService
import com.example.aiphysical.data.service.GeminiServiceImpl

actual fun createFirebaseAuthService(): FirebaseAuthService = FirebaseAuthServiceImpl()
actual fun createFirestoreService(): FirestoreService = FirestoreServiceImpl()
actual fun createGeminiService(): GeminiService = GeminiServiceImpl()

