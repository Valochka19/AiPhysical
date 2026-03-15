package com.example.aiphysical.data.service

/**
 * iOS stub — Firebase iOS SDK must be added via Swift Package Manager
 * in the Xcode project (Firebase/Auth) and a proper implementation provided here.
 */
class FirebaseAuthServiceStubImpl : FirebaseAuthService {
    override suspend fun signIn(email: String, password: String): AuthResult =
        AuthResult.Failure("Firebase Auth is not configured for iOS yet. Please add Firebase iOS SDK via SPM.")

    override suspend fun signUp(email: String, password: String): AuthResult =
        AuthResult.Failure("Firebase Auth is not configured for iOS yet. Please add Firebase iOS SDK via SPM.")

    override fun getCurrentUserId(): String? = null

    override suspend fun signOut() { /* no-op */ }
}

