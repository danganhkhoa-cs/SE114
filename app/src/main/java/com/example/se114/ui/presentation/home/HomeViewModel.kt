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
import com.example.se114.data.PostEventBus
import com.example.se114.data.PostUpdateEvent
import com.example.se114.data.Report

enum class HomeMessage { NONE, SAVED, UNSAVED, HIDDEN, REPORT_SUCCESS, REPORT_DUPLICATE, REPORT_ERROR }

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
    private val preferencesManager: PreferencesManager,
    private val postEventBus: PostEventBus
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadPosts()
        observeSavedPosts()
        observeBusEvents()
    }

    private fun observeSavedPosts() {
        viewModelScope.launch {
            repository.savedPostIdsFlow.collect { savedIds ->
                _uiState.update { it.copy(savedPostIds = savedIds) }
            }
        }
    }

    private fun observeBusEvents() {
        viewModelScope.launch {
            postEventBus.events.collect { event ->
                // Update local list (Logic y hệt SavedViewModel)
                val currentAll = _uiState.value.allPosts
                if (currentAll.none { it.id == event.postId }) return@collect

                _uiState.update { state ->
                    val updatedPosts = state.allPosts.map { post ->
                        if (post.id == event.postId) {
                            post.copy(
                                isLiked = event.isLiked ?: post.isLiked,
                                likeCount = event.likeCount ?: post.likeCount,
                                commentCount = event.commentCount ?: post.commentCount
                            )
                        } else post
                    }
                    state.copy(allPosts = updatedPosts)
                }
                calculateDisplayedPosts()
            }
        }
    }

    fun loadPosts() {
        val userId = preferencesManager.userId
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }

            // Repository fetch data và update vào Flow
            async { repository.getUserSavedPostIds(userId) }

            val postsDeferred = async { repository.getPosts(currentUserId = userId) }
            val postsResult = postsDeferred.await()

            if (postsResult.isSuccess) {
                val posts = postsResult.getOrDefault(emptyList())
                _uiState.update { it.copy(allPosts = posts, isRefreshing = false) }
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

        // Update UI Local
        val newLikeStatus = !isCurrentlyLiked
        val newLikeCount = (if (isCurrentlyLiked) currentPost.likeCount - 1 else currentPost.likeCount + 1).coerceAtLeast(0)

        _uiState.update { state ->
            val updatedPosts = state.allPosts.map { post ->
                if (post.id == postId) {
                    post.copy(isLiked = newLikeStatus, likeCount = newLikeCount)
                } else post
            }
            state.copy(allPosts = updatedPosts)
        }
        calculateDisplayedPosts()

        // Bắn sự kiện & Gọi API
        viewModelScope.launch {
            postEventBus.emitEvent(PostUpdateEvent(postId, isLiked = newLikeStatus, likeCount = newLikeCount))
            repository.toggleLikePost(postId, userId, isCurrentlyLiked)
        }
    }

    fun onToggleSave(postId: String) {
        val isSaved = _uiState.value.savedPostIds.contains(postId)
        val userId = preferencesManager.userId

        // Cập nhật UI Home
        _uiState.update { state ->
            val message = if (!isSaved) HomeMessage.SAVED else HomeMessage.UNSAVED
            state.copy(currentMessage = message)
        }
        val newSavedStatus = !isSaved

        viewModelScope.launch {
            // 1. Bắn tin cho SavedScreen biết
            postEventBus.emitEvent(PostUpdateEvent(postId, isSaved = newSavedStatus))

            // 2. Gọi API
            repository.toggleSavePost(postId, userId, isSaved)
        }
    }

    fun onHidePost(postId: String) {
        _uiState.update { state ->
            val newHiddenIds = state.hiddenPostIds.toMutableSet()
            newHiddenIds.add(postId)
            state.copy(hiddenPostIds = newHiddenIds, currentMessage = HomeMessage.HIDDEN)
        }
        calculateDisplayedPosts()
    }

    fun onSubmitReport(postId: String, reason: String, description: String) {
        val userId = preferencesManager.userId
        if (userId.isEmpty()) return

        viewModelScope.launch {
            val report = Report(
                reporterId = userId,
                postId = postId,
                reason = reason,
                description = description
            )

            val result = repository.createReport(report)

            if (result.isSuccess) {
                _uiState.update { it.copy(currentMessage = HomeMessage.REPORT_SUCCESS) }
            } else {
                val error = result.exceptionOrNull()
                if (error?.message == "duplicate") {
                    _uiState.update { it.copy(currentMessage = HomeMessage.REPORT_DUPLICATE) }
                } else {
                    android.util.Log.e("REPORT_ERROR", "Lỗi gửi report: ${error?.message}")
                    error?.printStackTrace()
                    _uiState.update { it.copy(currentMessage = HomeMessage.REPORT_ERROR) }
                }
            }
        }
    }
    fun onMessageShown() { _uiState.update { it.copy(currentMessage = HomeMessage.NONE) } }

    private fun calculateDisplayedPosts() {
        val state = _uiState.value
        val targetType = if (state.selectedTabIndex == 0) PostType.SUPPORT.name else PostType.SERVICE.name
        val filtered = state.allPosts.filter { it.type == targetType && it.id !in state.hiddenPostIds }
        _uiState.update { it.copy(displayedPosts = filtered) }
    }
}