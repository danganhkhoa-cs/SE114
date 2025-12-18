package com.example.se114.ui.presentation.home

import androidx.lifecycle.ViewModel
import com.example.se114.data.DummyPostData
import com.example.se114.data.Post
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
    val isShowingSavedPosts: Boolean = false,
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
        // Load data từ Dummy
        val initialPosts = DummyPostData.posts
        // Load danh sách đã lưu từ Dummy (Global state)
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
        _uiState.update {
            it.copy(
                selectedTabIndex = index,
                isShowingSavedPosts = if (index == 0) false else it.isShowingSavedPosts
            )
        }
        calculateDisplayedPosts()
    }

    fun toggleSavedFilter() {
        _uiState.update { it.copy(isShowingSavedPosts = !it.isShowingSavedPosts) }
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

    // --- CẬP NHẬT LOGIC LƯU ---
    fun onToggleSave(postId: Int) {
        // 1. Cập nhật vào kho dữ liệu chung (Dummy)
        DummyPostData.toggleSave(postId)

        // 2. Cập nhật UI State hiện tại dựa trên dữ liệu mới
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
        val filtered = when (state.selectedTabIndex) {
            0 -> state.allPosts.filter { it.id !in state.hiddenPostIds }
            1 -> if (state.isShowingSavedPosts) {
                state.allPosts.filter { it.id in state.savedPostIds && it.id !in state.hiddenPostIds }
            } else {
                state.allPosts.filter { it.id !in state.hiddenPostIds }
            }
            else -> emptyList()
        }
        _uiState.update { it.copy(displayedPosts = filtered) }
    }
}