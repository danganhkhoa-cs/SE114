package com.example.se114.ui.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.se114.data.Post
import com.example.se114.data.PostType
import com.example.se114.data.repository.PostRepository
import com.example.se114.local.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class HomeMessage { NONE, SAVED, UNSAVED, HIDDEN, REPORT_SUCCESS }

data class HomeUiState(
    val allPosts: List<Post> = emptyList(),
    val savedPostIds: Set<String> = emptySet(), // Set<String>
    val hiddenPostIds: Set<String> = emptySet(),
    val selectedTabIndex: Int = 0,
    val displayedPosts: List<Post> = emptyList(),
    val notificationUnreadCount: Int = 5,
    val currentMessage: HomeMessage = HomeMessage.NONE,
    val isRefreshing: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: PostRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadPosts()
    }

    fun loadPosts() {
        val userId = preferencesManager.userId
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }

            val postsDeferred = async { repository.getPosts(currentUserId = userId) }
            val savedIdsDeferred = async { repository.getUserSavedPostIds(userId) }

            val postsResult = postsDeferred.await()
            val savedIdsResult = savedIdsDeferred.await()

            if (postsResult.isSuccess) {
                val posts = postsResult.getOrDefault(emptyList())
                val savedIds = savedIdsResult.getOrDefault(emptyList()).toSet()
                _uiState.update { it.copy(allPosts = posts, savedPostIds = savedIds, isRefreshing = false) }
                calculateDisplayedPosts()
            } else {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun onRefresh() { loadPosts() }

    fun onTabSelected(index: Int) {
        _uiState.update { it.copy(selectedTabIndex = index) }
        calculateDisplayedPosts()
    }

    fun onToggleLike(postId: String) {
        val currentPost = _uiState.value.allPosts.find { it.id == postId } ?: return
        val isCurrentlyLiked = currentPost.isLiked
        val userId = preferencesManager.userId

        _uiState.update { state ->
            val updatedPosts = state.allPosts.map { post ->
                if (post.id == postId) {
                    val newCount = if (post.isLiked) post.likeCount - 1 else post.likeCount + 1
                    post.copy(isLiked = !post.isLiked, likeCount = newCount.coerceAtLeast(0))
                } else post
            }
            state.copy(allPosts = updatedPosts)
        }
        calculateDisplayedPosts()

        viewModelScope.launch { repository.toggleLikePost(postId, userId, isCurrentlyLiked) }
    }

    fun onToggleSave(postId: String) {
        val isSaved = _uiState.value.savedPostIds.contains(postId)
        val userId = preferencesManager.userId

        _uiState.update { state ->
            val newSavedIds = state.savedPostIds.toMutableSet()
            if (isSaved) newSavedIds.remove(postId) else newSavedIds.add(postId)
            val message = if (!isSaved) HomeMessage.SAVED else HomeMessage.UNSAVED
            state.copy(savedPostIds = newSavedIds, currentMessage = message)
        }
        viewModelScope.launch { repository.toggleSavePost(postId, userId, isSaved) }
    }

    fun onHidePost(postId: String) {
        _uiState.update { state ->
            val newHiddenIds = state.hiddenPostIds.toMutableSet()
            newHiddenIds.add(postId)
            state.copy(hiddenPostIds = newHiddenIds, currentMessage = HomeMessage.HIDDEN)
        }
        calculateDisplayedPosts()
    }

    fun onReportSubmitted() { _uiState.update { it.copy(currentMessage = HomeMessage.REPORT_SUCCESS) } }
    fun onMessageShown() { _uiState.update { it.copy(currentMessage = HomeMessage.NONE) } }

    private fun calculateDisplayedPosts() {
        val state = _uiState.value
        val targetType = if (state.selectedTabIndex == 0) PostType.SUPPORT.name else PostType.SERVICE.name
        val filtered = state.allPosts.filter { it.type == targetType && it.id !in state.hiddenPostIds }
        _uiState.update { it.copy(displayedPosts = filtered) }
    }
}