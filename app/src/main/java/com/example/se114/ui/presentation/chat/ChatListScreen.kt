package com.example.se114.ui.presentation.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.se114.data.model.Conversation
import com.example.se114.local.PreferencesManager
import com.example.se114.ui.theme.AppTealDark
import com.example.se114.ui.theme.DarkSurface

@Composable
fun ChatListScreen(
    onConversationClick: (String) -> Unit,
    preferencesManager: PreferencesManager,
    viewModel: ChatListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDarkMode = preferencesManager.isDarkMode

    // Header Colors
    val headerColor = AppTealDark
    val sheetColor = if (isDarkMode) DarkSurface else Color.White
    val deleteText = preferencesManager.getString("delete")

    Box(modifier = Modifier.fillMaxSize().background(headerColor)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // --- HEADER ---
            Column(
                modifier = Modifier.padding(top = 48.dp, bottom = 24.dp, start = 16.dp, end = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = preferencesManager.getString("chat_title"),
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = viewModel::showAddFriendDialog,
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                            .size(40.dp)
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(24.dp))
                    }
                }

                // Search Bar
                TextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    placeholder = {
                        Text(preferencesManager.getString("chat_search"), color = Color.White.copy(alpha = 0.7f))
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.2f),
                        unfocusedContainerColor = Color.White.copy(alpha = 0.2f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White
                    ),
                    singleLine = true
                )
            }

            // --- LIST CONTENT ---
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = sheetColor,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                LazyColumn(contentPadding = PaddingValues(top = 24.dp, bottom = 80.dp)) {
                    items(uiState.conversations, key = { it.id }) { conversation ->
                        ChatListItem(
                            conversation = conversation,
                            deleteText = deleteText,
                            onClick = { id ->
                                viewModel.markAsRead(id)
                                onConversationClick(id)
                            },
                            onDelete = viewModel::deleteConversation,
                            isDarkMode = isDarkMode
                        )
                    }
                }
            }
        }

        // --- DIALOG ---
        if (uiState.isShowingAddFriendDialog) {
            AddFriendDialog(
                onDismiss = viewModel::hideAddFriendDialog,
                onAdd = viewModel::addFriend,
                preferencesManager = preferencesManager
            )
        }
    }
}

@Composable
fun AddFriendDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit,
    preferencesManager: PreferencesManager
) {
    var phoneNumber by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val errorRequired = preferencesManager.getString("phone_required")

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(preferencesManager.getString("add_friend_title"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = phoneNumber,
                    onValueChange = { phoneNumber = it; errorMessage = "" },
                    label = { Text(preferencesManager.getString("phone_number")) },
                    leadingIcon = { Icon(Icons.Default.Phone, null) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF006D66), focusedLabelColor = Color(0xFF006D66))
                )

                if (errorMessage.isNotEmpty()) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF006D66))
                    ) {
                        Text(preferencesManager.getString("cancel"), color = Color(0xFF006D66))
                    }
                    Button(
                        onClick = {
                            if(phoneNumber.isBlank()) errorMessage = errorRequired
                            else onAdd(phoneNumber)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF006D66))
                    ) {
                        Text(preferencesManager.getString("add"))
                    }
                }
            }
        }
    }
}

@Composable
fun ChatListItem(
    conversation: Conversation,
    deleteText: String,
    onClick: (String) -> Unit,
    onDelete: (String) -> Unit,
    isDarkMode: Boolean
) {
    val nameColor = if (isDarkMode) Color.White else Color(0xFF006D66)
    val messageColor = if (isDarkMode) Color.LightGray else Color.Gray
    val isUnread = conversation.unreadCount > 0
    var expanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(conversation.id) }
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isUnread) {
            Box(modifier = Modifier.size(8.dp).background(Color(0xFFFF5252), CircleShape))
        } else {
            Spacer(modifier = Modifier.size(8.dp))
        }

        Spacer(modifier = Modifier.width(16.dp))

        Box {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(if(isDarkMode) Color.Gray.copy(alpha = 0.2f) else Color(0xFFE0F2F1)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = conversation.avatar,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if(isDarkMode) Color.White else Color(0xFF006D66)
                )
            }
            if (conversation.isOnline) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(14.dp)
                        .background(if(isDarkMode) DarkSurface else Color.White, CircleShape)
                        .padding(2.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF00E676), CircleShape))
                }
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = conversation.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Medium,
                    color = nameColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = conversation.lastMessageTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = messageColor
                )
            }
            Text(
                text = conversation.lastMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isUnread && !isDarkMode) Color.Black else messageColor,
                fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box {
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Default.MoreVert, null, tint = messageColor)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(if(isDarkMode) Color(0xFF333333) else Color.White)
            ) {
                DropdownMenuItem(
                    text = { Text(deleteText, color = Color.Red) },
                    onClick = { onDelete(conversation.id); expanded = false }
                )
            }
        }
    }
}