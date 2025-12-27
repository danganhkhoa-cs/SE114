package com.example.se114.data

import com.google.firebase.Timestamp

data class Comment(
    val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatar: String = "",
    val content: String = "",
    val parentId: String? = null, // Null nếu là Root Comment
    val likeCount: Int = 0,
    val timestamp: Timestamp = Timestamp.now(),

    // Các trường phục vụ UI Local (không lưu trực tiếp trên Firestore comment document)
    val isLiked: Boolean = false,
    val replies: List<Comment> = emptyList() // List chứa các comment con
)