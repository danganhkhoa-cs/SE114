package com.example.se114.ui.presentation.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.se114.data.Comment
import com.example.se114.data.repository.PostRepository
import com.example.se114.local.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CommentViewModel @Inject constructor(
    private val repository: PostRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _comments = MutableStateFlow<List<Comment>>(emptyList())
    val comments = _comments.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    // Cache danh sách ID comment mà user đã like
    private val _likedCommentIds = MutableStateFlow<Set<String>>(emptySet())

    private var currentPostId: String = ""

    fun loadComments(postId: String) {
        currentPostId = postId
        val userId = preferencesManager.userId

        viewModelScope.launch {
            _isLoading.value = true

            // 1. Lấy danh sách ID đã like (One-shot)
            val initialLikedIds = repository.getLikedCommentIds(userId, postId).toSet()
            _likedCommentIds.value = initialLikedIds

            // 2. Combine: Danh sách gốc (Realtime) + Trạng thái Like Local (Realtime UI)
            repository.getCommentsFlow(postId, userId)
                .combine(_likedCommentIds) { commentList, likedSet ->
                    commentList.map { root ->
                        // Logic Mapping cho Root Comment
                        val isRootLikedLocal = root.id in likedSet

                        val mappedRoot = root.copy(isLiked = isRootLikedLocal)

                        // Logic Mapping cho Replies
                        val mappedReplies = root.replies.map { child ->
                            val isChildLikedLocal = child.id in likedSet
                            child.copy(isLiked = isChildLikedLocal)
                        }

                        mappedRoot.copy(replies = mappedReplies)
                    }
                }
                .collect { mappedComments ->
                    _comments.value = mappedComments
                    _isLoading.value = false
                }
        }
    }

    // Trong CommentViewModel.kt

    fun sendComment(content: String, parentId: String? = null) {
        if (content.isBlank() || currentPostId.isEmpty()) return

        val userId = preferencesManager.userId
        val userName = preferencesManager.userName
        val userAvatar = preferencesManager.userAvatar

        // --- KIỂM TRA LOG ---
        android.util.Log.d("CommentDebug", "User: $userId, Post: $currentPostId")

        viewModelScope.launch {
            val result = repository.addComment(
                postId = currentPostId,
                content = content,
                parentId = parentId,
                userId = userId,
                userName = userName,
                userAvatar = userAvatar
            )

            // --- XỬ LÝ KẾT QUẢ ---
            if (result.isSuccess) {
                android.util.Log.d("CommentDebug", "Success")
                // Có thể thêm logic clear text ở đây nếu muốn ViewModel quản lý
            } else {
                val error = result.exceptionOrNull()
                android.util.Log.e("CommentDebug", "Failed: ${error?.message}")
                error?.printStackTrace() // In chi tiết lỗi ra Logcat
            }
        }
    }

    fun toggleLike(comment: Comment) {
        val userId = preferencesManager.userId
        val isCurrentlyLiked = comment.isLiked

        // Optimistic Update Local State ngay lập tức
        val newSet = if (isCurrentlyLiked) _likedCommentIds.value - comment.id else _likedCommentIds.value + comment.id
        _likedCommentIds.value = newSet

        viewModelScope.launch {
            repository.toggleLikeComment(currentPostId, comment.id, userId, isCurrentlyLiked)
        }
    }
}