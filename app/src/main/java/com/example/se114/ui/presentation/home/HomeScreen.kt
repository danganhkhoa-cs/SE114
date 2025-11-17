package com.example.se114.ui.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.se114.ui.theme.AppTealDark

@Composable
fun HomeScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Home Screen",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = AppTealDark
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Welcome to LocaSOS!",
                fontSize = 18.sp,
                color = AppTealDark.copy(alpha = 0.7f)
            )
        }
    }
}