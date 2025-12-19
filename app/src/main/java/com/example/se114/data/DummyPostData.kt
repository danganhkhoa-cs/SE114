package com.example.se114.data

import androidx.compose.runtime.mutableStateListOf

// 1. Định nghĩa Enum cho loại bài viết
enum class PostType {
    SUPPORT, // Cho tab Hỗ trợ
    SERVICE  // Cho tab Dịch vụ
}

data class Post(
    val id: Int,
    val userName: String,
    val userAvatar: String,
    val timeAgo: String,
    val content: String,
    val district: String,
    val city: String,
    val category: String,
    val imageUrl: String? = null,
    val likeCount: Int,
    val commentCount: Int,
    val isLiked: Boolean = false,
    // 2. Thêm trường type
    val type: PostType = PostType.SUPPORT
)

object DummyPostData {
    // Danh sách bài viết gốc
    val posts = mutableStateListOf(
        Post(
            id = 1,
            userName = "Nguyen Van A",
            userAvatar = "",
            timeAgo = "2 hours ago",
            content = "Need urgent help! Flooding in my area. Anyone nearby can assist?",
            district = "District 1",
            city = "HCMC",
            category = "Emergency",
            imageUrl = null,
            likeCount = 24,
            commentCount = 8,
            type = PostType.SUPPORT
        ),
        Post(
            id = 2,
            userName = "Tran Thi B",
            userAvatar = "",
            timeAgo = "5 hours ago",
            content = "Providing free medical checkups for elderly people this weekend.",
            district = "District 3",
            city = "HCMC",
            category = "Medical",
            imageUrl = null,
            likeCount = 45,
            commentCount = 12,
            type = PostType.SERVICE
        ),
        Post(
            id = 3,
            userName = "Le Van C",
            userAvatar = "",
            timeAgo = "1 day ago",
            content = "Lost pet - Golden Retriever. Last seen near Landmark 81. Please help!",
            district = "Binh Thanh",
            city = "HCMC",
            category = "Lost and Found",
            imageUrl = null,
            likeCount = 67,
            commentCount = 23,
            type = PostType.SUPPORT
        ),
        Post(
            id = 4,
            userName = "Dich Vu Sua Chua",
            userAvatar = "",
            timeAgo = "2 days ago",
            content = "Professional plumbing and electrical repair services. Available 24/7.",
            district = "District 7",
            city = "HCMC",
            category = "Repair",
            imageUrl = null,
            likeCount = 10,
            commentCount = 2,
            type = PostType.SERVICE
        )
    )

    val savedPostIds = mutableStateListOf(2)

    fun toggleSave(postId: Int) {
        if (savedPostIds.contains(postId)) {
            savedPostIds.remove(postId)
        } else {
            savedPostIds.add(postId)
        }
    }

    fun addPost(
        content: String,
        district: String,
        city: String,
        category: String,
        imageUrl: String? = null,
        type: PostType
    ) {
        val newPost = Post(
            id = (posts.maxOfOrNull { it.id } ?: 0) + 1,
            userName = "Bạn (Me)",
            userAvatar = "",
            timeAgo = "Vừa xong",
            content = content,
            district = district,
            city = city,
            category = category,
            imageUrl = imageUrl,
            likeCount = 0,
            commentCount = 0,
            type = type
        )
        posts.add(0, newPost)
    }
}