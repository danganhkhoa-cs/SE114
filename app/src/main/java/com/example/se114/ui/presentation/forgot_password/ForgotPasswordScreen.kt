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
import com.example.se114.ui.theme.AppFormBackground
import com.example.se114.ui.theme.AppTealBlob
import com.example.se114.ui.theme.AppTealDark
import com.example.se114.ui.theme.AppTealLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(

    viewModel: ForgotPasswordViewModel = hiltViewModel(),
    onSendMailSuccess: () -> Unit,
    onBackToLogin: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current

    LaunchedEffect(key1 = uiState.sendMailSuccess) {
        if (uiState.sendMailSuccess) {
            Toast.makeText(context, "Send Mail Successful!", Toast.LENGTH_SHORT).show()
            onSendMailSuccess()
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

        ForgotPasswordForm(
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
fun ForgotPasswordForm(

    uiState: ForgotPasswordUiState,
    viewModel: ForgotPasswordViewModel,
    onBackToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxSize(),
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
                text = "Forgot password",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.fillMaxWidth(),
                color = AppTealDark
            )
            Text(
                text = "Please enter your email to reset the password",
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth(),
                color = AppTealDark.copy(alpha = .6F)
            )

            Spacer(modifier = Modifier.height(16.dp))

            EmailTextField(

                modifier = Modifier.fillMaxWidth(),
                value = uiState.email,
                onValueChange = viewModel::onEmailChange,
                isError = uiState.emailError != null,
                errorMessage = uiState.emailError
            )

            Spacer(modifier = Modifier.height(36.dp))

            Button(

                onClick = viewModel::sendMail,
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = AppTealDark)
            ) {
                Text(
                    text = "Send OTP",
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