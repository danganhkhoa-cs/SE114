package com.example.se114.ui.presentation.forgot_password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


data class ForgotPasswordUiState(
    val email: String = "",
    val emailError: String? = null,
    val isLoading: Boolean = false,
    val sendMailSuccess: Boolean = false
)

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(

) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = null) }
    }

    fun sendMail() {
        if (!validateInputs()) return // Nếu validation thất bại, dừng lại

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            kotlinx.coroutines.delay(2000)



            _uiState.update { it.copy(isLoading = false, sendMailSuccess = true) }
        }
    }



    private fun validateInputs(): Boolean {
        val state = _uiState.value
        var isValid = true


        if (state.email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            _uiState.update { it.copy(emailError = "Invalid email") }
            isValid = false
        }

        return isValid
    }
}