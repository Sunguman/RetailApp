package com.example.retail360.data

import com.example.retail360.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.tasks.await

class AuthRepository(private val firebase: FirebaseHelper) {
    private val auth = FirebaseAuth.getInstance()

    fun isLoggedIn(): Boolean = auth.currentUser != null

    fun currentUser(): User? = auth.currentUser?.let {
        User(
            uid = it.uid,
            email = it.email ?: "",
            name = it.displayName ?: "User",
            phone = it.phoneNumber ?: ""
        )
    }

    fun signOut() {
        auth.signOut()
    }

    suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    suspend fun signUp(email: String, password: String, name: String, phone: String) {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val profileUpdates = userProfileChangeRequest {
            displayName = name
        }
        result.user?.updateProfile(profileUpdates)?.await()
        // Note: phone number in Firebase Auth is usually for SMS login.
        // In a real app, we'd save (uid, phone, etc) to Firestore/Realtime DB here.
    }
}
