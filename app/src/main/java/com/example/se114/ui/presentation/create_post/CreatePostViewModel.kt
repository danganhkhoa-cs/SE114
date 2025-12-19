package com.example.se114.ui.presentation.create_post

import androidx.lifecycle.ViewModel
import com.example.se114.data.DummyPostData
import com.example.se114.data.PostType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
class CreatePostViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(CreatePostUiState())
    val uiState = _uiState.asStateFlow()

    // --- DUMMY DATA FOR DROPDOWNS ---
    val cities = listOf("Hồ Chí Minh", "Hà Nội", "Đà Nẵng", "Cần Thơ", "Hải Phòng")

    val districtsMap = mapOf(
        "Hồ Chí Minh" to listOf("Quận 1", "Quận 3", "Quận 5", "Quận 10", "Bình Thạnh", "Tân Bình", "Thủ Đức"),
        "Hà Nội" to listOf("Ba Đình", "Hoàn Kiếm", "Tây Hồ", "Cầu Giấy", "Đống Đa"),
        "Đà Nẵng" to listOf("Hải Châu", "Thanh Khê", "Sơn Trà", "Ngũ Hành Sơn"),
        "Cần Thơ" to listOf("Ninh Kiều", "Bình Thủy", "Cái Răng"),
        "Hải Phòng" to listOf("Hồng Bàng", "Ngô Quyền", "Lê Chân")
    )

    val categories = listOf(
        "Cứu hộ khẩn cấp", "Y tế", "Sửa chữa", "Vệ sinh", "Gia sư", "Vận chuyển", "Tìm đồ thất lạc", "Khác"
    )

    fun onContentChanged(newContent: String) {
        _uiState.update { it.copy(content = newContent) }
    }

    fun onCitySelected(city: String) {
        _uiState.update {
            it.copy(
                selectedCity = city,
                selectedDistrict = "" // Reset district khi đổi city
            )
        }
    }

    fun onDistrictSelected(district: String) {
        _uiState.update { it.copy(selectedDistrict = district) }
    }

    fun onCategorySelected(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun onPostTypeChanged(type: PostType) {
        _uiState.update { it.copy(selectedPostType = type) }
    }

    fun createPost() {
        val currentState = _uiState.value
        if (currentState.content.isBlank()
            || currentState.selectedCity.isEmpty()
            || currentState.selectedDistrict.isEmpty()
            || currentState.selectedCategory.isEmpty()) return

        _uiState.update { it.copy(isLoading = true) }

        // Format lại nội dung để gửi vào Dummy
        // Thực tế sẽ gửi từng field lên server
        val fullContent = currentState.content
        val city = currentState.selectedCity
        val district = currentState.selectedDistrict
        val category = currentState.selectedCategory
        val type = currentState.selectedPostType


        DummyPostData.addPost(
            fullContent,
            district,
            city,
            category,
            null,
            type
        )

        _uiState.update {
            it.copy(
                isLoading = false,
                isSuccess = true,
                content = "",
                selectedCity = "",
                selectedDistrict = "",
                selectedCategory = ""
            )
        }
    }

    fun resetState() {
        _uiState.update { it.copy(isSuccess = false) }
    }
}