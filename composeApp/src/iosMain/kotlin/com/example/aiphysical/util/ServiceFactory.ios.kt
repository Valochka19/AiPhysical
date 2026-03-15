package com.example.aiphysical.util

import com.example.aiphysical.data.service.FirebaseAuthService
import com.example.aiphysical.data.service.FirebaseAuthServiceStubImpl
import com.example.aiphysical.data.service.FirestoreService
import com.example.aiphysical.data.service.FirestoreServiceStubImpl

actual fun createFirebaseAuthService(): FirebaseAuthService = FirebaseAuthServiceStubImpl()
actual fun createFirestoreService(): FirestoreService = FirestoreServiceStubImpl()

