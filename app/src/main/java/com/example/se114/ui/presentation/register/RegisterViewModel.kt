package com.example.se114.ui.presentation.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.se114.local.PreferencesManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

data class RegisterUiState(
    val email: String = "",
    val emailError: String? = null,
    val password: String = "",
    val passwordError: String? = null,
    val confirmPassword: String = "",
    val confirmPasswordError: String? = null,
    val phone: String = "",
    val phoneError: String? = null,
    val isLoading: Boolean = false,
    val registerSuccess: Boolean = false,
    val errorMessage: String? = null,

    // --- THÊM CÁC TRƯỜNG KIỂM TRA MẬT KHẨU ---
    val hasLowercase: Boolean = false,
    val hasUppercase: Boolean = false,
    val hasNumber: Boolean = false,
    val hasMinimum8Chars: Boolean = false
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState = _uiState.asStateFlow()

    private val phoneRegex = "^(0|\\+84)[35789]\\d{8}$".toRegex()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = null, errorMessage = null) }
    }

    // --- CẬP NHẬT LOGIC CHECK PASSWORD KHI NHẬP ---
    fun onPasswordChange(password: String) {
        _uiState.update {
            it.copy(
                password = password,
                passwordError = null,
                errorMessage = null,
                // Tự động kiểm tra các điều kiện
                hasLowercase = password.any { char -> char.isLowerCase() },
                hasUppercase = password.any { char -> char.isUpperCase() },
                hasNumber = password.any { char -> char.isDigit() },
                hasMinimum8Chars = password.length >= 8
            )
        }
    }

    fun onConfirmPasswordChange(confirmPass: String) {
        _uiState.update { it.copy(confirmPassword = confirmPass, confirmPasswordError = null, errorMessage = null) }
    }

    fun onPhoneChange(phone: String) {
        _uiState.update { it.copy(phone = phone, phoneError = null, errorMessage = null) }
    }

    fun signUp() {
        if (!validateInputs()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                // 1. Tạo tài khoản trên Firebase Auth
                val authResult = auth.createUserWithEmailAndPassword(_uiState.value.email, _uiState.value.password).await()
                val user = authResult.user

                if (user != null) {
                    // Gửi email xác thực
                    user.sendEmailVerification().await()

                    // 2. Chuẩn bị dữ liệu User
                    val defaultName = _uiState.value.email.substringBefore("@")
                    val userMap = hashMapOf(
                        "firebase_uid" to user.uid,
                        "email" to _uiState.value.email,
                        "phone" to _uiState.value.phone,
                        "name" to defaultName,
                        "address" to "",
                        "gender" to "not_update",
                        "job" to "",
                        "bio" to "New member of LocaSOS",
                        "avatar_url" to "",
                        "average_rating" to 0.0,
                        "review_count" to 0,
                        "is_deleted" to false,
                        "created_at" to Date(),
                        "last_active_at" to Date(),
                        "fcm_token" to ""
                    )

                    // 3. Lưu vào Firestore (Không lưu password)
                    firestore.collection("users")
                        .document(user.uid)
                        .set(userMap)
                        .await()

                    auth.signOut() // Đăng xuất để yêu cầu đăng nhập lại
                    _uiState.update { it.copy(isLoading = false, registerSuccess = true) }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Authentication failed.") }
                }

            } catch (e: Exception) {
                val errorMsg = when (e) {
                    is FirebaseAuthUserCollisionException -> preferencesManager.getString("email_exists_error")
                    else -> e.message ?: preferencesManager.getString("unknown_error")
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        emailError = if (e is FirebaseAuthUserCollisionException) errorMsg else null,
                        errorMessage = if (e !is FirebaseAuthUserCollisionException) errorMsg else null
                    )
                }
            }
        }
    }

    private fun validateInputs(): Boolean {
        val state = _uiState.value
        var isValid = true

        if (state.email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            _uiState.update { it.copy(emailError = "Invalid email") }
            isValid = false
        }

        // --- CẬP NHẬT VALIDATE PASSWORD MẠNH ---
        if (!state.hasLowercase || !state.hasUppercase || !state.hasNumber || !state.hasMinimum8Chars) {
            _uiState.update { it.copy(passwordError = "Password is not strong enough") }
            isValid = false
        }

        if (state.password != state.confirmPassword) {
            _uiState.update { it.copy(confirmPasswordError = "Password do not match") }
            isValid = false
        }

        if (state.phone.isBlank() || !phoneRegex.matches(state.phone)) {
            _uiState.update { it.copy(phoneError = "Invalid Vietnamese phone number") }
            isValid = false
        }

        return isValid
    }
}