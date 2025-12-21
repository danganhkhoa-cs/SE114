package com.example.se114.ui.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.se114.local.PreferencesManager
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class AccountSettingsUiState(
    val email: String = "",
    val phone: String = "",

    val isShowingPasswordDialog: Boolean = false,
    val isShowingPhoneDialog: Boolean = false,
    val isShowingPasswordVerifyDialog: Boolean = false,

    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class AccountSettingsViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountSettingsUiState())
    val uiState = _uiState.asStateFlow()

    fun refreshData() {
        _uiState.update {
            it.copy(
                email = preferencesManager.userEmail,
                phone = preferencesManager.userPhone
            )
        }
    }

    // --- LOGIC ĐỔI MẬT KHẨU (CHUẨN FIREBASE AUTH) ---
    fun changePassword(currentPass: String, newPass: String) {
        val user = auth.currentUser
        val email = user?.email ?: preferencesManager.userEmail

        if (user == null || email.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // BƯỚC 1: Re-authenticate (Xác thực lại người dùng bằng mật khẩu cũ)
                // Đây là cách an toàn nhất thay vì so sánh chuỗi thủ công
                val credential = EmailAuthProvider.getCredential(email, currentPass)
                user.reauthenticate(credential).await()

                // BƯỚC 2: Nếu xác thực thành công, tiến hành đổi mật khẩu trên Auth
                user.updatePassword(newPass).await()

                // KHÔNG LƯU MẬT KHẨU VÀO FIRESTORE NỮA

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isShowingPasswordDialog = false,
                        successMessage = "Đổi mật khẩu thành công!"
                    )
                }
            } catch (e: Exception) {
                val msg = if (e is FirebaseAuthInvalidCredentialsException) {
                    "Mật khẩu hiện tại không đúng"
                } else {
                    e.message ?: "Đổi mật khẩu thất bại"
                }
                _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
            }
        }
    }

    // --- LOGIC XÁC THỰC SĐT (CHUẨN FIREBASE AUTH) ---
    fun verifyPasswordForPhoneChange(password: String) {
        val user = auth.currentUser
        val email = user?.email ?: preferencesManager.userEmail

        if (user == null) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // Xác thực lại bằng mật khẩu người dùng nhập vào
                val credential = EmailAuthProvider.getCredential(email, password)
                user.reauthenticate(credential).await()

                // Nếu không lỗi (Exception) nghĩa là mật khẩu đúng -> Cho phép đổi SĐT
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isShowingPasswordVerifyDialog = false,
                        isShowingPhoneDialog = true, // Mở dialog nhập SĐT mới
                        errorMessage = null
                    )
                }
            } catch (e: Exception) {
                val msg = if (e is FirebaseAuthInvalidCredentialsException) {
                    "Mật khẩu không đúng"
                } else {
                    e.message
                }
                _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
            }
        }
    }

    fun updatePhone(newPhone: String) {
        val uid = preferencesManager.userId
        if (uid.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                // Chỉ cập nhật số điện thoại vào Firestore (Thông tin Profile)
                firestore.collection("users").document(uid).update("phone", newPhone).await()

                // Cập nhật Local Preference
                preferencesManager.userPhone = newPhone

                _uiState.update {
                    it.copy(phone = newPhone, isShowingPhoneDialog = false, isLoading = false, successMessage = "Cập nhật số điện thoại thành công!")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    // Các hàm Helper giữ nguyên
    fun showPasswordDialog() { _uiState.update { it.copy(isShowingPasswordDialog = true, errorMessage = null) } }
    fun hidePasswordDialog() { _uiState.update { it.copy(isShowingPasswordDialog = false) } }
    fun showPhoneDialog() { _uiState.update { it.copy(isShowingPhoneDialog = true, errorMessage = null) } }
    fun hidePhoneDialog() { _uiState.update { it.copy(isShowingPhoneDialog = false) } }
    fun showPasswordVerifyDialog() { _uiState.update { it.copy(isShowingPasswordVerifyDialog = true, errorMessage = null) } }
    fun hidePasswordVerifyDialog() { _uiState.update { it.copy(isShowingPasswordVerifyDialog = false) } }
    fun clearMessages() { _uiState.update { it.copy(errorMessage = null, successMessage = null) } }
}