package com.example.se114.ui.presentation.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
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
    val errorMessage: String? = null // Thêm để hiển thị lỗi từ Firebase
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState = _uiState.asStateFlow()

    private val phoneRegex = "^(0|\\+84)[35789]\\d{8}\$".toRegex()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = null, errorMessage = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null, errorMessage = null) }
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

                    // 2. Chuẩn bị dữ liệu User theo Schema
                    // Tên mặc định lấy từ email (phần trước @)
                    val defaultName = _uiState.value.email.substringBefore("@")

                    val userMap = hashMapOf(
                        "firebase_uid" to user.uid,
                        "email" to _uiState.value.email,
                        "phone" to _uiState.value.phone,
                        "name" to defaultName,
                        "address" to "",
                        "gender" to true, // Default: Male/True (có thể sửa logic sau)
                        "job" to "",
                        "bio" to "New member of LocaSOS",
                        "avatar_url" to "",
                        "average_rating" to 0.0,
                        "review_count" to 0,
                        "is_deleted" to false,
                        "created_at" to Date(),
                        "last_active_at" to Date()
                    )

                    // 3. Lưu vào Firestore (Collection 'users', Document ID = UID)
                    firestore.collection("users")
                        .document(user.uid)
                        .set(userMap)
                        .await()

                    auth.signOut()

                    _uiState.update { it.copy(isLoading = false, registerSuccess = true) }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Authentication failed.") }
                }

            } catch (e: Exception) {
                // Xử lý lỗi (ví dụ: email đã tồn tại)
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message ?: "Registration failed") }
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

        if (state.password.length < 6) {
            _uiState.update { it.copy(passwordError = "Password must contain at least 6 characters") }
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