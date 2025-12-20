package com.example.se114.ui.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.se114.local.PreferencesManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class ProfileUiState(
    val userName: String = "",
    val userBio: String = "",

    val isShowingEditProfileDialog: Boolean = false,
    val isShowingLogoutDialog: Boolean = false,

    val isLoggingOut: Boolean = false,
    val logoutSuccess: Boolean = false,

    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(
                userName = preferencesManager.userName,
                userBio = preferencesManager.userBio
            )
        }
    }

    fun updateUserInfo(name: String, bio: String) {
        // Sửa lỗi: Lấy UID từ Prefs thay vì Auth
        val uid = preferencesManager.userId
        if (uid.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Không tìm thấy người dùng. Vui lòng đăng nhập lại.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val updates = mapOf(
                    "name" to name,
                    "bio" to bio
                )

                firestore.collection("users").document(uid)
                    .update(updates)
                    .await()

                preferencesManager.userName = name
                preferencesManager.userBio = bio

                _uiState.update {
                    it.copy(
                        userName = name,
                        userBio = bio,
                        isShowingEditProfileDialog = false,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Lỗi cập nhật: ${e.message}"
                    )
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoggingOut = true, isShowingLogoutDialog = false) }
            try {
                auth.signOut()
                preferencesManager.clearUserData()
                _uiState.update { it.copy(isLoggingOut = false, logoutSuccess = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoggingOut = false, errorMessage = "Đăng xuất thất bại") }
            }
        }
    }

    fun showEditProfileDialog() {
        _uiState.update {
            it.copy(
                isShowingEditProfileDialog = true,
                userName = preferencesManager.userName,
                userBio = preferencesManager.userBio
            )
        }
    }
    fun hideEditProfileDialog() { _uiState.update { it.copy(isShowingEditProfileDialog = false) } }

    fun showLogoutDialog() { _uiState.update { it.copy(isShowingLogoutDialog = true) } }
    fun hideLogoutDialog() { _uiState.update { it.copy(isShowingLogoutDialog = false) } }
}