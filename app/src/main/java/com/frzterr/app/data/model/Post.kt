package com.frzterr.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Post(
    val id: String,

    @SerialName("user_id")
    val userId: String,

    val content: String,

    @SerialName("image_url")
    val imageUrl: String? = null,

    @SerialName("created_at")
    val createdAt: String,

    @SerialName("like_count")
    val likeCount: Int = 0,

    @SerialName("comment_count")
    val commentCount: Int = 0,

    @SerialName("repost_count")
    val repostCount: Int = 0
) {
    val imageUrls: List<String>
        get() = try {
            if (imageUrl.isNullOrBlank()) {
                emptyList()
            } else if (imageUrl.trim().startsWith("[")) {
                // Robust parsing: Remove brackets and quotes, then split
                imageUrl.replace(Regex("[\\[\\]\"]"), "") // Remove [ ] "
                    .split(",")
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
            } else {
                listOf(imageUrl)
            }
        } catch (e: Exception) {
            if (imageUrl != null) listOf(imageUrl) else emptyList()
        }
}
