package com.example.se114.ui.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val userName: String = "",
    val userBio: String = "",

    // Dialog Visibility
    val isShowingEditProfileDialog: Boolean = false,
    val isShowingLogoutDialog: Boolean = false,

    // Logout States
    val isLoggingOut: Boolean = false,
    val logoutSuccess: Boolean = false
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    // Không Inject PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    fun setInitialData(name: String, bio: String) {
        _uiState.update { it.copy(userName = name, userBio = bio) }
    }

    fun updateUserInfo(name: String, bio: String) {
        _uiState.update {
            it.copy(userName = name, userBio = bio, isShowingEditProfileDialog = false)
        }
    }

    // --- Dialog Controls ---
    fun showEditProfileDialog() { _uiState.update { it.copy(isShowingEditProfileDialog = true) } }
    fun hideEditProfileDialog() { _uiState.update { it.copy(isShowingEditProfileDialog = false) } }

    fun showLogoutDialog() { _uiState.update { it.copy(isShowingLogoutDialog = true) } }
    fun hideLogoutDialog() { _uiState.update { it.copy(isShowingLogoutDialog = false) } }

    // --- LOGIC LOGOUT (Kết nối Server) ---
    fun logout() {
        viewModelScope.launch {
            // 1. Bắt đầu loading, ẩn dialog
            _uiState.update { it.copy(isLoggingOut = true, isShowingLogoutDialog = false) }

            // 2. Giả lập gọi API Logout lên Server
            delay(1500)

            // 3. Báo thành công
            _uiState.update { it.copy(isLoggingOut = false, logoutSuccess = true) }
        }
    }

    // Reset trạng thái sau khi đã điều hướng xong (để tránh loop nếu quay lại)
    fun onLogoutHandled() {
        _uiState.update { it.copy(logoutSuccess = false) }
    }
}