package com.example.se114.ui.presentation.login

import android.text.Layout
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.se114.ui.presentation.components.EmailTextField
import com.example.se114.ui.presentation.components.PasswordTextField
import com.example.se114.ui.presentation.components.PhoneTextField
import com.example.se114.ui.theme.AppFormBackground
import com.example.se114.ui.theme.AppGray
import com.example.se114.ui.theme.AppTealBlob
import com.example.se114.ui.theme.AppTealDark
import com.example.se114.ui.theme.AppTealLight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(

    viewModel: LoginViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current

    LaunchedEffect(key1 = uiState.loginSuccess) {
        if (uiState.loginSuccess) {
            Toast.makeText(context, "Login Successful!", Toast.LENGTH_SHORT).show()
            onLoginSuccess()
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

            Column (
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.Center).offset(x=0.dp, y=20.dp)
            ){
                Text(
                    "Hello!",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 70.sp
                )

                Text(
                    "Welcome to LocaSOS",
                    color = Color.White,
                    fontWeight = FontWeight.Normal,
                    fontSize = 20.sp
                )
            }
        }

        LoginForm(
            uiState = uiState,
            viewModel = viewModel,
            onNavigateToRegister = onNavigateToRegister,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 300.dp)
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
fun LoginForm(

    uiState: LoginUiState,
    viewModel: LoginViewModel,
    onNavigateToRegister: () -> Unit,
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

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "Login",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.fillMaxWidth(), // 👈 Sẽ tự động căn lề trái
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

                modifier = Modifier.fillMaxWidth().padding(bottom = 0.dp),
                label = "Password",
                value = uiState.password,
                onValueChange = viewModel::onPasswordChange,
                isError = uiState.passwordError != null,
                errorMessage = uiState.passwordError
            )

            TextButton(
                onClick = onNavigateToRegister,
                contentPadding = PaddingValues(horizontal = 8.dp),
                modifier = Modifier.align(Alignment.End).height(20.dp).offset(y=10.dp)
            ) {
                Text(
                    "Forgot password",
                    color = AppTealDark,
                    style = MaterialTheme.typography.labelLarge
                )
            }


            Spacer(modifier = Modifier.height(48.dp))

            Button(

                onClick = viewModel::signIn,
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = AppTealDark)
            ) {
                Text(
                    text = "Login",
                    modifier = Modifier.padding(vertical = 8.dp),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().offset(y=10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Don't have account? ",
                    color = AppGray,
                    style = MaterialTheme.typography.labelLarge
                )
                TextButton(
                    onClick = onNavigateToRegister,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(20.dp)
                ) {
                    Text(
                        "Sign Up",
                        color = AppTealDark,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
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