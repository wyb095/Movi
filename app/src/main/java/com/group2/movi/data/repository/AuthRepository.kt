package com.group2.movi.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.group2.movi.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.channels.awaitClose
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    val currentUser: FirebaseUser? get() = auth.currentUser
    val isLoggedIn: Boolean get() = currentUser != null

    fun authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    suspend fun register(displayName: String, email: String, password: String): Result<String> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return Result.failure(Exception("No UID returned"))

            // Send email verification
            result.user?.sendEmailVerification()?.await()

            // Create Firestore user doc
            val user = User(
                userId = uid,
                displayName = displayName,
                email = email,
                isVerified = false,
                createdAt = Timestamp.now()
            )
            firestore.collection("users").document(uid).set(user).await()
            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<String> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: return Result.failure(Exception("No UID"))
            // Reload to pick up email verification state
            result.user?.reload()?.await()
            Result.success(uid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPasswordReset(email: String): Result<Unit> = runCatching {
        auth.sendPasswordResetEmail(email).await()
    }

    suspend fun resendVerification(): Result<Unit> = runCatching {
        auth.currentUser?.sendEmailVerification()?.await()
        Unit
    }

    suspend fun reloadUser(): Boolean {
        return try {
            auth.currentUser?.reload()?.await()
            auth.currentUser?.isEmailVerified == true
        } catch (e: Exception) {
            false
        }
    }

    fun logout() = auth.signOut()

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", RegexOption.IGNORE_CASE)

        fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email.trim())
    }
}
