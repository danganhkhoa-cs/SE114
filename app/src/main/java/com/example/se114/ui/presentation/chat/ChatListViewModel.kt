package com.example.se114.ui.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.se114.data.dummy.DummyChatData
import com.example.se114.data.model.Conversation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import javax.inject.Inject

data class ChatListUiState(
    val conversations: List<Conversation> = emptyList(),
    val searchQuery: String = "",
    val isShowingAddFriendDialog: Boolean = false
)

@HiltViewModel
class ChatListViewModel @Inject constructor(
    // Inject Repository nếu có
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState = _uiState.asStateFlow()

    // Danh sách gốc để lọc
    private var allConversations: List<Conversation> = emptyList()

    init {
        loadConversations()
    }

    private fun loadConversations() {
        // Giả lập lấy dữ liệu từ Dummy
        allConversations = DummyChatData.conversations
        updateFilteredList()
    }

    // --- Search Logic ---

    fun onSearchQueryChange(newQuery: String) {
        _uiState.update { it.copy(searchQuery = newQuery) }
        updateFilteredList()
    }

    private fun updateFilteredList() {
        val query = _uiState.value.searchQuery
        val filtered = if (query.isBlank()) {
            allConversations
        } else {
            allConversations.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.lastMessage.contains(query, ignoreCase = true)
            }
        }
        _uiState.update { it.copy(conversations = filtered) }
    }

    // --- Actions ---

    fun markAsRead(conversationId: String) {
        DummyChatData.markAsRead(conversationId)
        loadConversations() // Reload để cập nhật UI (bỏ chấm đỏ)
    }

    fun deleteConversation(conversationId: String) {
        DummyChatData.deleteConversation(conversationId)
        loadConversations() // Reload list
    }

    fun addFriend(phoneNumber: String) {
        // Logic thêm bạn giả lập
        val newChat = Conversation(
            id = UUID.randomUUID().toString(),
            name = "User $phoneNumber",
            avatar = phoneNumber.takeLast(1),
            lastMessage = "Hello!",
            lastMessageTime = "Now",
            unreadCount = 0,
            isOnline = true
        )
        DummyChatData.conversations.add(0, newChat)

        loadConversations()
        hideAddFriendDialog()
    }

    // --- Dialog Controls ---

    fun showAddFriendDialog() {
        _uiState.update { it.copy(isShowingAddFriendDialog = true) }
    }

    fun hideAddFriendDialog() {
        _uiState.update { it.copy(isShowingAddFriendDialog = false) }
    }
}