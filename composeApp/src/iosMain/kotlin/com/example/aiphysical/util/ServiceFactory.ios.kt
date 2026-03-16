package com.example.aiphysical.util

import com.example.aiphysical.data.service.FirebaseAuthService
import com.example.aiphysical.data.service.FirebaseAuthServiceStubImpl
import com.example.aiphysical.data.service.FirestoreService
import com.example.aiphysical.data.service.FirestoreServiceStubImpl
import com.example.aiphysical.data.service.GeminiService
import com.example.aiphysical.data.service.GeminiServiceStubImpl

actual fun createFirebaseAuthService(): FirebaseAuthService = FirebaseAuthServiceStubImpl()
actual fun createFirestoreService(): FirestoreService = FirestoreServiceStubImpl()
actual fun createGeminiService(): GeminiService = GeminiServiceStubImpl()

