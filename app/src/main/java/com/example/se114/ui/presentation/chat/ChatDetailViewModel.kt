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

                val avatarUrl = doc.getString("avatar_url")
                val avatar = if (!avatarUrl.isNullOrEmpty()) avatarUrl else name.take(1).uppercase()

                val phone = doc.getString("phone") ?: ""
                val email = doc.getString("email") ?: ""

                val summary = UserSummary(partnerId, name, avatar, phone, email)
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

        // 1. Kiểm tra nếu đối phương đã xóa tài khoản
        if (conversation.deletedAccountUsers.isNotEmpty()) {
            _uiState.update { it.copy(sendError = preferencesManager.getString("user_inactive")) }
            return
        }

        // 2. CHECK BLOCK: Nếu trạng thái Chat là REJECTED (Bị chặn)
        if (conversation.status == ChatStatus.REJECTED) {
            _uiState.update { it.copy(sendError = preferencesManager.getString("blocked_msg_error")) }
            return
        }

        viewModelScope.launch {
            try {
                // 3. CHECK BLOCK MẠNH HƠN: Kiểm tra trực tiếp blockedUsers list của đối phương
                val partnerId = conversation.participants.find { it != currentUserId } ?: ""
                if (partnerId.isNotEmpty()) {
                    val partnerDoc = firestore.collection("users").document(partnerId).get().await()
                    val blockedList = partnerDoc.get("blockedUsers") as? List<String> ?: emptyList()
                    if (blockedList.contains(currentUserId)) {
                        _uiState.update { it.copy(sendError = preferencesManager.getString("user_unavailable")) }
                        return@launch
                    }
                }

                val messageId = UUID.randomUUID().toString()
                val message = ChatMessage(
                    id = messageId,
                    senderId = currentUserId,
                    content = content,
                    timestamp = System.currentTimeMillis()
                )

                // 4. Add Message
                firestore.collection("conversations").document(currentConversationId)
                    .collection("messages").document(messageId).set(message).await()

                // 5. Update Conversation Last Message
                val updates = mutableMapOf<String, Any>(
                    "lastMessage" to content,
                    "lastMessageTime" to System.currentTimeMillis(),
                    "lastSenderId" to currentUserId,
                    "readBy" to listOf(currentUserId),
                    "deletedBy" to emptyList<String>() // Un-hide chat for everyone
                )

                if (conversation.status == ChatStatus.REJECTED) {
                    updates["status"] = ChatStatus.PENDING
                    updates["requestSenderId"] = currentUserId
                }

                firestore.collection("conversations").document(currentConversationId)
                    .update(updates)
                    .await()

                _uiState.update { it.copy(messageInput = "") }
            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(sendError = "${preferencesManager.getString("error")}: ${e.message}") }
            }
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
            try {
                firestore.collection("conversations").document(currentConversationId)
                    .update("status", ChatStatus.ACCEPTED).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun declineConversation() {
        viewModelScope.launch {
            try {
                firestore.collection("conversations").document(currentConversationId)
                    .update("deletedBy", FieldValue.arrayUnion(currentUserId))
                    .await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}