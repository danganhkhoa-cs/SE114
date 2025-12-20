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

data class SettingsUiState(
    val blockedUsers: List<String> = emptyList(),
    val isShowingLanguageDialog: Boolean = false,
    val isShowingThemeDialog: Boolean = false,
    val isShowingBlockListDialog: Boolean = false,
    val isShowingDeleteAccountDialog: Boolean = false,
    val deleteStep: Int = 1,
    val deleteError: String = "",
    val isDeleting: Boolean = false
)

@HiltViewModel
class SettingsScreenViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    // Biến này lưu mật khẩu đã được xác thực (Verified) để dùng cho bước xóa cuối cùng
    private var validPasswordForDeletion: String = ""

    // --- VERIFY PASSWORD (KIỂM TRA KÉP) ---
    fun onDeleteNextStep(currentStep: Int, input: String) {
        if (currentStep == 2) {
            // Step 2: Verify Password
            if (input.isBlank()) {
                _uiState.update { it.copy(deleteError = "Vui lòng nhập mật khẩu") }
                return
            }
            verifyPasswordAndDeleteStep(input)
        } else if (currentStep == 1) {
            _uiState.update { it.copy(deleteStep = 2, deleteError = "") }
        }
    }

    private fun verifyPasswordAndDeleteStep(password: String) {
        val uid = preferencesManager.userId
        val email = preferencesManager.userEmail
        if (uid.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, deleteError = "") }
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
                    validPasswordForDeletion = password // LƯU MẬT KHẨU ĐÚNG
                    _uiState.update { it.copy(isDeleting = false, deleteStep = 3, deleteError = "") }
                } else {
                    _uiState.update { it.copy(isDeleting = false, deleteError = "Mật khẩu không đúng") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isDeleting = false, deleteError = "Lỗi kết nối: ${e.message}") }
            }
        }
    }

    // --- CONFIRM & DELETE (DÙNG MẬT KHẨU ĐÚNG ĐỂ XÓA) ---
    fun confirmDeleteAccount(confirmText: String, onSuccess: () -> Unit) {
        if (confirmText != "DELETE") {
            _uiState.update { it.copy(deleteError = "Vui lòng nhập chính xác chữ DELETE") }
            return
        }

        val uid = preferencesManager.userId
        val email = preferencesManager.userEmail

        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true, deleteError = "") }
            try {
                // 1. XÓA FIREBASE AUTH (Dùng mật khẩu đã verify để ép đăng nhập lại)
                if (email.isNotEmpty() && validPasswordForDeletion.isNotEmpty()) {
                    try {
                        val authResult = auth.signInWithEmailAndPassword(email, validPasswordForDeletion).await()
                        authResult.user?.delete()?.await()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        // Nếu xóa Auth lỗi, vẫn tiếp tục xóa Firestore để không bị kẹt
                    }
                } else {
                    // Fallback
                    try { auth.currentUser?.delete()?.await() } catch (e: Exception) {}
                }

                // 2. XÓA FIRESTORE
                firestore.collection("users").document(uid).delete().await()

                // 3. CLEANUP
                auth.signOut()
                preferencesManager.clearUserData()

                _uiState.update { it.copy(isDeleting = false, isShowingDeleteAccountDialog = false) }
                onSuccess()

            } catch (e: Exception) {
                _uiState.update { it.copy(isDeleting = false, deleteError = "Xóa thất bại: ${e.message}") }
            }
        }
    }

    // --- Helpers ---
    fun showDeleteAccountDialog() {
        validPasswordForDeletion = ""
        _uiState.update { it.copy(isShowingDeleteAccountDialog = true, deleteStep = 1, deleteError = "") }
    }
    fun hideDeleteAccountDialog() { _uiState.update { it.copy(isShowingDeleteAccountDialog = false) } }
    fun onDeletePreviousStep() {
        _uiState.update {
            if (it.deleteStep > 1) it.copy(deleteStep = it.deleteStep - 1, deleteError = "") else it
        }
    }

    // --- Other Settings ---
    fun updateLanguage(language: String) { preferencesManager.language = language; hideLanguageDialog() }
    fun updateTheme(isDarkMode: Boolean) { preferencesManager.isDarkMode = isDarkMode; hideThemeDialog() }
    fun unblockUser(user: String) {
        val newList = _uiState.value.blockedUsers.toMutableList().apply { remove(user) }
        _uiState.update { it.copy(blockedUsers = newList) }
    }
    fun showLanguageDialog() { _uiState.update { it.copy(isShowingLanguageDialog = true) } }
    fun hideLanguageDialog() { _uiState.update { it.copy(isShowingLanguageDialog = false) } }
    fun showThemeDialog() { _uiState.update { it.copy(isShowingThemeDialog = true) } }
    fun hideThemeDialog() { _uiState.update { it.copy(isShowingThemeDialog = false) } }
    fun showBlockListDialog() { _uiState.update { it.copy(isShowingBlockListDialog = true) } }
    fun hideBlockListDialog() { _uiState.update { it.copy(isShowingBlockListDialog = false) } }
}