package com.example.se114.data

import androidx.compose.runtime.mutableStateListOf

data class Post(
    val id: Int,
    val userName: String,
    val userAvatar: String,
    val timeAgo: String,
    val content: String,
    val location: String,
    val imageUrl: String? = null,
    val likeCount: Int,
    val commentCount: Int,
    val isLiked: Boolean = false
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
            location = "District 1, HCMC",
            likeCount = 24,
            commentCount = 8
        ),
        Post(
            id = 2,
            userName = "Tran Thi B",
            userAvatar = "",
            timeAgo = "5 hours ago",
            content = "Medical emergency. Looking for nearby hospital or ambulance.",
            location = "District 3, HCMC",
            likeCount = 45,
            commentCount = 12
        ),
        Post(
            id = 3,
            userName = "Le Van C",
            userAvatar = "",
            timeAgo = "1 day ago",
            content = "Lost pet - Golden Retriever. Last seen near Landmark 81. Please help!",
            location = "Binh Thanh, HCMC",
            likeCount = 67,
            commentCount = 23
        )
    )

    // --- MỚI: Danh sách ID các bài đã lưu (Global State) ---
    // Giả sử bài số 2 đã được lưu từ trước
    val savedPostIds = mutableStateListOf(2)

    // Hàm xử lý lưu/bỏ lưu dùng chung cho toàn App
    fun toggleSave(postId: Int) {
        if (savedPostIds.contains(postId)) {
            savedPostIds.remove(postId)
        } else {
            savedPostIds.add(postId)
        }
    }

    fun addPost(content: String) {
        val newPost = Post(
            id = (posts.maxOfOrNull { it.id } ?: 0) + 1,
            userName = "Bạn (Me)",
            userAvatar = "",
            timeAgo = "Vừa xong",
            content = content,
            location = "Vị trí của bạn",
            likeCount = 0,
            commentCount = 0
        )
        posts.add(0, newPost)
    }
}