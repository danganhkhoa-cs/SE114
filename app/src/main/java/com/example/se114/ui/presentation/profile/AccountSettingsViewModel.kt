package com.example.se114.ui.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.se114.local.PreferencesManager
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
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

    // --- LOGIC ĐỔI MẬT KHẨU (KIỂM TRA KÉP) ---
    fun changePassword(currentPass: String, newPass: String) {
        val uid = preferencesManager.userId
        val email = preferencesManager.userEmail

        if (uid.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                var isVerified = false

                // BƯỚC 1: Kiểm tra Firestore (Nguồn ưu tiên)
                val doc = firestore.collection("users").document(uid).get().await()
                val dbPass = doc.getString("password")

                if (dbPass != null && dbPass == currentPass) {
                    isVerified = true
                } else {
                    // BƯỚC 2: Nếu Firestore sai, thử check với Firebase Auth (Nguồn dự phòng)
                    try {
                        if (email.isNotEmpty()) {
                            auth.signInWithEmailAndPassword(email, currentPass).await()
                            isVerified = true
                        }
                    } catch (e: Exception) {
                        isVerified = false
                    }
                }

                if (isVerified) {
                    // 1. Cập nhật Firestore
                    firestore.collection("users").document(uid)
                        .update("password", newPass)
                        .await()

                    // 2. Cố gắng cập nhật Auth (để đồng bộ)
                    try {
                        val user = auth.currentUser ?: auth.signInWithEmailAndPassword(email, currentPass).await().user
                        user?.updatePassword(newPass)?.await()
                    } catch (e: Exception) {
                        // Bỏ qua lỗi Auth update (quan trọng là Firestore đã xong)
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isShowingPasswordDialog = false,
                            successMessage = "Password changed successfully!"
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Incorrect current password") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Error: ${e.message}") }
            }
        }
    }

    // --- LOGIC XÁC THỰC SĐT (KIỂM TRA KÉP) ---
    fun verifyPasswordForPhoneChange(password: String) {
        val uid = preferencesManager.userId
        val email = preferencesManager.userEmail

        if (uid.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                var isVerified = false

                // 1. Check Firestore
                val doc = firestore.collection("users").document(uid).get().await()
                val dbPass = doc.getString("password")

                if (dbPass != null && dbPass == password) {
                    isVerified = true
                } else {
                    // 2. Check Auth
                    try {
                        if (email.isNotEmpty()) {
                            auth.signInWithEmailAndPassword(email, password).await()
                            isVerified = true
                        }
                    } catch (e: Exception) {
                        isVerified = false
                    }
                }

                if (isVerified) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isShowingPasswordVerifyDialog = false,
                            isShowingPhoneDialog = true,
                            errorMessage = null
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Incorrect password") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Error: ${e.message}") }
            }
        }
    }

    fun updatePhone(newPhone: String) {
        val uid = preferencesManager.userId
        if (uid.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                firestore.collection("users").document(uid).update("phone", newPhone).await()
                preferencesManager.userPhone = newPhone
                _uiState.update {
                    it.copy(phone = newPhone, isShowingPhoneDialog = false, isLoading = false, successMessage = "Phone updated!")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun showPasswordDialog() { _uiState.update { it.copy(isShowingPasswordDialog = true, errorMessage = null) } }
    fun hidePasswordDialog() { _uiState.update { it.copy(isShowingPasswordDialog = false) } }
    fun showPhoneDialog() { _uiState.update { it.copy(isShowingPhoneDialog = true, errorMessage = null) } }
    fun hidePhoneDialog() { _uiState.update { it.copy(isShowingPhoneDialog = false) } }
    fun showPasswordVerifyDialog() { _uiState.update { it.copy(isShowingPasswordVerifyDialog = true, errorMessage = null) } }
    fun hidePasswordVerifyDialog() { _uiState.update { it.copy(isShowingPasswordVerifyDialog = false) } }
    fun clearMessages() { _uiState.update { it.copy(errorMessage = null, successMessage = null) } }
}