package com.example.aiphysical.data.service

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class FirebaseAuthServiceImpl : FirebaseAuthService {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override suspend fun signIn(email: String, password: String): AuthResult {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw Exception("UID is null after sign-in")
            AuthResult.Success(uid)
        } catch (e: Exception) {
            AuthResult.Failure(e.localizedMessage ?: "Ошибка авторизации")
        }
    }

    override suspend fun signUp(email: String, password: String): AuthResult {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw Exception("UID is null after sign-up")
            AuthResult.Success(uid)
        } catch (e: Exception) {
            AuthResult.Failure(e.localizedMessage ?: "Ошибка регистрации")
        }
    }

    override fun getCurrentUserId(): String? = auth.currentUser?.uid

    override suspend fun signOut() {
        auth.signOut()
    }
}

