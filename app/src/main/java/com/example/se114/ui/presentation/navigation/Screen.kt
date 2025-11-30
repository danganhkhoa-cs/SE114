package com.example.se114.ui.presentation.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login_screen")
    object Register : Screen("register_screen")
    object ForgotPassword : Screen("forgot_password_screen")
    object OTPVerification : Screen("otp_verification_screen")
    object Main : Screen("main_screen")

//     Profile related screens
    object ProfileHome : Screen("profile_home")
    object AccountSettings : Screen("account_settings")
    object AccountData : Screen("account_data")
    object HelpSupport : Screen("help_support")
    object Settings : Screen("settings")
}