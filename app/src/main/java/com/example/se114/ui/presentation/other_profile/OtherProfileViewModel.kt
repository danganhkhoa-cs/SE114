package com.example.se114.ui.presentation.other_profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OtherProfileUiState(
    val userId: String = "",
    val userName: String = "",
    val userBio: String = "",
    val address: String = "",
    val gender: String = "",
    val job: String = "",
    val joinedDate: String = "",
    val rating: Float = 0.0f,
    val reviewCount: Int = 0,
    val isFollow: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class OtherProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(OtherProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // Lấy userId từ navigation arguments
        val userId = savedStateHandle.get<String>("userId")
        if (userId != null) {
            loadUserProfile(userId)
        }
    }

    private fun loadUserProfile(userId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, userId = userId) }

            // Giả lập call API lấy thông tin người dùng khác
            delay(1000)

            // Dữ liệu Mock
            _uiState.update {
                it.copy(
                    userName = "Sarah Nguyen",
                    userBio = "Helping others is the way we help ourselves ❤️ | Volunteer",
                    address = "District 7, Ho Chi Minh City",
                    gender = "Female",
                    job = "Nurse",
                    joinedDate = "October 2023",
                    rating = 4.7f, // Điểm đánh giá (đã tính trung bình từ việc giúp đỡ)
                    reviewCount = 128,
                    isFollow = false,
                    isLoading = false
                )
            }
        }
    }

    fun toggleFriendStatus() {
        _uiState.update { it.copy(isFollow = !it.isFollow) }
    }
}