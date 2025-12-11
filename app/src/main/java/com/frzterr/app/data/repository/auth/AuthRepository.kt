package com.frzterr.app.data.repository.auth

import android.util.Log
import com.frzterr.app.data.remote.supabase.SupabaseManager
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class AuthRepository {

    private val auth get() = SupabaseManager.client.auth

    // ============================================================
    // EMAIL SIGNUP
    // ============================================================

    suspend fun signUpWithEmail(email: String, password: String): UserInfo? {
        Log.e("SUPABASE_EMAIL", "Trying signUpWithEmail: $email")

        auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }

        return auth.currentUserOrNull()
    }

    // ============================================================
    // EMAIL LOGIN
    // ============================================================

    suspend fun signInWithEmail(email: String, password: String): UserInfo? {
        Log.e("SUPABASE_EMAIL", "Trying signInWithEmail: $email")

        auth.signInWith(Email) {
            this.email = email
            this.password = password
        }

        return auth.currentUserOrNull()
    }

    // ============================================================
    // RESET PASSWORD
    // ============================================================
    suspend fun sendPasswordResetEmail(email: String) {
        auth.resetPasswordForEmail(email)
    }
    suspend fun updatePassword(newPassword: String) {
        auth.updateUser {
            password = newPassword
        }
    }

    // ============================================================
    // GOOGLE LOGIN VIA ID TOKEN (GIS Credential Manager)
    // ============================================================

    suspend fun signInWithGoogleIdToken(idToken: String): UserInfo? {
        Log.e("SUPABASE_GOOGLE", "Received ID Token (first 20 chars): ${idToken.take(20)}...")

        val session = auth.signInWith(IDToken) {
            this.idToken = idToken
            this.provider = Google
        }

        Log.e("SUPABASE_GOOGLE", "Supabase session response: $session")

        return auth.currentUserOrNull()
    }

    // ============================================================
    // UPDATE DISPLAY NAME
    // ============================================================

    suspend fun updateDisplayName(name: String) {
        Log.e("SUPABASE_USER", "Updating display name to: $name")

        auth.updateUser {
            data = buildJsonObject {
                put("full_name", name)
            }
        }
    }

    // ============================================================
    // SIGN OUT
    // ============================================================

    suspend fun signOut() {
        Log.e("SUPABASE_AUTH", "Signing out user")
        auth.signOut()
    }

    // ============================================================
    // GET CURRENT USER
    // ============================================================

    fun getCurrentUser(): UserInfo? {
        val usr = auth.currentUserOrNull()
        Log.e("SUPABASE_AUTH", "Current user = $usr")
        return usr
    }

    fun isLoggedIn(): Boolean {
        val session = auth.currentSessionOrNull()
        Log.e("SUPABASE_AUTH", "Checking isLoggedIn = ${session != null}")
        return session != null
    }
}
