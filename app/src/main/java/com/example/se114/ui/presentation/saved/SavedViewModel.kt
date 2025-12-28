package com.example.se114.ui.presentation.saved

import androidx.lifecycle.viewModelScope
import com.example.se114.data.Post
import com.example.se114.data.PostEventBus
import com.example.se114.data.PostType
import com.example.se114.data.PostUpdateEvent
import com.example.se114.data.repository.PostRepository
import com.example.se114.local.PreferencesManager
import com.example.se114.ui.presentation.components.BasePostViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SavedUiState(
    val allSavedPosts: List<Post> = emptyList(),
    val displayedPosts: List<Post> = emptyList(),
    val selectedTabIndex: Int = 0,
    val isLoading: Boolean = false
)

@HiltViewModel
class SavedViewModel @Inject constructor(
    repository: PostRepository,
    preferencesManager: PreferencesManager,
    postEventBus: PostEventBus
) : BasePostViewModel(repository, preferencesManager, postEventBus) {

    private val _uiState = MutableStateFlow(SavedUiState())
    val uiState = _uiState.asStateFlow()

    fun loadSavedPosts() {
        if (_uiState.value.allSavedPosts.isNotEmpty()) return

        val userId = preferencesManager.userId
        if (userId.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val savedIdsResult = repository.getUserSavedPostIds(userId)

            if (savedIdsResult.isSuccess) {
                val savedIds = savedIdsResult.getOrThrow()
                if (savedIds.isNotEmpty()) {
                    val postsResult = repository.getPostsByIds(savedIds, currentUserId = userId)
                    if (postsResult.isSuccess) {
                        _uiState.update { it.copy(allSavedPosts = postsResult.getOrThrow(), isLoading = false) }
                        calculateDisplayedPosts()
                    }
                } else {
                    _uiState.update { it.copy(allSavedPosts = emptyList(), displayedPosts = emptyList(), isLoading = false) }
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onTabSelected(index: Int) {
        _uiState.update { it.copy(selectedTabIndex = index) }
        calculateDisplayedPosts()
    }

    // Xử lý sự kiện Bus (Quan trọng: Xử lý Unsave tại đây)
    override fun handlePostUpdate(event: PostUpdateEvent) {
        // Nếu sự kiện là Unsave -> Xóa khỏi danh sách Saved ngay lập tức
        if (event.isSaved == false) {
            _uiState.update { state ->
                state.copy(allSavedPosts = state.allSavedPosts.filter { it.id != event.postId })
            }
            calculateDisplayedPosts()
            return
        }

        // Nếu là Like/Comment -> Cập nhật thông tin
        _uiState.update { state ->
            val updatedPosts = state.allSavedPosts.map { post ->
                if (post.id == event.postId) {
                    post.copy(
                        isLiked = event.isLiked ?: post.isLiked,
                        likeCount = event.likeCount ?: post.likeCount,
                        commentCount = event.commentCount ?: post.commentCount
                    )
                } else post
            }
            state.copy(allSavedPosts = updatedPosts)
        }
        calculateDisplayedPosts()
    }

    private fun calculateDisplayedPosts() {
        val state = _uiState.value
        val targetType = if (state.selectedTabIndex == 0) PostType.SUPPORT.name else PostType.SERVICE.name
        val filtered = state.allSavedPosts.filter { it.type == targetType }
        _uiState.update { it.copy(displayedPosts = filtered) }
    }
}