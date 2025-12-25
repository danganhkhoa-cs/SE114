package com.example.se114.ui.presentation.saved

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.se114.data.Post
import com.example.se114.data.repository.PostRepository
import com.example.se114.local.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SavedUiState(
    val savedPosts: List<Post> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class SavedViewModel @Inject constructor(
    private val repository: PostRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(SavedUiState())
    val uiState = _uiState.asStateFlow()

    fun loadSavedPosts() {
        val userId = preferencesManager.userId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Lấy tất cả bài viết và danh sách ID đã lưu để lọc
            val postsDeferred = async { repository.getPosts() }
            val savedIdsDeferred = async { repository.getUserSavedPostIds(userId) }

            val postsResult = postsDeferred.await()
            val savedIdsResult = savedIdsDeferred.await()

            if (postsResult.isSuccess && savedIdsResult.isSuccess) {
                val allPosts = postsResult.getOrThrow()
                val savedIds = savedIdsResult.getOrThrow().toSet()

                val filtered = allPosts.filter { it.id in savedIds }
                _uiState.update { it.copy(savedPosts = filtered, isLoading = false) }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun onUnsave(postId: String) {
        val userId = preferencesManager.userId
        viewModelScope.launch {
            repository.toggleSavePost(postId, userId, true) // Xóa khỏi DB
            loadSavedPosts() // Refresh danh sách
        }
    }
}