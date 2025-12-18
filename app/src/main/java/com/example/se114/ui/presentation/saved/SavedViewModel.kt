package com.example.se114.ui.presentation.saved

import androidx.lifecycle.ViewModel
import com.example.se114.data.DummyPostData
import com.example.se114.data.Post
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class SavedUiState(
    val savedPosts: List<Post> = emptyList()
)

@HiltViewModel
class SavedViewModel @Inject constructor() : ViewModel() {
    private val _uiState = MutableStateFlow(SavedUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadSavedPosts()
    }

    // Gọi hàm này mỗi khi vào màn hình để đảm bảo đồng bộ
    fun loadSavedPosts() {
        val allPosts = DummyPostData.posts
        val savedIds = DummyPostData.savedPostIds

        // Lọc ra các bài có ID nằm trong danh sách đã lưu
        val filtered = allPosts.filter { it.id in savedIds }

        _uiState.update { it.copy(savedPosts = filtered) }
    }

    fun onUnsave(postId: Int) {
        DummyPostData.toggleSave(postId)
        loadSavedPosts() // Refresh danh sách ngay lập tức
    }
}