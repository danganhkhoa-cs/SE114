package com.example.se114.ui.presentation.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.se114.data.Comment
import com.example.se114.data.Post
import com.example.se114.data.PostEventBus
import com.example.se114.data.PostUpdateEvent
import com.example.se114.data.repository.PostRepository
import com.example.se114.local.PreferencesManager
import com.example.se114.ui.presentation.components.BaseMessageState
import com.example.se114.ui.presentation.components.BasePostViewModel
import com.example.se114.ui.presentation.components.MessageType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PostDetailViewModel @Inject constructor(
    repository: PostRepository,
    preferencesManager: PreferencesManager,
    postEventBus: PostEventBus,
    savedStateHandle: SavedStateHandle
) : BasePostViewModel(repository, preferencesManager, postEventBus) {

    private val postId: String = checkNotNull(savedStateHandle["postId"])

    private val _post = MutableStateFlow<Post?>(null)
    val post = _post.asStateFlow()

    // Cache danh sách ID comment đã like (để update UI mượt hơn)
    private val _likedCommentIds = MutableStateFlow<Set<String>>(emptySet())

    // Combine comment từ server với trạng thái Like local
    val comments: StateFlow<List<Comment>> = repository.getCommentsFlow(postId, preferencesManager.userId)
        .combine(_likedCommentIds) { commentList, likedSet ->
            commentList.map { root ->
                // Map trạng thái like cho root và replies của nó
                val mappedReplies = root.replies.map { child ->
                    child.copy(isLiked = child.id in likedSet)
                }
                root.copy(
                    isLiked = root.id in likedSet,
                    replies = mappedReplies
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _commentText = MutableStateFlow("")
    val commentText = _commentText.asStateFlow()

    // --- MỚI: Trạng thái đang reply comment nào ---
    private val _replyingTo = MutableStateFlow<Comment?>(null)
    val replyingTo = _replyingTo.asStateFlow()

    init {
        loadPostDetail()
        loadLikedComments() // Load danh sách like ban đầu
    }

    private fun loadPostDetail() {
        viewModelScope.launch {
            val result = repository.getPostsByIds(listOf(postId), preferencesManager.userId)
            if (result.isSuccess) {
                val posts = result.getOrThrow()
                if (posts.isNotEmpty()) _post.value = posts.first()
            }
        }
    }

    private fun loadLikedComments() {
        viewModelScope.launch {
            val ids = repository.getLikedCommentIds(preferencesManager.userId, postId)
            _likedCommentIds.value = ids.toSet()
        }
    }

    fun onCommentTextChanged(text: String) {
        _commentText.value = text
    }

    // --- MỚI: Chọn comment để reply ---
    fun onReplyToComment(comment: Comment) {
        _replyingTo.value = comment
        // Có thể focus vào ô nhập liệu ở UI nếu cần
    }

    // --- MỚI: Hủy chế độ reply ---
    fun onCancelReply() {
        _replyingTo.value = null
    }

    // --- MỚI: Toggle Like Comment ---
    fun onToggleLikeComment(comment: Comment) {
        val currentUserId = preferencesManager.userId
        val isCurrentlyLiked = comment.isLiked

        // Optimistic Update (Cập nhật UI ngay)
        val currentSet = _likedCommentIds.value
        _likedCommentIds.value = if (isCurrentlyLiked) currentSet - comment.id else currentSet + comment.id

        viewModelScope.launch {
            repository.toggleLikeComment(postId, comment.id, currentUserId, isCurrentlyLiked)
        }
    }

    fun sendComment() {
        val content = _commentText.value.trim()
        if (content.isEmpty()) return

        val currentPost = _post.value ?: return
        val currentUser = preferencesManager
        // Lấy parentId từ comment đang reply (nếu có)
        val parentId = _replyingTo.value?.id

        viewModelScope.launch {
            val result = repository.addComment(
                postId = currentPost.id,
                content = content,
                parentId = parentId,
                userId = currentUser.userId,
                userName = currentUser.userName,
                userAvatar = currentUser.userAvatar
            )

            if (result.isSuccess) {
                _commentText.value = ""
                _replyingTo.value = null // Reset trạng thái reply sau khi gửi
            } else {
                _messageState.update { BaseMessageState("Failed to send comment", MessageType.ERROR) }
            }
        }
    }

    override fun handlePostUpdate(event: PostUpdateEvent) {
        val currentPost = _post.value ?: return
        if (currentPost.id == event.postId) {
            _post.update {
                it?.copy(
                    isLiked = event.isLiked ?: it.isLiked,
                    likeCount = event.likeCount ?: it.likeCount
                )
            }
        }
    }
}