package com.example.se114.ui.presentation.home

import androidx.lifecycle.viewModelScope
import com.example.se114.data.Post
import com.example.se114.data.PostEventBus
import com.example.se114.data.PostType
import com.example.se114.data.PostUpdateEvent
import com.example.se114.data.repository.PostRepository
import com.example.se114.local.PreferencesManager
import com.example.se114.ui.presentation.components.BasePostViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val allPosts: List<Post> = emptyList(),
    val savedPostIds: Set<String> = emptySet(),
    val hiddenPostIds: Set<String> = emptySet(),
    val displayedPosts: List<Post> = emptyList(),
    val selectedTabIndex: Int = 0,
    val notificationUnreadCount: Int = 5,
    val isRefreshing: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    repository: PostRepository,
    preferencesManager: PreferencesManager,
    postEventBus: PostEventBus
) : BasePostViewModel(repository, preferencesManager, postEventBus) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadPosts()
        observeSavedPosts()
    }

    private fun observeSavedPosts() {
        viewModelScope.launch {
            repository.savedPostIdsFlow.collect { savedIds ->
                _uiState.update { it.copy(savedPostIds = savedIds) }
            }
        }
    }

    fun loadPosts() {
        val userId = preferencesManager.userId
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            // Gọi song song
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

    // Override từ Base: Xử lý ẩn bài viết
    override fun onHidePost(postId: String) {
        super.onHidePost(postId) // Gọi cha để hiện thông báo
        _uiState.update { state ->
            state.copy(hiddenPostIds = state.hiddenPostIds + postId)
        }
        calculateDisplayedPosts()
    }

    // Override từ Base: Xử lý khi có sự kiện từ Bus (Like/Save/Comment)
    override fun handlePostUpdate(event: PostUpdateEvent) {
        val currentAll = _uiState.value.allPosts
        if (currentAll.none { it.id == event.postId }) return

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

    private fun calculateDisplayedPosts() {
        val state = _uiState.value
        val targetType = if (state.selectedTabIndex == 0) PostType.SUPPORT.name else PostType.SERVICE.name
        val filtered = state.allPosts.filter { it.type == targetType && it.id !in state.hiddenPostIds }
        _uiState.update { it.copy(displayedPosts = filtered) }
    }
}