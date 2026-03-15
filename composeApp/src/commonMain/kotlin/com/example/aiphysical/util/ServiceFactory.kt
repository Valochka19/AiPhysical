package com.example.aiphysical.util

import com.example.aiphysical.data.service.FirebaseAuthService
import com.example.aiphysical.data.service.FirestoreService

expect fun createFirebaseAuthService(): FirebaseAuthService
expect fun createFirestoreService(): FirestoreService

