package com.example.se114.ui.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
                }
            )
        }

        // Màn hình Register
        composable(route = Screen.Register.route) {
            RegisterScreen(
                onRegisterSuccess = {
                    // Đăng ký thành công: Quay lại Login (đúng theo yêu cầu của bạn)
                    // Xóa màn Register khỏi stack
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onBackToLogin = {
                    // Nhấn nút back trên màn Register
                    navController.popBackStack()
                }
            )
        }

        // Màn hình Home (sau khi đăng nhập)
        composable(route = Screen.Home.route) {
            HomeScreen()
        }
    }
}