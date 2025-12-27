package com.example.se114.ui.presentation.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.se114.local.PreferencesManager
import com.example.se114.ui.presentation.forgot_password.ForgotPasswordScreen
import com.example.se114.ui.presentation.forgot_password.OTPVerificationScreen
import com.example.se114.ui.presentation.login.LoginScreen
import com.example.se114.ui.presentation.main.MainScreen
import com.example.se114.ui.presentation.register.RegisterScreen
import com.example.se114.ui.theme.SE114Theme
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AppNavigation(
    preferencesManager: PreferencesManager,
) {
    val navController = rememberNavController()
    var isDarkTheme by remember { mutableStateOf(preferencesManager.isDarkMode) }
    val appViewModel: AppViewModel = hiltViewModel()

    // --- LOGIC MỚI: KIỂM TRA ĐĂNG NHẬP ---
    // Kiểm tra xem đã có User ID lưu trong máy chưa VÀ Firebase còn phiên đăng nhập không
    val isLoggedIn = remember {
        preferencesManager.userId.isNotEmpty() && FirebaseAuth.getInstance().currentUser != null
    }

    // Xác định màn hình bắt đầu dựa trên trạng thái đăng nhập
    val startDest = if (isLoggedIn) Screen.Main.route else Screen.Login.route
    // --------------------------------------

    NavHost(
        navController = navController,
        startDestination = startDest, // <--- Thay đổi ở đây
    ) {

        // Login Screen
        composable(route = Screen.Login.route) {
            SE114Theme(darkTheme = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    LoginScreen(
                        onLoginSuccess = {
                            // Cập nhật lại theme theo user setting khi vừa login xong
                            isDarkTheme = preferencesManager.isDarkMode
                            navController.navigate(Screen.Main.route) {
                                // Xóa toàn bộ stack Login để không back lại được
                                popUpTo(navController.graph.id) { inclusive = true }
                            }
                        },
                        onNavigateToRegister = {
                            navController.navigate(Screen.Register.route)
                        },
                        onNavigateToForgotPassword = {
                            navController.navigate(Screen.ForgotPassword.route)
                        }
                    )
                }
            }
        }

        // Register Screen
        composable(route = Screen.Register.route) {
            SE114Theme(darkTheme = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RegisterScreen(
                        onRegisterSuccess = {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        },
                        onBackToLogin = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }

        // Forgot Password Screen
        composable(route = Screen.ForgotPassword.route) {
            SE114Theme(darkTheme = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ForgotPasswordScreen(
                        onSendMailSuccess = {
                            navController.navigate(Screen.OTPVerification.route) {
                                popUpTo(Screen.ForgotPassword.route) { inclusive = true }
                            }
                        },
                        onBackToLogin = {
                            navController.popBackStack()
                        }
                    )
                }
            }
        }

        // OTP Verification Screen
        composable(route = Screen.OTPVerification.route) {
            SE114Theme(darkTheme = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    OTPVerificationScreen(
                        onResetPasswordSuccess = {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(navController.graph.id) { inclusive = true }
                            }
                        },
                        onBackToLogin = {
                            navController.navigate(Screen.Login.route) {
                                popUpTo(navController.graph.id) { inclusive = true }
                            }
                        }
                    )
                }
            }
        }

        // Main Screen
        composable(route = Screen.Main.route) {
            SE114Theme(darkTheme = isDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(
                        preferencesManager = preferencesManager,
                        isDarkTheme = isDarkTheme,
                        onThemeChange = { newTheme ->
                            isDarkTheme = newTheme
                            preferencesManager.isDarkMode = newTheme
                        },
                        onLogout = {
                            // 1. Đăng xuất Firebase
                            FirebaseAuth.getInstance().signOut()

                            // Hủy listener thông báo
                            appViewModel.logout()

                            // 2. Xóa dữ liệu local
                            preferencesManager.clearUserData()

                            // 3. Chuyển về màn hình Login và xóa backstack
                            navController.navigate(Screen.Login.route) {
                                popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
        }
    }
}