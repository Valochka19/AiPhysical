package com.example.aiphysical.data.service

sealed class AuthResult {
    data class Success(val uid: String) : AuthResult()
    data class Failure(val message: String) : AuthResult()
}

interface FirebaseAuthService {
    suspend fun signIn(email: String, password: String): AuthResult
    suspend fun signUp(email: String, password: String): AuthResult
    fun getCurrentUserId(): String?
    suspend fun signOut()
}

