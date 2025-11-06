package com.example.se114.ui.presentation.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    val registerSuccess: Boolean = false
)

@HiltViewModel
class RegisterViewModel @Inject constructor(

) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState = _uiState.asStateFlow()


    private val phoneRegex = "^(0|\\+84)[35789]\\d{8}\$".toRegex()



    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null) }
    }

    fun onConfirmPasswordChange(confirmPass: String) {
        _uiState.update { it.copy(confirmPassword = confirmPass, confirmPasswordError = null) }
    }

    fun onPhoneChange(phone: String) {
        _uiState.update { it.copy(phone = phone, phoneError = null) }
    }



    fun signUp() {
        if (!validateInputs()) return // Nếu validation thất bại, dừng lại

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            kotlinx.coroutines.delay(2000)



            _uiState.update { it.copy(isLoading = false, registerSuccess = true) }
        }
    }



    private fun validateInputs(): Boolean {
        val state = _uiState.value
        var isValid = true


        if (state.email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            _uiState.update { it.copy(emailError = "Email không hợp lệ") }
            isValid = false
        }


        if (state.password.length < 6) {
            _uiState.update { it.copy(passwordError = "Mật khẩu phải có ít nhất 6 ký tự") }
            isValid = false
        }


        if (state.password != state.confirmPassword) {
            _uiState.update { it.copy(confirmPasswordError = "Mật khẩu không khớp") }
            isValid = false
        }


        if (state.phone.isBlank() || !phoneRegex.matches(state.phone)) {
            _uiState.update { it.copy(phoneError = "Số điện thoại Việt Nam không hợp lệ") }
            isValid = false
        }

        return isValid
    }
}