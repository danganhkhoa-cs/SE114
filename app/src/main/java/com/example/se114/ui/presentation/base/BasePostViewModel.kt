package com.example.se114.ui.presentation.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.se114.data.Post
import com.example.se114.data.PostEventBus
import com.example.se114.data.PostUpdateEvent
import com.example.se114.data.Report
import com.example.se114.data.repository.PostRepository
import com.example.se114.local.PreferencesManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Trạng thái thông báo chung cho UI
data class BaseMessageState(
    val message: String? = null,
    val type: MessageType = MessageType.INFO
)

enum class MessageType { INFO, ERROR, SUCCESS }

abstract class BasePostViewModel(
    protected val repository: PostRepository,
    val preferencesManager: PreferencesManager,
    protected val postEventBus: PostEventBus
) : ViewModel() {

    // Quản lý thông báo (Snackbar) chung
    protected val _messageState = MutableStateFlow(BaseMessageState())
    val messageState = _messageState.asStateFlow()

    init {
        observeBusEvents()
        val userId = preferencesManager.userId
        if (userId.isNotEmpty()) {
            viewModelScope.launch {
                repository.updateFcmToken(userId)
            }
        }
        // Đăng ký nhận tin hệ thống
        viewModelScope.launch {
            repository.subscribeToSystemTopic()
        }
    }

    // --- LOGIC EVENT BUS (Lắng nghe thay đổi) ---
    private fun observeBusEvents() {
        viewModelScope.launch {
            postEventBus.events.collect { event ->
                handlePostUpdate(event)
            }
        }
    }

    // Hàm trừu tượng: Mỗi màn hình tự quyết định cách update list của mình khi có sự kiện
    // VD: Home thì update icon, Saved thì xóa bài nếu un-save
    abstract fun handlePostUpdate(event: PostUpdateEvent)

    // --- CÁC CHỨC NĂNG CHUNG (Actions) ---

    fun onToggleLike(post: Post) {
        val currentUserId = preferencesManager.userId
        val isCurrentlyLiked = post.isLiked
        val newLikeStatus = !isCurrentlyLiked
        val newLikeCount = (if (isCurrentlyLiked) post.likeCount - 1 else post.likeCount + 1).coerceAtLeast(0)

        // 1. Update UI ngay lập tức (Optimistic Update) thông qua EventBus để đồng bộ mọi nơi
        viewModelScope.launch {
            // Emit event để Home và Saved cùng update UI local
            postEventBus.emitEvent(PostUpdateEvent(post.id, isLiked = newLikeStatus, likeCount = newLikeCount))

            // 2. Gọi API background
            repository.toggleLikePost(post.id, currentUserId, isCurrentlyLiked)
        }
    }

    fun onToggleSave(post: Post, isSaved: Boolean) {
        val currentUserId = preferencesManager.userId
        val newStatus = !isSaved

        // Show message
        val msg = if (newStatus) preferencesManager.getString("post_saved") else preferencesManager.getString("unsave_post")
        _messageState.update { BaseMessageState(msg, MessageType.SUCCESS) }

        viewModelScope.launch {
            // Emit event (Quan trọng: SavedScreen sẽ lắng nghe cái này để xóa bài)
            postEventBus.emitEvent(PostUpdateEvent(post.id, isSaved = newStatus))

            // Call API
            repository.toggleSavePost(post.id, currentUserId, isSaved)
        }
    }

    open fun onHidePost(postId: String) {
        // Show message
        _messageState.update { BaseMessageState(preferencesManager.getString("post_hidden"), MessageType.INFO) }
        // Các lớp con cần override để filter bài viết khỏi list hiển thị
    }

    fun onSubmitReport(postId: String, reason: String, description: String) {
        val userId = preferencesManager.userId
        if (userId.isEmpty()) return

        viewModelScope.launch {
            val report = Report(reporterId = userId, postId = postId, reason = reason, description = description)
            val result = repository.createReport(report)

            if (result.isSuccess) {
                _messageState.update { BaseMessageState(preferencesManager.getString("report_success"), MessageType.SUCCESS) }
            } else {
                val error = result.exceptionOrNull()
                val msg = if (error?.message == "duplicate") preferencesManager.getString("report_duplicate_post")
                else preferencesManager.getString("unknown_error")
                _messageState.update { BaseMessageState(msg, MessageType.ERROR) }
            }
        }
    }

    fun clearMessage() {
        _messageState.update { it.copy(message = null) }
    }
}