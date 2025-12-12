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
    // 2. Dùng mutableStateListOf để Home tự động cập nhật khi có bài mới
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

    // 3. Hàm thêm bài viết vào ĐẦU danh sách (index 0)
    fun addPost(content: String) {
        val newPost = Post(
            id = (posts.maxOfOrNull { it.id } ?: 0) + 1,
            userName = "Bạn (Me)", // Tên hiển thị khi bạn đăng
            userAvatar = "", // Có thể thêm avatar của bạn vào đây
            timeAgo = "Vừa xong",
            content = content,
            location = "Vị trí của bạn",
            likeCount = 0,
            commentCount = 0
        )
        // add(0, ...) để chèn vào đầu danh sách
        posts.add(0, newPost)
    }
}