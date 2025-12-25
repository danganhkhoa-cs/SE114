package com.example.se114.data

import com.google.firebase.Timestamp

enum class PostType {
    SUPPORT,
    SERVICE
}

data class Post(
    val id: String = "", // Đổi thành String
    val userId: String = "",
    val userName: String = "",
    val userAvatar: String = "",
    val content: String = "",
    val district: String = "",
    val city: String = "",
    val category: String = "",
    val imageUrl: String? = null,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val isLiked: Boolean = false, // Chuyển isLiked vào đây để dễ quản lý
    val type: String = PostType.SUPPORT.name,
    val createdAt: Timestamp = Timestamp.now()
) {
    val timeAgo: String
        get() {
            val now = Timestamp.now().seconds
            val created = createdAt.seconds
            val diff = now - created
            return when {
                diff < 60 -> "Vừa xong"
                diff < 3600 -> "${diff / 60} phút trước"
                diff < 86400 -> "${diff / 3600} giờ trước"
                else -> "${diff / 86400} ngày trước"
            }
        }
}