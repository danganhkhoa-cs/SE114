package com.example.se114.ui.presentation.forgot_password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.se114.local.PreferencesManager
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
    val hasMinimum8Chars: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class OTPVerificationViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val firestore: FirebaseFirestore // Inject Firestore
) : ViewModel() {

    private val _uiState = MutableStateFlow(OTPVerificationUiState())
    val uiState = _uiState.asStateFlow()

    fun onOTPChange(otp: String) {
        if (otp.all { it.isDigit() } && otp.length <= 6) {
            _uiState.update { it.copy(otp = otp, otpError = null, errorMessage = null) }
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
        val correctOTP = preferencesManager.getOTPForReset()

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            delay(1000)

            if (correctOTP != null && state.otp == correctOTP) {
                _uiState.update { it.copy(isLoading = false, isOTPVerified = true, otpError = null) }
            } else {
                _uiState.update { it.copy(isLoading = false, otpError = "Mã OTP không đúng") }
            }
        }
    }

    fun resetPassword() {
        if (!validatePasswordInputs()) return

        val email = preferencesManager.getEmailForReset()
        if (email == null) {
            _uiState.update { it.copy(errorMessage = "Không tìm thấy email cần reset") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                // 1. Tìm User trong Firestore bằng Email
                val querySnapshot = firestore.collection("users")
                    .whereEqualTo("email", email)
                    .get()
                    .await()

                if (!querySnapshot.isEmpty) {
                    val document = querySnapshot.documents[0]

                    // 2. Cập nhật field 'password' trong Firestore
                    // (Đây là cách bypass: lưu pass plaintext vào DB để LoginViewModel kiểm tra sau này)
                    // LƯU Ý: Cách này không an toàn cho production, chỉ dùng cho project sinh viên.
                    firestore.collection("users")
                        .document(document.id)
                        .update("password", _uiState.value.newPassword)
                        .await()

                    _uiState.update { it.copy(isLoading = false, resetPasswordSuccess = true) }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Email không tồn tại trong hệ thống") }
                }

            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Lỗi: ${e.message}") }
            }
        }
    }

    private fun validatePasswordInputs(): Boolean {
        val state = _uiState.value
        var isValid = true

        if (!state.hasLowercase || !state.hasUppercase || !state.hasNumber || !state.hasMinimum8Chars) {
            _uiState.update { it.copy(newPasswordError = "Mật khẩu không đủ mạnh") }
            isValid = false
        }
        if (state.newPassword != state.confirmPassword) {
            _uiState.update { it.copy(confirmPasswordError = "Mật khẩu không khớp") }
            isValid = false
        }
        return isValid
    }
}