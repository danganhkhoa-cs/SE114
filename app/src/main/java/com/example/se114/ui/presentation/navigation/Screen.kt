package com.example.se114.ui.presentation.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login_screen")
    object Register : Screen("register_screen")
    object ForgotPassword : Screen("forgot_password_screen")
    object OTPVerification : Screen("otp_verification_screen")
    object ResetPassword : Screen("reset_password_screen")
    object Home : Screen("home_screen")
}