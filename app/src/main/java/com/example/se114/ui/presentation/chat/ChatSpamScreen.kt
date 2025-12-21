package com.example.se114.ui.presentation.chat

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.se114.data.model.UserSummary
import com.example.se114.local.PreferencesManager
import com.example.se114.ui.theme.AppTealDark
import com.example.se114.ui.theme.DarkSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatSpamScreen(
    onBackClick: () -> Unit,
    onConversationClick: (String) -> Unit,
    preferencesManager: PreferencesManager,
    viewModel: ChatListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDarkMode = preferencesManager.isDarkMode
    val myId = preferencesManager.userId

    val backgroundColor = if (isDarkMode) DarkSurface else Color.White

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(preferencesManager.getString("spam_messages_title"), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(preferencesManager.getString("spam_messages_subtitle"), style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = AppTealDark)
            )
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.spamConversations.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(preferencesManager.getString("no_spam_messages"), color = Color.Gray)
                }
            } else {
                LazyColumn(contentPadding = PaddingValues(16.dp)) {
                    items(uiState.spamConversations, key = { it.id }) { conversation ->

                        val partnerId = conversation.participants.find { it != myId } ?: ""
                        val partnerInfo = uiState.userProfiles[partnerId] ?: UserSummary("User", "")

                        ChatListItem(
                            conversation = conversation,
                            partnerInfo = partnerInfo,
                            myId = myId,
                            onClick = { onConversationClick(conversation.id) },
                            isDarkMode = isDarkMode,
                            preferencesManager = preferencesManager
                        )
                    }
                }
            }
        }
    }
}