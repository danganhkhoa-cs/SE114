package com.example.se114.ui.presentation.home

import androidx.lifecycle.ViewModel
import com.example.se114.data.DummyPostData
import com.example.se114.data.Post
import com.example.se114.data.PostType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

enum class HomeMessage {
    NONE, SAVED, UNSAVED, HIDDEN, REPORT_SUCCESS
}

data class HomeUiState(
    val allPosts: List<Post> = emptyList(),
    val savedPostIds: Set<Int> = emptySet(),
    val hiddenPostIds: Set<Int> = emptySet(),
    val selectedTabIndex: Int = 0,
    val displayedPosts: List<Post> = emptyList(),
    val notificationUnreadCount: Int = 5,
    val currentMessage: HomeMessage = HomeMessage.NONE
)

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        val initialPosts = DummyPostData.posts
        val initialSaved = DummyPostData.savedPostIds.toSet()

        _uiState.update {
            it.copy(
                allPosts = initialPosts,
                savedPostIds = initialSaved
            )
        }
        calculateDisplayedPosts()
    }

    fun onTabSelected(index: Int) {
        _uiState.update { it.copy(selectedTabIndex = index) }
        calculateDisplayedPosts()
    }

    fun onToggleLike(postId: Int) {
        _uiState.update { currentState ->
            val updatedPosts = currentState.allPosts.map { post ->
                if (post.id == postId) {
                    val newLikeStatus = !post.isLiked
                    post.copy(
                        isLiked = newLikeStatus,
                        likeCount = if (newLikeStatus) post.likeCount + 1 else post.likeCount - 1
                    )
                } else {
                    post
                }
            }
            currentState.copy(allPosts = updatedPosts)
        }
        calculateDisplayedPosts()
    }

    fun onToggleSave(postId: Int) {
        DummyPostData.toggleSave(postId)

        _uiState.update { state ->
            val newSavedIds = DummyPostData.savedPostIds.toSet()
            val message = if (newSavedIds.contains(postId)) HomeMessage.SAVED else HomeMessage.UNSAVED

            state.copy(savedPostIds = newSavedIds, currentMessage = message)
        }
        calculateDisplayedPosts()
    }

    fun onHidePost(postId: Int) {
        _uiState.update { state ->
            val newHiddenIds = state.hiddenPostIds.toMutableSet()
            newHiddenIds.add(postId)
            state.copy(hiddenPostIds = newHiddenIds, currentMessage = HomeMessage.HIDDEN)
        }
        calculateDisplayedPosts()
    }

    fun onReportSubmitted() {
        _uiState.update { it.copy(currentMessage = HomeMessage.REPORT_SUCCESS) }
    }

    fun onMessageShown() {
        _uiState.update { it.copy(currentMessage = HomeMessage.NONE) }
    }

    private fun calculateDisplayedPosts() {
        val state = _uiState.value

        // Logic lọc bài viết: Dựa trên Tab và Loại bài viết
        val filteredByType = when (state.selectedTabIndex) {
            0 -> state.allPosts.filter { it.type == PostType.SUPPORT } // Tab Support
            1 -> state.allPosts.filter { it.type == PostType.SERVICE } // Tab Service
            else -> state.allPosts
        }

        // Lọc tiếp các bài bị ẩn
        val finalFiltered = filteredByType.filter { post ->
             post.id !in state.hiddenPostIds
        }

        _uiState.update { it.copy(displayedPosts = finalFiltered) }
    }
}