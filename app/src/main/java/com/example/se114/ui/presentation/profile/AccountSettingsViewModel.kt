package com.example.se114.ui.presentation.profile

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class AccountSettingsUiState(
    val email: String = "",
    val phone: String = "",

    // Dialog visibility states
    val isShowingPasswordDialog: Boolean = false,
    val isShowingPhoneDialog: Boolean = false,
    val isShowingPasswordVerifyDialog: Boolean = false
)

@HiltViewModel
class AccountSettingsViewModel @Inject constructor(
    // Không Inject PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountSettingsUiState())
    val uiState = _uiState.asStateFlow()

    fun setInitialData(email: String, phone: String) {
        _uiState.update { it.copy(email = email, phone = phone) }
    }

    // --- Phone Updates ---

    fun updatePhone(newPhone: String) {
        _uiState.update { it.copy(phone = newPhone, isShowingPhoneDialog = false) }
    }

    // --- Dialog Controls ---

    fun showPasswordDialog() {
        _uiState.update { it.copy(isShowingPasswordDialog = true) }
    }

    fun hidePasswordDialog() {
        _uiState.update { it.copy(isShowingPasswordDialog = false) }
    }

    fun showPhoneDialog() {
        _uiState.update { it.copy(isShowingPhoneDialog = true) }
    }

    fun hidePhoneDialog() {
        _uiState.update { it.copy(isShowingPhoneDialog = false) }
    }

    fun showPasswordVerifyDialog() {
        _uiState.update { it.copy(isShowingPasswordVerifyDialog = true) }
    }

    fun hidePasswordVerifyDialog() {
        _uiState.update { it.copy(isShowingPasswordVerifyDialog = false) }
    }
}