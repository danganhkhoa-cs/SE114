package com.example.se114.ui.presentation.forgot_password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.se114.local.PreferencesManager
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
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
    private val functions: FirebaseFunctions
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
        val otpInput = _uiState.value.otp

        // 1. Validate sơ bộ độ dài
        if (otpInput.length != 6) {
            _uiState.update { it.copy(otpError = "Vui lòng nhập đủ 6 số") }
            return
        }

        val email = preferencesManager.getEmailForReset()
        if (email == null) {
            _uiState.update { it.copy(errorMessage = "Phiên làm việc hết hạn") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, otpError = null) }

            // 2. Gọi Server để check OTP
            val data = hashMapOf(
                "email" to email,
                "otp" to otpInput
            )

            functions
                .getHttpsCallable("verifyOtp") // Gọi hàm mới viết
                .call(data)
                .addOnSuccessListener {
                    // 3. Nếu Server bảo OK -> Mới cho hiện khung nhập Password
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isOTPVerified = true, // Lúc này mới set true
                            otpError = null
                        )
                    }
                }
                .addOnFailureListener { e ->
                    // 4. Nếu Server bảo sai -> Báo lỗi ngay
                    val msg = if (e is FirebaseFunctionsException) {
                        when (e.code) {
                            FirebaseFunctionsException.Code.PERMISSION_DENIED -> "Mã OTP không đúng"
                            FirebaseFunctionsException.Code.DEADLINE_EXCEEDED -> "Mã OTP đã hết hạn"
                            FirebaseFunctionsException.Code.NOT_FOUND -> "Yêu cầu không tồn tại"
                            else -> e.message
                        }
                    } else {
                        e.message
                    }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isOTPVerified = false, // Vẫn giữ ở màn hình nhập OTP
                            otpError = msg ?: "Lỗi xác thực"
                        )
                    }
                }
        }
    }

    fun resetPassword() {
        if (!validatePasswordInputs()) return

        val email = preferencesManager.getEmailForReset()
        if (email == null) {
            _uiState.update { it.copy(errorMessage = "Phiên làm việc hết hạn") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // Gọi Function 'resetPassword'
            val data = hashMapOf(
                "email" to email,
                "otp" to _uiState.value.otp, // Gửi OTP lên để Server check
                "newPassword" to _uiState.value.newPassword
            )

            functions
                .getHttpsCallable("resetPassword")
                .call(data)
                .addOnSuccessListener {
                    // Thành công
                    preferencesManager.saveEmailForReset("") // Xóa cache email
                    _uiState.update { it.copy(isLoading = false, resetPasswordSuccess = true) }
                }
                .addOnFailureListener { e ->
                    // Thất bại
                    val msg = if (e is FirebaseFunctionsException) {
                        when (e.code) {
                            FirebaseFunctionsException.Code.PERMISSION_DENIED -> "Mã OTP không đúng!"
                            FirebaseFunctionsException.Code.DEADLINE_EXCEEDED -> "Mã OTP đã hết hạn!"
                            FirebaseFunctionsException.Code.NOT_FOUND -> "Yêu cầu không tồn tại!"
                            else -> e.message
                        }
                    } else {
                        e.message
                    }
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = msg ?: "Đặt lại mật khẩu thất bại")
                    }
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