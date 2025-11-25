package com.example.se114.ui.presentation.emergency

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
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
fun EmergencyScreen() {
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
                imageVector = Icons.Default.Warning,
                contentDescription = "Emergency",
                tint = Color.Red,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Emergency",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = AppTealDark
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "SOS Feature Coming Soon",
                fontSize = 16.sp,
                color = Color.Gray
            )
        }
    }
}