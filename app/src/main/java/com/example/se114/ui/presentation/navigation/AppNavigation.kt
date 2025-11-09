package com.example.se114.ui.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.se114.ui.presentation.forgot_password.ForgotPasswordScreen
import com.example.se114.ui.presentation.forgot_password.OTPVerificationScreen
import com.example.se114.ui.presentation.home.HomeScreen
import com.example.se114.ui.presentation.login.LoginScreen
import com.example.se114.ui.presentation.register.RegisterScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Login.route // Màn hình bắt đầu là Login
    ) {

        // Màn hình Login
        composable(route = Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    // Đăng nhập thành công: Tới Home và xóa hết stack cũ
                    navController.navigate(Screen.Home.route) {
                        popUpTo(navController.graph.id) { inclusive = true }
                    }
                },
                onNavigateToRegister = {
                    // Tới màn hình Đăng ký
                    navController.navigate(Screen.Register.route)
                },
                onNavigateToForgotPassword = {
                    navController.navigate(Screen.ForgotPassword.route)
                }
            )
        }

        // Màn hình Register
        composable(route = Screen.Register.route) {
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



        // Màn hình Forgot password
        composable(route = Screen.ForgotPassword.route) {
            ForgotPasswordScreen(
                onSendMailSuccess = {
                    navController.navigate(Screen.OTPVerification.route) {
                        popUpTo(Screen.ForgotPassword.route) {inclusive = true}
                    }
                },
                onBackToLogin = {
                    navController.popBackStack()
                }
            )
        }

        // Màn hình xác thực OTP
        composable(route = Screen.OTPVerification.route) {
            OTPVerificationScreen()
        }

        // Màn hình Home (sau khi đăng nhập)
        composable(route = Screen.Home.route) {
            HomeScreen()
        }
    }
}