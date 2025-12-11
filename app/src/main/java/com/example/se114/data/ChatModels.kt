package com.example.se114.data.model

data class ChatMessage(
    val id: String,
    val senderId: String,
    val content: String,
    val timestamp: Long
)

data class Conversation(
    val id: String,
    val name: String,
    val avatar: String,       // Icon giả lập hoặc URL ảnh
    val lastMessage: String,
    val lastMessageTime: String, // Ví dụ: "2m", "1h"
    val unreadCount: Int = 0,    // Số tin nhắn chưa đọc (để hiện chấm đỏ)
    val isOnline: Boolean = false
)