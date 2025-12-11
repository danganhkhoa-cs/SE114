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

data class SettingsUiState(
    // Block List
    val blockedUsers: List<String> = listOf("user123", "anonymous_user", "john_doe"),

    // Dialog Visibility
    val isShowingLanguageDialog: Boolean = false,
    val isShowingThemeDialog: Boolean = false,
    val isShowingBlockListDialog: Boolean = false,
    val isShowingDeleteAccountDialog: Boolean = false,

    // Delete Account Logic State
    val deleteStep: Int = 1,
    val deleteError: String = "",
    val isDeleting: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    // Không inject PreferencesManager, UI sẽ tự handle logic lưu prefs đơn giản
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState = _uiState.asStateFlow()

    // --- Block List Logic ---
    fun unblockUser(username: String) {
        _uiState.update { state ->
            state.copy(blockedUsers = state.blockedUsers - username)
        }
    }

    // --- Dialog Controls ---
    fun showLanguageDialog() { _uiState.update { it.copy(isShowingLanguageDialog = true) } }
    fun hideLanguageDialog() { _uiState.update { it.copy(isShowingLanguageDialog = false) } }

    fun showThemeDialog() { _uiState.update { it.copy(isShowingThemeDialog = true) } }
    fun hideThemeDialog() { _uiState.update { it.copy(isShowingThemeDialog = false) } }

    fun showBlockListDialog() { _uiState.update { it.copy(isShowingBlockListDialog = true) } }
    fun hideBlockListDialog() { _uiState.update { it.copy(isShowingBlockListDialog = false) } }

    fun showDeleteAccountDialog() {
        // Reset state khi mở dialog
        _uiState.update { it.copy(isShowingDeleteAccountDialog = true, deleteStep = 1, deleteError = "") }
    }
    fun hideDeleteAccountDialog() { _uiState.update { it.copy(isShowingDeleteAccountDialog = false) } }

    // --- Delete Account Logic ---

    fun onDeleteNextStep(currentStep: Int, passwordInput: String = "") {
        _uiState.update { it.copy(deleteError = "") } // Clear error trước

        when (currentStep) {
            1 -> {
                // Chuyển từ cảnh báo sang nhập pass
                _uiState.update { it.copy(deleteStep = 2) }
            }
            2 -> {
                // Validate password (giả lập)
                if (passwordInput.isEmpty()) {
                    _uiState.update { it.copy(deleteError = "Password required") } // Chuỗi này UI sẽ localize lại nếu cần
                } else {
                    // Giả sử check pass ok -> sang bước 3
                    _uiState.update { it.copy(deleteStep = 3) }
                }
            }
        }
    }

    fun onDeletePreviousStep() {
        _uiState.update { state ->
            if (state.deleteStep > 1) {
                state.copy(deleteStep = state.deleteStep - 1, deleteError = "")
            } else {
                state
            }
        }
    }

    fun confirmDeleteAccount(confirmTextInput: String, onSuccess: () -> Unit) {
        if (confirmTextInput != "DELETE") {
            _uiState.update { it.copy(deleteError = "Please type DELETE exactly") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }

            // Giả lập gọi API xóa tài khoản
            delay(2000)

            _uiState.update { it.copy(isDeleting = false, isShowingDeleteAccountDialog = false) }
            onSuccess() // Callback để UI điều hướng về màn hình Login hoặc thoát app
        }
    }
}