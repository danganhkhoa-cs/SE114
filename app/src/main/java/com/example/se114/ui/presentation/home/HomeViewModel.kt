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
import kotlinx.coroutines.flow.combine
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
    val isRefreshing: Boolean = false,

    // Filter States
    val filterCity: String = "",
    val filterDistrict: String = "",
    val filterCategory: String = ""
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
        val userId = preferencesManager.userId
        if (userId.isNotEmpty()) {
            repository.startListeningToUnreadNotifications(userId)
        }
        observeSavedPosts()
        observeTotalBadge(userId)
    }

    private fun observeSavedPosts() {
        viewModelScope.launch {
            repository.savedPostIdsFlow.collect { savedIds ->
                _uiState.update { it.copy(savedPostIds = savedIds) }
            }
        }
    }

    private fun observeTotalBadge(userId: String) {
        viewModelScope.launch {
            combine(
                repository.unreadCountFlow,
                repository.getSystemTotalCountFlow(),
                repository.getReadSystemIdsFlow(userId)
            ) { socialUnread, systemTotal, readIds ->
                val systemUnread = (systemTotal - readIds.size).coerceAtLeast(0)
                socialUnread + systemUnread
            }.collect { totalUnread ->
                _uiState.update { it.copy(notificationUnreadCount = totalUnread) }
            }
        }
    }

    fun loadPosts() {
        val userId = preferencesManager.userId
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            async { repository.getUserSavedPostIds(userId) }
            val postsDeferred = async { repository.getPosts(currentUserId = userId) }

            val postsResult = postsDeferred.await()
            if (postsResult.isSuccess) {
                // Sửa lỗi getOrDefault: Dùng getOrNull() ?: emptyList() an toàn hơn
                val posts = postsResult.getOrNull() ?: emptyList()
                _uiState.update { it.copy(allPosts = posts, isRefreshing = false) }
                calculateDisplayedPosts()
            } else {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun onRefresh() { loadPosts() }

    fun onTabSelected(index: Int) {
        _uiState.update { it.copy(selectedTabIndex = index, filterCategory = "") }
        calculateDisplayedPosts()
    }

    // Hàm Apply Filter
    fun applyFilter(city: String, district: String, category: String) {
        _uiState.update {
            it.copy(
                filterCity = city,
                filterDistrict = district,
                filterCategory = category
            )
        }
        calculateDisplayedPosts()
    }

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

        // --- LOGIC LỌC ---
        // Lưu ý: Nếu file Post.kt chưa có field cityKey, districtKey, categoryKey thì sẽ báo đỏ.
        // Bạn cần thêm các trường này vào data class Post.
        val filtered = state.allPosts.filter { post ->
            val matchType = post.type == targetType
            val matchHidden = post.id !in state.hiddenPostIds

            // Dùng safe call (?.) và elvis (?: "") để tránh lỗi null pointer
            val matchCity = state.filterCity.isEmpty() || post.city == state.filterCity
            val matchDistrict = state.filterDistrict.isEmpty() || post.district == state.filterDistrict
            val matchCategory = state.filterCategory.isEmpty() || post.category == state.filterCategory

            matchType && matchHidden && matchCity && matchDistrict && matchCategory
        }

        _uiState.update { it.copy(displayedPosts = filtered) }
    }
}