package com.frzterr.app.utils

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import java.security.MessageDigest
import java.util.UUID

class GoogleSignInHelper(private val context: Context) {

    // Web Client ID Supabase kamu
    private val webClientId =
        "25471157266-g9p37e12ldum46rc3ob9usi03a3f961e.apps.googleusercontent.com"

    private val credentialManager = CredentialManager.create(context)

    // ============================================================
    // Generate SHA256 nonce (Google requires this for Android sign-in)
    // ============================================================
    private fun generateNonce(): String {
        val raw = UUID.randomUUID().toString()
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(raw.toByteArray())

        return digest.joinToString("") { "%02x".format(it) }
    }

    // ============================================================
    // Launch Google Credential Picker and get ID Token
    // ============================================================
    suspend fun getGoogleIdToken(): String? {

        val nonce = generateNonce()
        Log.e("GOOGLE_FLOW", "Generated Nonce = $nonce")

        val googleOption = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false) // always show picker
            .setServerClientId(webClientId)
            .setNonce(nonce)
            .setAutoSelectEnabled(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleOption)
            .build()

        return try {
            Log.e("GOOGLE_FLOW", "Launching Google CredentialManager request")

            val result = credentialManager.getCredential(
                context,
                request
            )

            Log.e("GOOGLE_FLOW", "CredentialManager returned: ${result.credential}")

            val googleCred = GoogleIdTokenCredential.createFrom(result.credential.data)

            val token = googleCred.idToken

            Log.e("GOOGLE_FLOW", "Google ID Token extracted (first 20 chars): ${token.take(20)}...")

            token

        } catch (e: Exception) {
            Log.e("GOOGLE_FLOW_ERROR", "Google ID Token fetch failed: ${e.message}", e)
            null
        }
    }
}
