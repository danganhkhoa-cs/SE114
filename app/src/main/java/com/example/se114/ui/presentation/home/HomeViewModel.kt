package com.example.se114.ui.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.se114.data.dummy.DummyPostData
import com.example.se114.data.dummy.Post
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

// Định nghĩa các loại thông báo để UI tự lấy chuỗi từ PreferencesManager
enum class HomeMessage {
    NONE, SAVED, UNSAVED, HIDDEN, REPORT_SUCCESS
}

data class HomeUiState(
    // Dữ liệu gốc
    val allPosts: List<Post> = emptyList(),

    // Các tập hợp ID để quản lý trạng thái nhanh
    val savedPostIds: Set<Int> = emptySet(),
    val hiddenPostIds: Set<Int> = emptySet(),

    // Trạng thái bộ lọc UI
    val selectedTabIndex: Int = 0, // 0: Everyone, 1: For You
    val isShowingSavedPosts: Boolean = false,

    // Kết quả cuối cùng hiển thị lên màn hình (đã qua xử lý lọc)
    val displayedPosts: List<Post> = emptyList(),

    // Trạng thái khác
    val notificationUnreadCount: Int = 5,
    val currentMessage: HomeMessage = HomeMessage.NONE
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    // Nếu bạn chưa setup DI cho PreferencesManager, ta không inject vào đây
    // mà để UI xử lý phần text. ViewModel chỉ quản lý logic data.
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        // Giả lập load data từ Dummy
        val initialPosts = DummyPostData.posts
        // Giả sử bài có id=2 đã được lưu trước đó
        val initialSaved = setOf(2)

        _uiState.update {
            it.copy(
                allPosts = initialPosts,
                savedPostIds = initialSaved
            )
        }
        calculateDisplayedPosts()
    }

    // --- LOGIC CHUYỂN TAB & FILTER ---

    fun onTabSelected(index: Int) {
        _uiState.update {
            it.copy(
                selectedTabIndex = index,
                // Reset filter saved khi về tab Everyone (tab 0)
                isShowingSavedPosts = if (index == 0) false else it.isShowingSavedPosts
            )
        }
        calculateDisplayedPosts()
    }

    fun toggleSavedFilter() {
        _uiState.update { it.copy(isShowingSavedPosts = !it.isShowingSavedPosts) }
        calculateDisplayedPosts()
    }

    // --- LOGIC TƯƠNG TÁC BÀI VIẾT (LIKE, SAVE, HIDE) ---

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
        _uiState.update { state ->
            val newSavedIds = state.savedPostIds.toMutableSet()
            val message: HomeMessage

            if (newSavedIds.contains(postId)) {
                newSavedIds.remove(postId)
                message = HomeMessage.UNSAVED // Hoặc NONE nếu không muốn hiện thông báo khi bỏ lưu
            } else {
                newSavedIds.add(postId)
                message = HomeMessage.SAVED
            }

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

    // --- CORE LOGIC: TÍNH TOÁN DANH SÁCH HIỂN THỊ ---

    private fun calculateDisplayedPosts() {
        val state = _uiState.value

        val filtered = when (state.selectedTabIndex) {
            0 -> { // Everyone
                state.allPosts.filter { it.id !in state.hiddenPostIds }
            }
            1 -> { // For You
                if (state.isShowingSavedPosts) {
                    state.allPosts.filter { it.id in state.savedPostIds && it.id !in state.hiddenPostIds }
                } else {
                    state.allPosts.filter { it.id !in state.hiddenPostIds }
                }
            }
            else -> emptyList()
        }

        _uiState.update { it.copy(displayedPosts = filtered) }
    }
}