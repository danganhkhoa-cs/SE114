package com.example.se114.ui.presentation.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.se114.local.PreferencesManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
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
    private val storage: FirebaseStorage,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        // 1. Load từ Local trước cho nhanh
        _uiState.update {
            it.copy(
                userName = preferencesManager.userName,
                userBio = preferencesManager.userBio
            )
        }

        // 2. Gọi hàm đồng bộ dữ liệu mới nhất từ Server
        fetchLatestUserInfo()
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun fetchLatestUserInfo() {
        val uid = preferencesManager.userId
        if (uid.isBlank()) return

        viewModelScope.launch {
            try {
                val document = firestore.collection("users").document(uid).get().await()

                if (document.exists()) {
                    val name = document.getString("name") ?: ""
                    val bio = document.getString("bio") ?: ""
                    val avatarUrl = document.getString("avatar_url") ?: ""
                    val phone = document.getString("phone") ?: ""

                    preferencesManager.userName = name
                    preferencesManager.userBio = bio
                    // Cập nhật lại avatar nếu trên server có thay đổi (hoặc rỗng)
                    preferencesManager.userAvatar = avatarUrl
                    preferencesManager.userPhone = phone

                    _uiState.update {
                        it.copy(
                            userName = name,
                            userBio = bio
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- HÀM MỚI: XỬ LÝ TẤT CẢ THAY ĐỔI KHI NHẤN SAVE ---
    fun saveProfileChanges(
        newName: String,
        newBio: String,
        newAvatarUri: Uri?,
        isAvatarDeleted: Boolean
    ) {
        val uid = preferencesManager.userId
        if (uid.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Không tìm thấy người dùng. Vui lòng đăng nhập lại.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                var finalAvatarUrl = preferencesManager.userAvatar

                // 1. Xử lý logic Avatar
                if (isAvatarDeleted) {
                    // Nếu người dùng chọn xóa avatar -> Set thành rỗng
                    finalAvatarUrl = ""
                } else if (newAvatarUri != null) {
                    // Nếu người dùng chọn ảnh mới -> Upload lên Storage
                    val storageRef = storage.reference.child("users/$uid/avatar_${System.currentTimeMillis()}.jpg")
                    storageRef.putFile(newAvatarUri).await()
                    finalAvatarUrl = storageRef.downloadUrl.await().toString()
                }
                // Nếu không xóa và không chọn ảnh mới -> Giữ nguyên finalAvatarUrl cũ

                // 2. Cập nhật Firestore một lần duy nhất
                val updates = mapOf(
                    "name" to newName,
                    "bio" to newBio,
                    "avatar_url" to finalAvatarUrl
                )
                firestore.collection("users").document(uid).update(updates).await()

                // 3. Cập nhật Local Preferences
                preferencesManager.userName = newName
                preferencesManager.userBio = newBio
                preferencesManager.userAvatar = finalAvatarUrl

                // 4. Update UI State & Đóng Dialog
                _uiState.update {
                    it.copy(
                        userName = newName,
                        userBio = newBio,
                        isShowingEditProfileDialog = false,
                        isLoading = false
                    )
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update { it.copy(isLoading = false, errorMessage = "Lỗi cập nhật: ${e.message}") }
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