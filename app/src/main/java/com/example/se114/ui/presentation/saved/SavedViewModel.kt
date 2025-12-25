package com.example.se114.ui.presentation.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.se114.data.Post
import com.example.se114.data.PostEventBus
import com.example.se114.data.PostType
import com.example.se114.data.PostUpdateEvent
import com.example.se114.data.repository.PostRepository
import com.example.se114.local.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SavedUiState(
    val allSavedPosts: List<Post> = emptyList(), // Danh sách gốc đầy đủ
    val displayedPosts: List<Post> = emptyList(), // Danh sách đang hiển thị theo Tab
    val selectedTabIndex: Int = 0, // 0: Hỗ trợ, 1: Dịch vụ
    val isLoading: Boolean = false
)

@HiltViewModel
class SavedViewModel @Inject constructor(
    private val repository: PostRepository,
    val preferencesManager: PreferencesManager,
    private val postEventBus: PostEventBus // [MỚI] Inject EventBus
) : ViewModel() {

    private val _uiState = MutableStateFlow(SavedUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // [MỚI] Lắng nghe sự kiện từ EventBus ngay khi khởi tạo
        observeBusEvents()
    }

    // [MỚI] Hàm lắng nghe thay đổi từ Home
    private fun observeBusEvents() {
        viewModelScope.launch {
            postEventBus.events.collect { event ->
                updateLocalPost(event.postId, event.isLiked, event.likeCount, event.commentCount)
            }
        }
    }

    // [MỚI] Cập nhật list local mà không cần gọi API
    private fun updateLocalPost(postId: String, isLiked: Boolean?, likeCount: Int?, commentCount: Int?) {
        val currentAll = _uiState.value.allSavedPosts
        // Nếu bài viết không có trong list đã lưu thì bỏ qua
        if (currentAll.none { it.id == postId }) return

        _uiState.update { state ->
            val updatedAll = state.allSavedPosts.map { post ->
                if (post.id == postId) {
                    post.copy(
                        isLiked = isLiked ?: post.isLiked,
                        likeCount = likeCount ?: post.likeCount,
                        commentCount = commentCount ?: post.commentCount
                    )
                } else post
            }
            state.copy(allSavedPosts = updatedAll)
        }
        // Tính toán lại hiển thị
        calculateDisplayedPosts()
    }

    fun loadSavedPosts() {
        // [FIX UX] Nếu đã có dữ liệu rồi thì không load lại nữa (trừ khi pull-to-refresh)
        if (_uiState.value.allSavedPosts.isNotEmpty()) return

        val userId = preferencesManager.userId
        if (userId.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val savedIdsResult = repository.getUserSavedPostIds(userId)
            if (savedIdsResult.isSuccess) {
                val savedIds = savedIdsResult.getOrThrow()
                if (savedIds.isEmpty()) {
                    _uiState.update { it.copy(allSavedPosts = emptyList(), displayedPosts = emptyList(), isLoading = false) }
                } else {
                    val postsResult = repository.getPostsByIds(savedIds, currentUserId = userId)
                    if (postsResult.isSuccess) {
                        val posts = postsResult.getOrThrow()
                        _uiState.update { it.copy(allSavedPosts = posts, isLoading = false) }
                        calculateDisplayedPosts() // Phân loại ra Tab hiện tại
                    } else {
                        _uiState.update { it.copy(isLoading = false) }
                    }
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // [MỚI] Chuyển Tab
    fun onTabSelected(index: Int) {
        _uiState.update { it.copy(selectedTabIndex = index) }
        calculateDisplayedPosts()
    }

    // [MỚI] Logic lọc bài viết theo Tab
    private fun calculateDisplayedPosts() {
        val state = _uiState.value
        val targetType = if (state.selectedTabIndex == 0) PostType.SUPPORT.name else PostType.SERVICE.name
        val filtered = state.allSavedPosts.filter { it.type == targetType }
        _uiState.update { it.copy(displayedPosts = filtered) }
    }

    fun onToggleLike(postId: String) {
        val currentList = _uiState.value.allSavedPosts
        val currentPost = currentList.find { it.id == postId } ?: return
        val isCurrentlyLiked = currentPost.isLiked
        val userId = preferencesManager.userId

        // 1. Update Local UI ngay lập tức
        val newLikeStatus = !isCurrentlyLiked
        val newLikeCount = (if (isCurrentlyLiked) currentPost.likeCount - 1 else currentPost.likeCount + 1).coerceAtLeast(0)

        updateLocalPost(postId, newLikeStatus, newLikeCount, null)

        // 2. Bắn Event cho Home biết
        viewModelScope.launch {
            postEventBus.emitEvent(PostUpdateEvent(postId, isLiked = newLikeStatus, likeCount = newLikeCount))
            repository.toggleLikePost(postId, userId, isCurrentlyLiked)
        }
    }

    fun onUnsave(postId: String) {
        val userId = preferencesManager.userId
        viewModelScope.launch {
            val result = repository.toggleSavePost(postId, userId, true)
            if (result.isSuccess) {
                _uiState.update { state ->
                    val newAll = state.allSavedPosts.filter { it.id != postId }
                    state.copy(allSavedPosts = newAll)
                }
                calculateDisplayedPosts()
            }
        }
    }
}