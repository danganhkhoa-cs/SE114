package com.example.se114.ui.presentation.login

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

data class LoginUiState(
    val email: String = "",
    val emailError: String? = null,
    val password: String = "",
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val loginSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = null, errorMessage = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null, errorMessage = null) }
    }

    fun signIn() {
        if (!validateInputs()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                signInWithFirebaseAuth()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Connection error: ${e.message}") }
            }
        }
    }

    private suspend fun signInWithFirebaseAuth() {
        try {
            val authResult = auth.signInWithEmailAndPassword(_uiState.value.email, _uiState.value.password).await()
            val user = authResult.user

            if (user != null) {
                // KIỂM TRA EMAIL ĐÃ KÍCH HOẠT CHƯA
                if (user.isEmailVerified) {
                    // Đã kích hoạt -> Cho phép đăng nhập
                    saveUserDataAndProceed(user.uid)
                } else {
                    // Chưa kích hoạt -> Đăng xuất ngay và báo lỗi
                    auth.signOut()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Please activate your email to login!" // Thông báo lỗi cụ thể
                        )
                    }
                }
                // --------------------------------------------------------
            } else {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        emailError = null,
                        passwordError = "Incorrect email or password",
                        errorMessage = "Incorrect email or password"
                    )
                }
            }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    emailError = null,
                    passwordError = "Incorrect email or password",
                    errorMessage = "Incorrect email or password"
                )
            }
        }
    }

    private suspend fun saveUserDataAndProceed(uid: String) {
        try {
            val documentSnapshot = firestore.collection("users").document(uid).get().await()

            if (documentSnapshot.exists()) {
                val name = documentSnapshot.getString("name") ?: "User"
                val phone = documentSnapshot.getString("phone") ?: ""
                val bio = documentSnapshot.getString("bio") ?: ""
                val address = documentSnapshot.getString("address") ?: ""
                val job = documentSnapshot.getString("job") ?: ""

                val genderRaw = documentSnapshot.get("gender")
                val gender = when (genderRaw) {
                    is String -> genderRaw
                    is Boolean -> if (genderRaw) "Male" else "Female"
                    else -> "Male"
                }

                preferencesManager.userId = uid
                preferencesManager.userName = name
                preferencesManager.userEmail = _uiState.value.email
                preferencesManager.userPhone = phone
                preferencesManager.userBio = bio
                preferencesManager.userAddress = address
                preferencesManager.userJob = job
                preferencesManager.userGender = gender

                _uiState.update { it.copy(isLoading = false, loginSuccess = true) }
            } else {
                _uiState.update { it.copy(isLoading = false, errorMessage = "User data not found.") }
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to load data: ${e.message}") }
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
            _uiState.update { it.copy(passwordError = "Password must be at least 6 characters") }
            isValid = false
        }

        return isValid
    }
}