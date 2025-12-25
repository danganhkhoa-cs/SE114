package com.example.se114.data

import com.google.firebase.Timestamp

data class Report(
    val id: String = "",
    val reporterId: String = "",

    // 2 trường này để phân biệt report Post hay User (Nullable)
    val postId: String? = null,
    val reportedUserId: String? = null,

    val reason: String = "",
    val description: String = "",
    val status: String = "PENDING",
    val createdAt: Timestamp = Timestamp.now()
)