package com.example.se114.ui.presentation.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login_screen")
    object Register : Screen("register_screen")
    object ForgotPassword : Screen("forgot_password_screen")
    object OTPVerification : Screen("otp_verification_screen")
    object ResetPassword : Screen("reset_password_screen")
    object Main : Screen("main_screen")
    object Home : Screen("home_screen")

    // Profile related screens
    object AccountSettings : Screen("account_settings_screen")
    object AccountData : Screen("account_data_screen")
    object HelpSupport : Screen("help_support_screen")
    object Settings : Screen("settings_screen")
}