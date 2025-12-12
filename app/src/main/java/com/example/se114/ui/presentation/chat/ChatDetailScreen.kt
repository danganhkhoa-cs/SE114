package com.example.se114.ui.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.se114.data.dummy.DummyChatData
import com.example.se114.data.model.ChatMessage
import com.example.se114.local.PreferencesManager
import com.example.se114.ui.theme.AppTealDark
import com.example.se114.ui.theme.AppTealLight
import com.example.se114.ui.theme.DarkSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    conversationId: String,
    onBackClick: () -> Unit,
    preferencesManager: PreferencesManager,
    viewModel: ChatDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDarkMode = preferencesManager.isDarkMode

    // Auto Scroll to bottom when new message arrives
    val listState = rememberLazyListState()
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    // Load conversation data when screen opens
    LaunchedEffect(conversationId) {
        viewModel.loadConversation(conversationId)
    }

    // Colors
    val headerColor = if (isDarkMode) Color.Black else AppTealDark
    val backgroundColor = if (isDarkMode) DarkSurface else Color(0xFFF5F7F8)
    val inputAreaColor = if (isDarkMode) Color.Black else Color.White
    val inputFieldColor = if (isDarkMode) Color(0xFF333333) else Color(0xFFF0F2F5)
    val inputTextColor = if (isDarkMode) Color.White else Color.Black

    Scaffold(
        topBar = {
            Surface(color = headerColor, shadowElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(top = 2.dp, bottom = 8.dp, start = 4.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }

                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = uiState.conversation?.avatar ?: "", fontSize = 20.sp)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = uiState.conversation?.name ?: "Chat",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                maxLines = 1
                            )
                            if (uiState.conversation?.isOnline == true) {
                                Text(
                                    preferencesManager.getString("chat_active_now"),
                                    color = AppTealLight,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        containerColor = backgroundColor,
        bottomBar = {
            Surface(color = inputAreaColor, tonalElevation = 8.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = uiState.messageInput,
                        onValueChange = viewModel::onMessageInputChange,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp, max = 100.dp),
                        placeholder = {
                            Text(
                                preferencesManager.getString("chat_type_message"),
                                fontSize = 15.sp,
                                color = Color.Gray
                            )
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = inputFieldColor,
                            unfocusedContainerColor = inputFieldColor,
                            focusedTextColor = inputTextColor,
                            unfocusedTextColor = inputTextColor,
                            cursorColor = AppTealDark
                        ),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            // Truyền text trả lời tự động để ViewModel xử lý
                            viewModel.sendMessage(
                                autoReplyText = preferencesManager.getString("chat_auto_reply")
                            )
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            "Send",
                            tint = if (uiState.messageInput.isNotBlank()) AppTealDark else Color.Gray,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(uiState.messages) { msg ->
                    MessageBubble(
                        message = msg,
                        isMe = msg.senderId == DummyChatData.CURRENT_USER_ID,
                        isDarkMode = isDarkMode
                    )
                }
            }
        }
    }
}

@Composable
fun MessageBubble(message: ChatMessage, isMe: Boolean, isDarkMode: Boolean) {
    val bubbleColor = if (isMe) AppTealDark else (if(isDarkMode) Color(0xFF333333) else Color(0xFFE4E6EB))
    val textColor = if (isMe) Color.White else (if(isDarkMode) Color.White else Color.Black)

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            color = bubbleColor,
            shape = RoundedCornerShape(
                topStart = 18.dp,
                topEnd = 18.dp,
                bottomStart = if(isMe) 18.dp else 4.dp,
                bottomEnd = if(isMe) 4.dp else 18.dp
            ),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.content,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                color = textColor,
                fontSize = 15.sp,
                lineHeight = 20.sp
            )
        }
    }
}