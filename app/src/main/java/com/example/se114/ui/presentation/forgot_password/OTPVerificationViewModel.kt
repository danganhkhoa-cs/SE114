package com.example.se114.ui.presentation.forgot_password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OTPVerificationUiState(
    val otp: String = "",
    val otpError: String? = null,
    val isOTPVerified: Boolean = false,
    val newPassword: String = "",
    val newPasswordError: String? = null,
    val confirmPassword: String = "",
    val confirmPasswordError: String? = null,
    val isLoading: Boolean = false,
    val resetPasswordSuccess: Boolean = false,
    val hasLowercase: Boolean = false,
    val hasUppercase: Boolean = false,
    val hasNumber: Boolean = false,
    val hasMinimum8Chars: Boolean = false
)

@HiltViewModel
class OTPVerificationViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(OTPVerificationUiState())
    val uiState = _uiState.asStateFlow()


    private val validOTP = "456789"

    fun onOTPChange(otp: String) {

        if (otp.all { it.isDigit() } && otp.length <= 6) {
            _uiState.update { it.copy(otp = otp, otpError = null) }
        }
    }

    fun onNewPasswordChange(password: String) {
        _uiState.update {
            it.copy(
                newPassword = password,
                newPasswordError = null,
                hasLowercase = password.any { char -> char.isLowerCase() },
                hasUppercase = password.any { char -> char.isUpperCase() },
                hasNumber = password.any { char -> char.isDigit() },
                hasMinimum8Chars = password.length >= 8
            )
        }
    }

    fun onConfirmPasswordChange(confirmPass: String) {
        _uiState.update { it.copy(confirmPassword = confirmPass, confirmPasswordError = null) }
    }

    fun verifyOTP() {
        val state = _uiState.value

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }


            kotlinx.coroutines.delay(1500)


            if (state.otp == validOTP) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isOTPVerified = true,
                        otpError = null
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        otpError = "Invalid OTP code"
                    )
                }
            }
        }
    }

    fun resetPassword() {
        if (!validatePasswordInputs()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            kotlinx.coroutines.delay(2000)

            _uiState.update { it.copy(isLoading = false, resetPasswordSuccess = true) }
        }
    }

    private fun validatePasswordInputs(): Boolean {
        val state = _uiState.value
        var isValid = true

        if (!state.hasLowercase || !state.hasUppercase ||
            !state.hasNumber || !state.hasMinimum8Chars) {
            _uiState.update { it.copy(newPasswordError = "Password does not meet requirements") }
            isValid = false
        }

        if (state.newPassword != state.confirmPassword) {
            _uiState.update { it.copy(confirmPasswordError = "Passwords do not match") }
            isValid = false
        }

        if (state.newPassword.isEmpty()) {
            _uiState.update { it.copy(newPasswordError = "Password cannot be empty") }
            isValid = false
        }

        return isValid
    }
}