package com.example.se114.ui.presentation.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.se114.data.model.ChatMessage
import com.example.se114.data.model.ChatStatus
import com.example.se114.data.model.Conversation
import com.example.se114.data.model.FriendshipState
import com.example.se114.data.model.UserSummary
import com.example.se114.local.PreferencesManager
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject

data class ChatDetailUiState(
    val conversation: Conversation? = null,
    val messages: List<ChatMessage> = emptyList(),
    val messageInput: String = "",
    val isPending: Boolean = false, // Vẫn dùng để check xem có phải spam không
    val partnerProfile: UserSummary? = null,
    val isLoading: Boolean = false,
    val sendError: String? = null
)

@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    savedStateHandle: SavedStateHandle,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val currentConversationId: String = checkNotNull(savedStateHandle["conversationId"])
    private val currentUserId = preferencesManager.userId

    private val _uiState = MutableStateFlow(ChatDetailUiState())
    val uiState = _uiState.asStateFlow()

    // Cache toàn bộ tin nhắn raw (chưa lọc)
    private var _allMessages: List<ChatMessage> = emptyList()

    init {
        // ... (Logic init nếu cần)
    }

    fun loadConversation(conversationId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Listen to Conversation Document
            firestore.collection("conversations").document(conversationId)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) return@addSnapshotListener
                    if (snapshot != null && snapshot.exists()) {
                        val conversation = snapshot.toObject(Conversation::class.java)

                        // Check if pending (Spam check)
                        val isPending = conversation?.status == ChatStatus.PENDING &&
                                conversation.requestSenderId != currentUserId &&
                                conversation.friendshipState != FriendshipState.FRIENDS

                        _uiState.update { it.copy(conversation = conversation, isPending = isPending) }

                        // CẬP NHẬT LẠI DANH SÁCH TIN NHẮN HIỂN THỊ DỰA TRÊN hiddenTimestamps
                        updateDisplayedMessages()

                        // Load Partner Profile (Optional if needed explicitly)
                        val partnerId = conversation?.participants?.find { it != currentUserId }
                        if (partnerId != null) {
                            loadPartnerProfile(partnerId)
                        }
                    } else {
                        // Handle conversation deleted/not found
                        _uiState.update { it.copy(sendError = "Cuộc trò chuyện không tồn tại") }
                    }
                }

            // Listen to Messages
            firestore.collection("conversations").document(conversationId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) return@addSnapshotListener
                    if (snapshot != null) {
                        // Lưu vào cache raw
                        _allMessages = snapshot.toObjects(ChatMessage::class.java)

                        // Lọc và hiển thị
                        updateDisplayedMessages()

                        _uiState.update { it.copy(isLoading = false) }

                        // Mark as Read
                        if (_allMessages.isNotEmpty()) {
                            firestore.collection("conversations").document(conversationId)
                                .update("readBy", FieldValue.arrayUnion(currentUserId))
                        }
                    }
                }
        }
    }

    // Hàm lọc tin nhắn dựa trên thời điểm người dùng xóa chat
    private fun updateDisplayedMessages() {
        val conversation = _uiState.value.conversation ?: return

        // Lấy thời điểm user xóa chat gần nhất (nếu có)
        val hiddenTimestamp = conversation.hiddenTimestamps[currentUserId] ?: 0L

        // Chỉ hiển thị tin nhắn đến SAU thời điểm xóa
        val filteredMessages = _allMessages.filter { it.timestamp > hiddenTimestamp }

        _uiState.update { it.copy(messages = filteredMessages) }
    }

    private fun loadPartnerProfile(partnerId: String) {
        viewModelScope.launch {
            try {
                val doc = firestore.collection("users").document(partnerId).get().await()
                val name = doc.getString("name") ?: "Unknown"
                val avatar = doc.getString("avatar_url") ?: name.take(1).uppercase()
                val phone = doc.getString("phone") ?: ""
                val summary = UserSummary(partnerId, name, avatar, phone)
                _uiState.update { it.copy(partnerProfile = summary) }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun onMessageInputChange(input: String) {
        _uiState.update { it.copy(messageInput = input) }
    }

    fun clearError() {
        _uiState.update { it.copy(sendError = null) }
    }

    fun sendMessage() {
        val content = _uiState.value.messageInput.trim()
        if (content.isBlank()) return

        val conversation = _uiState.value.conversation ?: return

        // Kiểm tra nếu đối phương đã xóa tài khoản (giả sử có logic này trong model)
        if (conversation.deletedAccountUsers.isNotEmpty()) {
            _uiState.update { it.copy(sendError = "Người dùng này không còn hoạt động") }
            return
        }

        viewModelScope.launch {
            val messageId = UUID.randomUUID().toString()
            val message = ChatMessage(
                id = messageId,
                senderId = currentUserId,
                content = content,
                timestamp = System.currentTimeMillis()
            )

            // 1. Add Message
            firestore.collection("conversations").document(currentConversationId)
                .collection("messages").document(messageId).set(message).await()

            // 2. Update Conversation Last Message
            val updates = mutableMapOf<String, Any>(
                "lastMessage" to content,
                "lastMessageTime" to System.currentTimeMillis(),
                "lastSenderId" to currentUserId,
                "readBy" to listOf(currentUserId),
                "deletedBy" to emptyList<String>() // Un-hide chat for everyone
            )

            // Nếu status đang là REJECTED (đã chặn/từ chối), set lại PENDING để hiện bên Spam của họ
            // Hoặc nếu đang PENDING thì giữ nguyên
            if (conversation.status == ChatStatus.REJECTED) {
                updates["status"] = ChatStatus.PENDING
                updates["requestSenderId"] = currentUserId
            }

            // Nếu là lần đầu chat (status ACCEPTED nhưng deletedBy có người kia) -> Nó tự un-hide nhờ dòng deletedBy trên

            firestore.collection("conversations").document(currentConversationId)
                .update(updates)

            _uiState.update { it.copy(messageInput = "") }
        }
    }

    fun deleteChatOneSided(onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                firestore.collection("conversations").document(currentConversationId)
                    .update(
                        mapOf(
                            "deletedBy" to FieldValue.arrayUnion(currentUserId),
                            "hiddenTimestamps.$currentUserId" to System.currentTimeMillis()
                        )
                    )
                    .await()
                onSuccess()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun acceptConversation() {
        viewModelScope.launch {
            firestore.collection("conversations").document(currentConversationId)
                .update("status", ChatStatus.ACCEPTED).await()
        }
    }

    // --- FIX: TỪ CHỐI TIN NHẮN CHỜ (DECLINE MESSAGE) ---
    // Thay vì xóa vĩnh viễn (delete), ta dùng "Xóa mềm" (thêm vào deletedBy).
    // Điều này làm ẩn tin nhắn khỏi danh sách Spam/Inbox của người dùng hiện tại,
    // NHƯNG vẫn giữ lại Conversation Document để Lời mời kết bạn (Friend Request) không bị mất.
    fun declineConversation() {
        viewModelScope.launch {
            firestore.collection("conversations").document(currentConversationId)
                .update("deletedBy", FieldValue.arrayUnion(currentUserId))
                .await()
        }
    }
}