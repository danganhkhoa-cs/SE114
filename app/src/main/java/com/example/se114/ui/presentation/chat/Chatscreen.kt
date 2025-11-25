package com.example.se114.ui.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.se114.ui.theme.AppTealDark
import com.example.se114.ui.theme.AppTealLight

@Composable
fun ChatScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppTealLight),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Chat,
                contentDescription = "Chat",
                tint = AppTealDark,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Chat Screen",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = AppTealDark
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Coming Soon",
                fontSize = 16.sp,
                color = Color.Gray
            )
        }
    }
}