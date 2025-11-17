package com.example.se114.ui.presentation.forgot_password

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.se114.ui.presentation.components.OTPTextField
import com.example.se114.ui.presentation.components.PasswordTextField
import com.example.se114.ui.theme.AppFormBackground
import com.example.se114.ui.theme.AppTealBlob
import com.example.se114.ui.theme.AppTealDark
import com.example.se114.ui.theme.AppTealLight

@Composable
fun OTPVerificationScreen(
    viewModel: OTPVerificationViewModel = hiltViewModel(),
    onResetPasswordSuccess: () -> Unit,
    onBackToLogin: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(key1 = uiState.resetPasswordSuccess) {
        if (uiState.resetPasswordSuccess) {
            Toast.makeText(context, "Password reset successful!", Toast.LENGTH_SHORT).show()
            onResetPasswordSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTealLight)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.35f)
                .background(AppTealDark)
                .clipToBounds()
        ) {
            HeaderBlob(
                modifier = Modifier
                    .size(width = 250.dp, height = 220.dp)
                    .align(Alignment.TopStart)
                    .offset(x = (-80).dp, y = (-50).dp)
            )

            HeaderBlob(
                modifier = Modifier
                    .size(width = 150.dp, height = 130.dp)
                    .align(Alignment.CenterEnd)
                    .offset(x = 60.dp, y = (-20).dp)
            )
            HeaderBlob(
                modifier = Modifier
                    .size(width = 100.dp, height = 90.dp)
                    .align(Alignment.BottomStart)
                    .offset(x = (-30).dp, y = 30.dp)
            )
        }

        OTPVerificationForm(
            uiState = uiState,
            viewModel = viewModel,
            onBackToLogin = onBackToLogin,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 140.dp)
        )

        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
        }
    }
}

@Composable
fun OTPVerificationForm(
    uiState: OTPVerificationUiState,
    viewModel: OTPVerificationViewModel,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxSize(),
        color = AppFormBackground,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onBackToLogin,
                    contentPadding = PaddingValues(end = 4.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = AppTealDark
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Back to login",
                        color = AppTealDark,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Reset Password",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.fillMaxWidth(),
                color = AppTealDark
            )
            Text(
                text = "Enter the code sent to your email to reset your password",
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth(),
                color = AppTealDark.copy(alpha = .6F)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OTPTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.otp,
                onValueChange = viewModel::onOTPChange,
                isError = uiState.otpError != null,
                errorMessage = uiState.otpError
            )

            if (uiState.isOTPVerified) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Verified",
                        tint = AppTealDark,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Code verified",
                        color = AppTealDark,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                }
            }

            if (uiState.isOTPVerified) {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "New password",
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleLarge,
                    color = AppTealDark
                )

                Spacer(modifier = Modifier.height(16.dp))

                PasswordTextField(
                    modifier = Modifier.fillMaxWidth(),
                    label = "New Password",
                    value = uiState.newPassword,
                    onValueChange = viewModel::onNewPasswordChange,
                    isError = uiState.newPasswordError != null,
                    errorMessage = uiState.newPasswordError
                )

                Spacer(modifier = Modifier.height(10.dp))

                PasswordTextField(
                    modifier = Modifier.fillMaxWidth(),
                    label = "Confirm Password",
                    value = uiState.confirmPassword,
                    onValueChange = viewModel::onConfirmPasswordChange,
                    isError = uiState.confirmPasswordError != null,
                    errorMessage = uiState.confirmPasswordError
                )

                Spacer(modifier = Modifier.height(8.dp))

                PasswordRequirement(
                    text = "At least one lowercase letter",
                    isMet = uiState.hasLowercase
                )
                PasswordRequirement(
                    text = "Minimum 8 characters",
                    isMet = uiState.hasMinimum8Chars
                )
                PasswordRequirement(
                    text = "At least one uppercase letter",
                    isMet = uiState.hasUppercase
                )
                PasswordRequirement(
                    text = "At least one number",
                    isMet = uiState.hasNumber
                )
            }

            Spacer(modifier = Modifier.height(36.dp))

            Button(
                onClick = {
                    if (uiState.isOTPVerified) {
                        viewModel.resetPassword()
                    } else {
                        viewModel.verifyOTP()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = AppTealDark),
                enabled = if (uiState.isOTPVerified) {
                    uiState.hasLowercase && uiState.hasMinimum8Chars &&
                            uiState.hasUppercase && uiState.hasNumber &&
                            uiState.newPassword == uiState.confirmPassword &&
                            uiState.newPassword.isNotEmpty()
                } else {
                    uiState.otp.length == 6
                }
            ) {
                Text(
                    text = if (uiState.isOTPVerified) "Reset password" else "Verify Code",
                    modifier = Modifier.padding(vertical = 8.dp),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PasswordRequirement(
    text: String,
    isMet: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = if (isMet) {
                Icons.Default.CheckCircle
            } else {
                Icons.Default.Cancel
            },
            contentDescription = null,
            tint = if (isMet) AppTealDark else Color.Gray,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            fontSize = 12.sp,
            color = if (isMet) AppTealDark else Color.Gray
        )
    }
}

@Composable
private fun HeaderBlob(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val path = Path().apply {
            moveTo(width * 0.2f, height * 0.3f)
            quadraticTo(width * 0.5f, -height * 0.1f, width * 0.8f, height * 0.3f)
            quadraticTo(width * 1.1f, height * 0.7f, width * 0.7f, height * 1.0f)
            quadraticTo(width * 0.3f, height * 1.1f, 0f, height * 0.7f)
            close()
        }
        drawPath(
            path = path,
            color = AppTealBlob
        )
    }
}