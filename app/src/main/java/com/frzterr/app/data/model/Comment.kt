package com.frzterr.app.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Comment(
    val id: String,

    @SerialName("post_id")
    val postId: String,

    @SerialName("user_id")
    val userId: String = "",
    
    @SerialName("parent_comment_id")
    val parentCommentId: String? = null,
    
    val content: String = "",
    
    @SerialName("created_at")
    val createdAt: String = "",
    
    @SerialName("like_count")
    val likeCount: Long = 0
)
