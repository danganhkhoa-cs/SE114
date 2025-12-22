package com.example.se114.data.model

enum class ChatStatus {
    ACCEPTED, // Đã chấp nhận chat (có thể là bạn hoặc người lạ)
    PENDING,  // Tin nhắn chờ (Spam) - Người nhận chưa trả lời/chấp nhận
    REJECTED  // Đã chặn / Từ chối
}

enum class FriendshipState {
    NONE,       // Không phải bạn bè
    PENDING,    // Đang chờ chấp nhận kết bạn (Lời mời kết bạn)
    FRIENDS     // Đã là bạn bè
}

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class Conversation(
    val id: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Long = System.currentTimeMillis(),

    // --- TRẠNG THÁI CHAT (Messaging) ---
    val status: ChatStatus = ChatStatus.ACCEPTED,
    val requestSenderId: String = "", // Ai là người bắt đầu cuộc trò chuyện (gửi tin nhắn đầu tiên)

    // --- TRẠNG THÁI BẠN BÈ (Friendship) - TÁCH BIỆT VỚI CHAT ---
    val friendshipState: FriendshipState = FriendshipState.NONE,
    val friendRequestSenderId: String = "", // Ai là người gửi lời mời kết bạn

    val participants: List<String> = emptyList(),

    // Unread logic
    val lastSenderId: String = "",
    val readBy: List<String> = emptyList(),

    // Logic ẩn tin nhắn (Xóa một phía)
    val deletedBy: List<String> = emptyList(),

    // Logic ẩn nội dung tin nhắn cũ (Map<UserId, Timestamp>)
    val hiddenTimestamps: Map<String, Long> = emptyMap(),

    // Logic User đã xóa tài khoản
    val deletedAccountUsers: List<String> = emptyList(),

    // Cache data
    val participantData: Map<String, UserSummary> = emptyMap()
) {
    fun isUnread(myId: String): Boolean {
        return lastSenderId.isNotEmpty() && lastSenderId != myId && !readBy.contains(myId)
    }

    fun getPartnerId(myId: String): String {
        return participants.find { it != myId } ?: ""
    }

    fun isDeletedBy(myId: String): Boolean {
        return deletedBy.contains(myId)
    }
}

data class UserSummary(
    val uid: String = "",
    val name: String = "",
    val avatar: String = "",
    val phone: String = "",
    val email: String = "" // Thêm trường email
)