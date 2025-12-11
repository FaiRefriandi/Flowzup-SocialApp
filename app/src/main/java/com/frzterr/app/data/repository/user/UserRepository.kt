package com.frzterr.app.data.repository.user

import android.util.Log
import com.frzterr.app.data.remote.supabase.SupabaseManager
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AppUser(
    val id: String,
    val fullName: String?,
    val email: String?,
    val avatarUrl: String?,
    val provider: String?
)

class UserRepository {

    private val postgrest get() = SupabaseManager.client.postgrest

    // ============================================================
    // GET USER FROM SUPABASE
    // ============================================================

    suspend fun getUserById(uid: String): AppUser? = withContext(Dispatchers.IO) {

        Log.e("SUPABASE_USER_DB", "Fetching user with ID: $uid")

        val result = postgrest["users"]
            .select {
                filter {
                    eq("id", uid)
                }
            }
            .decodeList<AppUser>()

        val user = result.firstOrNull()

        Log.e("SUPABASE_USER_DB", "User fetched = $user")

        return@withContext user
    }

    // ============================================================
    // CREATE / UPDATE USER DATA IN SUPABASE
    // ============================================================

    suspend fun createOrUpdateUser(
        id: String,
        name: String?,
        email: String?,
        avatarUrl: String?,
        provider: String?
    ) = withContext(Dispatchers.IO) {

        Log.e(
            "SUPABASE_USER_DB",
            "Upserting user:\n" +
                    "ID = $id\n" +
                    "Name = $name\n" +
                    "Email = $email\n" +
                    "Avatar = $avatarUrl\n" +
                    "Provider = $provider"
        )

        val payload = mapOf(
            "id" to id,
            "full_name" to name,
            "email" to email,
            "avatar_url" to avatarUrl,
            "provider" to provider
        )

        postgrest["users"].upsert(payload)

        Log.e("SUPABASE_USER_DB", "Upsert complete")
    }
}
