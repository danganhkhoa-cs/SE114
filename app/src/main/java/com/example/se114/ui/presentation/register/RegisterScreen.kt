package com.example.se114.ui.presentation.register

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
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
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
import com.example.se114.ui.presentation.components.EmailTextField
import com.example.se114.ui.presentation.components.PasswordTextField
import com.example.se114.ui.presentation.components.PhoneTextField
import com.example.se114.ui.theme.AppFormBackground
import com.example.se114.ui.theme.AppTealBlob
import com.example.se114.ui.theme.AppTealDark
import com.example.se114.ui.theme.AppTealLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: RegisterViewModel = hiltViewModel(),
    onRegisterSuccess: () -> Unit,
    onBackToLogin: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(key1 = uiState.registerSuccess) {
        if (uiState.registerSuccess) {
            Toast.makeText(
                context,
                "Register successful! Please check your email to verify.",
                Toast.LENGTH_LONG
            ).show()
            onBackToLogin()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTealLight)
    ) {
        // ... (HeaderBlob giữ nguyên) ...
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.35f)
                .background(AppTealDark)
                .clipToBounds()
        ) {
            HeaderBlob(modifier = Modifier.size(250.dp, 220.dp).align(Alignment.TopStart).offset((-80).dp, (-50).dp))
            HeaderBlob(modifier = Modifier.size(150.dp, 130.dp).align(Alignment.CenterEnd).offset(60.dp, (-20).dp))
            HeaderBlob(modifier = Modifier.size(100.dp, 90.dp).align(Alignment.BottomStart).offset((-30).dp, 30.dp))
        }

        RegisterForm(
            uiState = uiState,
            viewModel = viewModel,
            onBackToLogin = onBackToLogin,
            modifier = Modifier.fillMaxSize().padding(top = 140.dp)
        )

        if (uiState.errorMessage != null) {
            Snackbar(
                modifier = Modifier.padding(16.dp).align(Alignment.BottomCenter),
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer
            ) { Text(text = uiState.errorMessage!!) }
        }

        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(color = Color.White) }
        }
    }
}

@Composable
fun RegisterForm(
    uiState: RegisterUiState,
    viewModel: RegisterViewModel,
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
                .verticalScroll(rememberScrollState())
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBackToLogin, contentPadding = PaddingValues(end = 4.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = AppTealDark)
                    Spacer(Modifier.width(4.dp))
                    Text("Back to login", color = AppTealDark, style = MaterialTheme.typography.labelLarge)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Sign Up",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.fillMaxWidth(),
                color = AppTealDark
            )

            Spacer(modifier = Modifier.height(16.dp))

            EmailTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.email,
                onValueChange = viewModel::onEmailChange,
                isError = uiState.emailError != null,
                errorMessage = uiState.emailError
            )

            Spacer(modifier = Modifier.height(10.dp))

            PasswordTextField(
                modifier = Modifier.fillMaxWidth(),
                label = "Password",
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                isError = uiState.passwordError != null,
                errorMessage = uiState.passwordError
            )

            // --- HIỂN THỊ YÊU CẦU MẬT KHẨU ---
            Spacer(modifier = Modifier.height(8.dp))
            PasswordRequirement(text = "At least one lowercase letter", isMet = uiState.hasLowercase)
            PasswordRequirement(text = "Minimum 8 characters", isMet = uiState.hasMinimum8Chars)
            PasswordRequirement(text = "At least one uppercase letter", isMet = uiState.hasUppercase)
            PasswordRequirement(text = "At least one number", isMet = uiState.hasNumber)
            Spacer(modifier = Modifier.height(8.dp))
            // ---------------------------------

            PasswordTextField(
                modifier = Modifier.fillMaxWidth(),
                label = "Confirm Password",
                value = uiState.confirmPassword,
                onValueChange = viewModel::onConfirmPasswordChange,
                isError = uiState.confirmPasswordError != null,
                errorMessage = uiState.confirmPasswordError
            )

            Spacer(modifier = Modifier.height(10.dp))

            PhoneTextField(
                modifier = Modifier.fillMaxWidth(),
                value = uiState.phone,
                onValueChange = viewModel::onPhoneChange,
                isError = uiState.phoneError != null,
                errorMessage = uiState.phoneError
            )

            Spacer(modifier = Modifier.height(36.dp))

            Button(
                onClick = viewModel::signUp,
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = AppTealDark),
                // (Tùy chọn) Disable nút nếu chưa thỏa mãn đk
                // enabled = uiState.hasLowercase && uiState.hasUppercase && uiState.hasNumber && uiState.hasMinimum8Chars
            ) {
                Text(
                    text = "Sign Up",
                    modifier = Modifier.padding(vertical = 8.dp),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

// --- COMPONENT HIỂN THỊ TRẠNG THÁI MẬT KHẨU ---
@Composable
private fun PasswordRequirement(
    text: String,
    isMet: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 2.dp)
    ) {
        Icon(
            imageVector = if (isMet) Icons.Default.CheckCircle else Icons.Default.Cancel,
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
        drawPath(path = path, color = AppTealBlob)
    }
}