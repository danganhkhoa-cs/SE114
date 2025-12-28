package com.example.se114.ui.presentation.create_post

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.se114.data.Post
import com.example.se114.data.PostType
import com.example.se114.data.SelectionData
import com.example.se114.data.repository.PostRepository
import com.example.se114.local.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CreatePostUiState(
    val content: String = "",
    val selectedCity: String = "",
    val selectedDistrict: String = "",
    val selectedCategory: String = "",
    val selectedPostType: PostType = PostType.SUPPORT,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false
)

@HiltViewModel
class CreatePostViewModel @Inject constructor(
    private val repository: PostRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState = _uiState.asStateFlow()

    // Lấy danh sách KEY thành phố (city_hcm, city_bd...)
    val cities = SelectionData.locations.keys.toList().sorted()

    fun getDistricts(city: String): List<String> {
        return SelectionData.locations[city] ?: emptyList()
    }

    // Lấy danh sách KEY danh mục (cat_repair, cat_emergency...)
    fun getCategories(type: PostType): List<String> {
        return SelectionData.getCategories(type)
    }

    fun onContentChanged(newContent: String) {
        _uiState.update { it.copy(content = newContent) }
    }

    fun onCitySelected(city: String) {
        _uiState.update { it.copy(selectedCity = city, selectedDistrict = "") }
    }

    fun onDistrictSelected(district: String) {
        _uiState.update { it.copy(selectedDistrict = district) }
    }

    fun onCategorySelected(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun onPostTypeChanged(type: PostType) {
        _uiState.update { it.copy(selectedPostType = type, selectedCategory = "") }
    }

    fun createPost() {
        val currentState = _uiState.value
        if (currentState.content.isBlank() || currentState.selectedCity.isEmpty() ||
            currentState.selectedDistrict.isEmpty() || currentState.selectedCategory.isEmpty()) return

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val newPost = Post(
                userId = preferencesManager.userId,
                userName = preferencesManager.userName,
                userAvatar = preferencesManager.userAvatar,
                content = currentState.content,
                district = currentState.selectedDistrict,
                city = currentState.selectedCity,
                category = currentState.selectedCategory,
                type = currentState.selectedPostType.name
            )

            val result = repository.createPost(newPost)

            if (result.isSuccess) {
                _uiState.update {
                    it.copy(isLoading = false, isSuccess = true, content = "", selectedCity = "", selectedDistrict = "", selectedCategory = "")
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun resetState() {
        _uiState.update { it.copy(isSuccess = false) }
    }
}