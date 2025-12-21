package com.example.se114.ui.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.se114.local.PreferencesManager
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
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

    private var validPasswordForDeletion: String = ""

    init {
        loadBlockedUsers()
    }

    private fun loadBlockedUsers() {
        val uid = preferencesManager.userId
        if (uid.isBlank()) return
        viewModelScope.launch {
            try {
                val doc = firestore.collection("users").document(uid).get().await()
                val blocked = doc.get("blockedUsers") as? List<String> ?: emptyList()
                _uiState.update { it.copy(blockedUsers = blocked) }
            } catch (e: Exception) { }
        }
    }

    // --- VERIFY PASSWORD (LOGIC CŨ: nhận step và input) ---
    fun onDeleteNextStep(currentStep: Int, input: String) {
        if (currentStep == 2) {
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
                    // 2. Check Auth (Fallback)
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
                    validPasswordForDeletion = password
                    _uiState.update { it.copy(isDeleting = false, deleteStep = 3, deleteError = "") }
                } else {
                    _uiState.update { it.copy(isDeleting = false, deleteError = "Mật khẩu không đúng") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isDeleting = false, deleteError = "Lỗi kết nối: ${e.message}") }
            }
        }
    }

    // --- CONFIRM & DELETE (CÓ CALLBACK onSuccess) ---
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
                // 1. XÓA DỮ LIỆU FIRESTORE
                handleChatDeletion(uid)
                firestore.collection("users").document(uid).delete().await()

                // 2. XÓA AUTH
                val user = auth.currentUser
                if (user != null) {
                    try {
                        if (email.isNotEmpty() && validPasswordForDeletion.isNotEmpty()) {
                            val credential = EmailAuthProvider.getCredential(email, validPasswordForDeletion)
                            user.reauthenticate(credential).await()
                        }
                        user.delete().await()
                    } catch (e: Exception) { e.printStackTrace() }
                }

                // 3. THÀNH CÔNG -> Gọi Callback (Sẽ gọi onLogout để clear data + navigate)
                // QUAN TRỌNG: KHÔNG gọi preferencesManager.clearUserData() ở đây
                _uiState.update { it.copy(isDeleting = false, isShowingDeleteAccountDialog = false) }
                onSuccess()

            } catch (e: Exception) {
                _uiState.update { it.copy(isDeleting = false, deleteError = "Xóa thất bại: ${e.message}") }
            }
        }
    }

    private suspend fun handleChatDeletion(uid: String) {
        try {
            val conversationsSnapshot = firestore.collection("conversations")
                .whereArrayContains("participants", uid)
                .get()
                .await()

            for (doc in conversationsSnapshot.documents) {
                val convId = doc.id
                val participants = doc.get("participants") as? List<String> ?: emptyList()

                firestore.collection("conversations").document(convId)
                    .update("deletedAccountUsers", FieldValue.arrayUnion(uid))
                    .await()

                val updatedDoc = firestore.collection("conversations").document(convId).get().await()
                val deletedAccountUsers = updatedDoc.get("deletedAccountUsers") as? List<String> ?: emptyList()

                if (participants.isNotEmpty() && deletedAccountUsers.containsAll(participants)) {
                    firestore.collection("conversations").document(convId).delete()
                }
            }
        } catch (e: Exception) { e.printStackTrace() }
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
        viewModelScope.launch {
            val uid = preferencesManager.userId
            firestore.collection("users").document(uid).update("blockedUsers", FieldValue.arrayRemove(user))
            val newList = _uiState.value.blockedUsers.toMutableList().apply { remove(user) }
            _uiState.update { it.copy(blockedUsers = newList) }
        }
    }
    fun showLanguageDialog() { _uiState.update { it.copy(isShowingLanguageDialog = true) } }
    fun hideLanguageDialog() { _uiState.update { it.copy(isShowingLanguageDialog = false) } }
    fun showThemeDialog() { _uiState.update { it.copy(isShowingThemeDialog = true) } }
    fun hideThemeDialog() { _uiState.update { it.copy(isShowingThemeDialog = false) } }
    fun showBlockListDialog() { _uiState.update { it.copy(isShowingBlockListDialog = true) } }
    fun hideBlockListDialog() { _uiState.update { it.copy(isShowingBlockListDialog = false) } }
}