package com.example.se114.ui.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.se114.data.model.ChatStatus
import com.example.se114.data.model.UserSummary
import com.example.se114.local.PreferencesManager
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
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
    // Thay đổi: Lưu UserSummary để hiển thị Avatar/Tên
    val blockedUsers: List<UserSummary> = emptyList(),
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
                // 1. Lấy danh sách ID bị chặn
                val doc = firestore.collection("users").document(uid).get().await()
                val blockedIds = doc.get("blockedUsers") as? List<String> ?: emptyList()

                if (blockedIds.isEmpty()) {
                    _uiState.update { it.copy(blockedUsers = emptyList()) }
                    return@launch
                }

                // 2. Fetch thông tin chi tiết (Avatar, Name) từ ID
                val userList = mutableListOf<UserSummary>()
                // Firestore whereIn giới hạn 10 phần tử, cần chia nhỏ nếu danh sách dài
                blockedIds.chunked(10).forEach { chunk ->
                    val snapshot = firestore.collection("users")
                        .whereIn(FieldPath.documentId(), chunk)
                        .get()
                        .await()

                    val users = snapshot.documents.map { userDoc ->
                        val name = userDoc.getString("name") ?: "Unknown"
                        val avatar = userDoc.getString("avatar_url") ?: ""
                        val phone = userDoc.getString("phone") ?: ""
                        val email = userDoc.getString("email") ?: ""
                        UserSummary(userDoc.id, name, avatar, phone, email)
                    }
                    userList.addAll(users)
                }

                _uiState.update { it.copy(blockedUsers = userList) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- LOGIC UNBLOCK MỚI ---
    fun unblockUser(userId: String) {
        viewModelScope.launch {
            val myId = preferencesManager.userId
            try {
                // 1. Xóa khỏi danh sách blockedUsers trong User Document
                firestore.collection("users").document(myId)
                    .update("blockedUsers", FieldValue.arrayRemove(userId))
                    .await()

                // 2. Cập nhật UI Local
                val newList = _uiState.value.blockedUsers.toMutableList()
                newList.removeAll { it.uid == userId }
                _uiState.update { it.copy(blockedUsers = newList) }

                // 3. QUAN TRỌNG: Tìm cuộc trò chuyện và mở khóa (Set status = ACCEPTED)
                // Để sau khi unblock có thể nhắn tin lại ngay, nhưng Friendship vẫn là NONE (phải kết bạn lại)
                val convSnapshot = firestore.collection("conversations")
                    .whereArrayContains("participants", myId)
                    .get()
                    .await()

                val conversation = convSnapshot.documents.find { doc ->
                    val participants = doc.get("participants") as? List<String> ?: emptyList()
                    participants.contains(userId)
                }

                if (conversation != null) {
                    firestore.collection("conversations").document(conversation.id)
                        .update("status", ChatStatus.ACCEPTED) // Reset về ACCEPTED để chat được
                        .await()
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- VERIFY PASSWORD (LOGIC CŨ) ---
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
                val doc = firestore.collection("users").document(uid).get().await()
                val dbPass = doc.getString("password")

                if (dbPass != null && dbPass == password) {
                    isVerified = true
                } else {
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
                    _uiState.update { it.copy(isDeleting = false, deleteError = preferencesManager.getString("password_incorrect")) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isDeleting = false, deleteError = "Lỗi kết nối: ${e.message}") }
            }
        }
    }

    // --- CONFIRM & DELETE ---
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
                handleChatDeletion(uid)
                firestore.collection("users").document(uid).delete().await()

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

    fun updateLanguage(language: String) { preferencesManager.language = language; hideLanguageDialog() }
    fun updateTheme(isDarkMode: Boolean) { preferencesManager.isDarkMode = isDarkMode; hideThemeDialog() }

    fun showLanguageDialog() { _uiState.update { it.copy(isShowingLanguageDialog = true) } }
    fun hideLanguageDialog() { _uiState.update { it.copy(isShowingLanguageDialog = false) } }
    fun showThemeDialog() { _uiState.update { it.copy(isShowingThemeDialog = true) } }
    fun hideThemeDialog() { _uiState.update { it.copy(isShowingThemeDialog = false) } }

    // Gọi hàm loadBlockedUsers mỗi khi mở dialog để đảm bảo data mới nhất
    fun showBlockListDialog() {
        loadBlockedUsers()
        _uiState.update { it.copy(isShowingBlockListDialog = true) }
    }
    fun hideBlockListDialog() { _uiState.update { it.copy(isShowingBlockListDialog = false) } }
}