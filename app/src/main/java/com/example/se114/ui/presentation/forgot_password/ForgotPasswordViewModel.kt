package com.example.se114.ui.presentation.forgot_password

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.se114.local.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Properties
import javax.inject.Inject
import javax.mail.Authenticator
import javax.mail.Message
import javax.mail.PasswordAuthentication
import javax.mail.Session
import javax.mail.Transport
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeMessage

data class ForgotPasswordUiState(
    val email: String = "",
    val emailError: String? = null,
    val isLoading: Boolean = false,
    val sendMailSuccess: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState = _uiState.asStateFlow()

    // --- CẤU HÌNH GMAIL ---
    private val SENDER_EMAIL = "minhkhoa200511@gmail.com"
    private val SENDER_PASSWORD = "qeox hsde ahdz hkzy"

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = null, errorMessage = null) }
    }

    fun sendMail() {
        if (!validateInputs()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                val otpCode = (100000..999999).random().toString()

                withContext(Dispatchers.IO) {
                    sendEmailWithGmail(
                        toEmail = _uiState.value.email,
                        otpCode = otpCode
                    )
                }

                // Lưu OTP và Email vào Preferences
                preferencesManager.saveOTPForReset(otpCode)
                preferencesManager.saveEmailForReset(_uiState.value.email) // <-- LƯU EMAIL TẠI ĐÂY

                _uiState.update { it.copy(isLoading = false, sendMailSuccess = true) }

            } catch (e: Exception) {
                e.printStackTrace()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Gửi mail lỗi: ${e.message}"
                    )
                }
            }
        }
    }

    private fun sendEmailWithGmail(toEmail: String, otpCode: String) {
        val props = Properties().apply {
            put("mail.smtp.auth", "true")
            put("mail.smtp.starttls.enable", "true")
            put("mail.smtp.host", "smtp.gmail.com")
            put("mail.smtp.port", "587")
        }

        val session = Session.getInstance(props, object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(SENDER_EMAIL, SENDER_PASSWORD)
            }
        })

        try {
            val message = MimeMessage(session).apply {
                setFrom(InternetAddress(SENDER_EMAIL, "LocaSOS Support"))
                setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail))
                subject = "LocaSOS - Mã xác thực OTP"
                setText("Mã OTP của bạn là: $otpCode")
            }
            Transport.send(message)
        } catch (e: Exception) {
            throw e
        }
    }

    private fun validateInputs(): Boolean {
        val state = _uiState.value
        if (state.email.isBlank() || !android.util.Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            _uiState.update { it.copy(emailError = "Email không hợp lệ") }
            return false
        }
        return true
    }
}