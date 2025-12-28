package com.example.se114.data

import com.google.firebase.Timestamp

enum class PostType {
    SUPPORT,
    SERVICE
}

data class Post(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userAvatar: String = "",
    val content: String = "",
    val district: String = "",
    val city: String = "",
    val cityKey: String? = null,
    val districtKey: String? = null,
    val categoryKey: String? = null,
    val category: String = "",
    val imageUrl: String? = null,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val isLiked: Boolean = false,
    val type: String = PostType.SUPPORT.name,
    // Chỉ lưu thời điểm tạo, không lưu chuỗi "x phút trước" cố định
    val createdAt: Timestamp = Timestamp.now(),
    val status: String = "PUBLISHED"

)