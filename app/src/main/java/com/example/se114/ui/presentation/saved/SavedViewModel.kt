package com.example.se114.ui.presentation.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.se114.data.Post
import com.example.se114.data.repository.PostRepository
import com.example.se114.local.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SavedUiState(
    val savedPosts: List<Post> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class SavedViewModel @Inject constructor(
    private val repository: PostRepository,
    val preferencesManager: PreferencesManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(SavedUiState())
    val uiState = _uiState.asStateFlow()

    fun loadSavedPosts() {
        val userId = preferencesManager.userId
        if (userId.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // 1. Lấy danh sách ID đã lưu
            val savedIdsResult = repository.getUserSavedPostIds(userId)

            if (savedIdsResult.isSuccess) {
                val savedIds = savedIdsResult.getOrThrow()

                if (savedIds.isEmpty()) {
                    _uiState.update { it.copy(savedPosts = emptyList(), isLoading = false) }
                } else {
                    // 2. [CẬP NHẬT] Truyền userId vào để check Like status
                    val postsResult = repository.getPostsByIds(savedIds, currentUserId = userId)

                    if (postsResult.isSuccess) {
                        _uiState.update { it.copy(savedPosts = postsResult.getOrThrow(), isLoading = false) }
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // [MỚI] Hàm xử lý Like (Logic tương tự HomeViewModel)
    fun onToggleLike(postId: String) {
        val currentList = _uiState.value.savedPosts
        val currentPost = currentList.find { it.id == postId } ?: return
        val isCurrentlyLiked = currentPost.isLiked
        val userId = preferencesManager.userId

        // 1. Cập nhật UI ngay lập tức (Optimistic Update)
        _uiState.update { state ->
            val updatedPosts = state.savedPosts.map { post ->
                if (post.id == postId) {
                    val newCount = if (post.isLiked) post.likeCount - 1 else post.likeCount + 1
                    post.copy(isLiked = !post.isLiked, likeCount = newCount.coerceAtLeast(0))
                } else post
            }
            state.copy(savedPosts = updatedPosts)
        }

        // 2. Gọi Repository để lưu xuống DB
        viewModelScope.launch {
            repository.toggleLikePost(postId, userId, isCurrentlyLiked)
        }
    }

    fun onUnsave(postId: String) {
        val userId = preferencesManager.userId
        viewModelScope.launch {
            val result = repository.toggleSavePost(postId, userId, true)

            if (result.isSuccess) {
                _uiState.update { state ->
                    state.copy(savedPosts = state.savedPosts.filter { it.id != postId })
                }
            }
        }
    }
}