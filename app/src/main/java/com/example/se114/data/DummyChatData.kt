package com.example.se114.data.dummy

import androidx.compose.runtime.mutableStateListOf
import com.example.se114.data.model.ChatMessage
import com.example.se114.data.model.Conversation

object DummyChatData {
    const val CURRENT_USER_ID = "me"

    // DANH SÁCH CUỘC TRÒ CHUYỆN
    val conversations = mutableStateListOf(
        Conversation(
            id = "1",
            name = "Phước đám roblox",
            avatar = "👤",
            lastMessage = "Started following you",
            lastMessageTime = "1m",
            unreadCount = 0,
            isOnline = true
        ),
        Conversation(
            id = "2",
            name = "Nguyễn Văn A",
            avatar = "👨‍💻",
            lastMessage = "Liked your post",
            lastMessageTime = "1h",
            unreadCount = 2,
            isOnline = false
        ),
        Conversation(
            id = "3",
            name = "Nebulanomad",
            avatar = "🎨",
            lastMessage = "Commented on your post",
            lastMessageTime = "2h",
            unreadCount = 1,
            isOnline = true
        ),
        Conversation(
            id = "4",
            name = "Luna Voyager",
            avatar = "🌙",
            lastMessage = "Saved your post",
            lastMessageTime = "1d",
            unreadCount = 0,
            isOnline = false
        )
    )

    // MAP LƯU TIN NHẮN CHI TIẾT
    private val messages = mutableMapOf<String, MutableList<ChatMessage>>()

    // Lấy tin nhắn theo ID cuộc trò chuyện
    fun getMessages(conversationId: String): List<ChatMessage> {
        if (!messages.containsKey(conversationId)) {
            messages[conversationId] = mutableStateListOf()
        }
        return messages[conversationId]!!
    }

    // Thêm tin nhắn vào cuộc trò chuyện
    fun addMessage(conversationId: String, msg: ChatMessage) {
        if (!messages.containsKey(conversationId)) {
            messages[conversationId] = mutableStateListOf()
        }
        messages[conversationId]?.add(msg)
    }

    // Đánh dấu đã đọc
    fun markAsRead(conversationId: String) {
        val index = conversations.indexOfFirst { it.id == conversationId }
        if (index != -1) {
            val current = conversations[index]
            conversations[index] = current.copy(unreadCount = 0)
        }
    }

    // Xóa cuộc trò chuyện
    fun deleteConversation(conversationId: String) {
        conversations.removeIf { it.id == conversationId }
    }

    // --- CẬP NHẬT: GỬI SOS ĐẾN CỘNG ĐỒNG (KHÔNG GỬI CHO BẢN THÂN) ---
    fun sendSOS(helpContent: String, location: String) {
        val sosId = "sos_${System.currentTimeMillis()}"

        // 1. Tạo cuộc trò chuyện với "Cộng đồng" (Thay vì Me)
        val sosConversation = Conversation(
            id = sosId,
            name = "Cộng đồng Hỗ trợ Khẩn cấp", // Tên nhóm nhận tin
            avatar = "📢", // Icon đại diện cho nhóm/cộng đồng
            lastMessage = "Bạn: SOS - $helpContent", // Hiển thị nội dung bạn vừa gửi
            lastMessageTime = "Vừa xong",
            unreadCount = 0, // Tin mình gửi đi thì không có unread
            isOnline = true
        )

        // Thêm vào đầu danh sách chat
        conversations.add(0, sosConversation)

        // 2. Tạo nội dung tin nhắn chi tiết
        val sosMessage = ChatMessage(
            id = "msg_$sosId",
            senderId = CURRENT_USER_ID, // Người gửi là "me" (để hiển thị bên phải màn hình chat)
            content = "⚠️ KHẨN CẤP ⚠️\nTôi cần giúp: $helpContent\n📍 Tại: $location",
            timestamp = System.currentTimeMillis()
        )
mmmmmmmmm
        addMessage(sosId, sosMessage)
    }
}