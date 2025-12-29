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
    val isLoading: Boolean = false,

    // Filter States
    val filterCity: String = "",
    val filterDistrict: String = "",
    val filterCategory: String = ""
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
                // SỬA LỖI QUAN TRỌNG:
                // Dùng 'getOrNull() ?: emptySet()' để đảm bảo trả về Set<String>, tránh lỗi Type Mismatch với List.
                val savedIds = savedIdsResult.getOrNull() ?: emptySet()

                if (savedIds.isNotEmpty()) {
                    val postsResult = repository.getPostsByIds(postIds = savedIds.toList(), currentUserId = userId)
                    if (postsResult.isSuccess) {
                        // SỬA LỖI: Dùng 'getOrNull() ?: emptyList()' cho danh sách Post
                        val posts = postsResult.getOrNull() ?: emptyList()
                        _uiState.update { it.copy(allSavedPosts = posts, isLoading = false) }
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
        if (event.isSaved == false) {
            _uiState.update { state ->
                state.copy(allSavedPosts = state.allSavedPosts.filter { it.id != event.postId })
            }
            calculateDisplayedPosts()
            return
        }

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

        // --- LOGIC LỌC ---
        val filtered = state.allSavedPosts.filter { post ->
            val matchType = post.type == targetType

            // Safe call (?.)
            val matchCity = state.filterCity.isEmpty() || post.city == state.filterCity
            val matchDistrict = state.filterDistrict.isEmpty() || post.district == state.filterDistrict
            val matchCategory = state.filterCategory.isEmpty() || post.category == state.filterCategory

            matchType && matchCity && matchDistrict && matchCategory
        }

        _uiState.update { it.copy(displayedPosts = filtered) }
    }
}