package com.example.se114.ui.presentation.emergency

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.se114.data.dummy.DummyChatData
import com.example.se114.data.dummy.DummyPostData
import com.example.se114.local.PreferencesManager
import com.example.se114.ui.theme.AppTealDark
import com.example.se114.ui.theme.AppTealLight

@Composable
fun EmergencyScreen() {
    val context = LocalContext.current
    // 1. Khởi tạo PreferencesManager để lấy cài đặt
    val preferencesManager = remember { PreferencesManager(context) }
    val isDarkMode = preferencesManager.isDarkMode

    val scrollState = rememberScrollState()

    // State input
    var postContent by remember { mutableStateOf("") }
    var sosReason by remember { mutableStateOf("") }
    var sosLocation by remember { mutableStateOf("") }

    // --- CẤU HÌNH MÀU SẮC ĐỘNG (THEME) ---
    val backgroundColor = if (isDarkMode) MaterialTheme.colorScheme.background else AppTealLight.copy(alpha = 0.3f)
    val textColor = if (isDarkMode) Color.White else AppTealDark
    val cardColor = if (isDarkMode) MaterialTheme.colorScheme.surface else Color.White
    val subTextColor = if (isDarkMode) Color.LightGray else Color.Gray
    val inputBorderColor = if (isDarkMode) Color.Gray else AppTealDark

    // Màu gradient cho thẻ SOS
    val sosGradientColors = if (isDarkMode) {
        listOf(Color(0xFF5D1010), MaterialTheme.colorScheme.surface) // Đỏ thẫm -> Đen
    } else {
        listOf(Color(0xFFFFEBEE), Color.White) // Hồng nhạt -> Trắng
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor) // Màu nền thay đổi
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // HEADER
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(vertical = 24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFD32F2F),
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = preferencesManager.getString("emergency_title"), // Đa ngôn ngữ
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor // Màu chữ thay đổi
                )
            }

            // --- PHẦN 1: ĐĂNG BÀI LÊN HOME ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor), // Màu thẻ thay đổi
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = preferencesManager.getString("community_share"),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = postContent,
                        onValueChange = { postContent = it },
                        label = { Text(preferencesManager.getString("what_on_your_mind")) },
                        placeholder = { Text(preferencesManager.getString("post_placeholder")) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 3,
                        maxLines = 5,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = inputBorderColor,
                            focusedLabelColor = textColor,
                            unfocusedLabelColor = subTextColor
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (postContent.isNotBlank()) {
                                DummyPostData.addPost(postContent)
                                postContent = ""
                                Toast.makeText(context, preferencesManager.getString("post_success"), Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.align(Alignment.End),
                        colors = ButtonDefaults.buttonColors(containerColor = AppTealDark)
                    ) {
                        Text(preferencesManager.getString("post_button"))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- PHẦN 2: SOS KHẨN CẤP ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Box(
                    modifier = Modifier.background(
                        Brush.verticalGradient(colors = sosGradientColors) // Gradient thay đổi
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = preferencesManager.getString("sos_title"),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFD32F2F) // Luôn giữ màu đỏ cảnh báo
                        )
                        Text(
                            text = preferencesManager.getString("sos_subtitle"),
                            fontSize = 14.sp,
                            color = subTextColor
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Input 1
                        OutlinedTextField(
                            value = sosReason,
                            onValueChange = { sosReason = it },
                            label = { Text(preferencesManager.getString("sos_reason_label")) },
                            placeholder = { Text(preferencesManager.getString("sos_reason_placeholder")) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFD32F2F),
                                focusedLabelColor = Color(0xFFD32F2F),
                                unfocusedBorderColor = if(isDarkMode) Color.Gray else Color.LightGray,
                                cursorColor = Color(0xFFD32F2F)
                            )
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Input 2
                        OutlinedTextField(
                            value = sosLocation,
                            onValueChange = { sosLocation = it },
                            label = { Text(preferencesManager.getString("sos_location_label")) },
                            placeholder = { Text(preferencesManager.getString("sos_location_placeholder")) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            trailingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = Color(0xFFD32F2F),
                                focusedLabelColor = Color(0xFFD32F2F),
                                unfocusedBorderColor = if(isDarkMode) Color.Gray else Color.LightGray,
                                cursorColor = Color(0xFFD32F2F)
                            )
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Nút SOS
                        Button(
                            onClick = {
                                if (sosReason.isNotBlank() && sosLocation.isNotBlank()) {
                                    DummyChatData.sendSOS(sosReason, sosLocation)
                                    sosReason = ""
                                    sosLocation = ""
                                    Toast.makeText(context, preferencesManager.getString("sos_success"), Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, preferencesManager.getString("sos_error"), Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F)),
                            shape = RoundedCornerShape(28.dp),
                            elevation = ButtonDefaults.buttonElevation(8.dp)
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                preferencesManager.getString("sos_button"),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Padding bottom
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}