package com.example.se114.ui.presentation.chat

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.se114.data.dummy.DummyChatData
import com.example.se114.data.model.ChatMessage
import com.example.se114.data.model.Conversation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class ChatDetailUiState(
    val conversation: Conversation? = null,
    val messages: List<ChatMessage> = emptyList(),
    val messageInput: String = ""
)

@HiltViewModel
class ChatDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatDetailUiState())
    val uiState = _uiState.asStateFlow()

    // Lấy ID từ Navigation Arguments (nếu dùng SavedStateHandle)
    // Hoặc gọi hàm loadConversation từ Screen cũng được.
    // Ở đây tôi chọn cách gọi hàm load từ Screen cho linh hoạt theo luồng cũ.
    private var currentConversationId: String = ""

    fun loadConversation(conversationId: String) {
        currentConversationId = conversationId
        val conversation = DummyChatData.conversations.find { it.id == conversationId }
        val messages = DummyChatData.getMessages(conversationId)

        _uiState.update {
            it.copy(conversation = conversation, messages = messages)
        }
    }

    fun onMessageInputChange(newText: String) {
        _uiState.update { it.copy(messageInput = newText) }
    }

    fun sendMessage(autoReplyText: String) {
        val text = _uiState.value.messageInput
        if (text.isBlank()) return

        // 1. Thêm tin nhắn của mình
        val myMsg = ChatMessage(
            id = UUID.randomUUID().toString(),
            senderId = DummyChatData.CURRENT_USER_ID,
            content = text.trim(),
            timestamp = System.currentTimeMillis()
        )
        DummyChatData.addMessage(currentConversationId, myMsg)

        // Cập nhật UI ngay lập tức và xóa input
        refreshMessages()
        _uiState.update { it.copy(messageInput = "") }

        // 2. Giả lập đối phương trả lời sau 1 giây
        viewModelScope.launch {
            delay(1000)
            val replyMsg = ChatMessage(
                id = UUID.randomUUID().toString(),
                senderId = currentConversationId, // Sender là ID của conversation (người kia)
                content = autoReplyText,
                timestamp = System.currentTimeMillis()
            )
            DummyChatData.addMessage(currentConversationId, replyMsg)
            refreshMessages()
        }
    }

    private fun refreshMessages() {
        val updatedMessages = DummyChatData.getMessages(currentConversationId)
        _uiState.update { it.copy(messages = updatedMessages) }
    }
}