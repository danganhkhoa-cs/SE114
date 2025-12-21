package com.example.se114.ui.presentation.other_profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.se114.local.PreferencesManager
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherProfileScreen(
    preferencesManager: PreferencesManager,
    onBackClick: () -> Unit,
    onNavigateToChat: (String) -> Unit,
    viewModel: OtherProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is OtherProfileEvent.NavigateToChat -> {
                    onNavigateToChat(event.conversationId)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(preferencesManager.getString("other_profile_title")) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.errorMessage != null) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text(text = uiState.errorMessage ?: "Error", color = Color.Red)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Profile
                Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF00695C), Color(0xFF4DB6AC))
                                )
                            )
                    )

                    // Avatar
                    Surface(
                        modifier = Modifier
                            .size(120.dp)
                            .align(Alignment.BottomCenter)
                            .shadow(8.dp, CircleShape),
                        shape = CircleShape,
                        color = Color.White
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = uiState.userAvatar,
                                fontSize = 48.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00695C)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = uiState.userName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                val displayBio = if (uiState.userBio.isBlank() || uiState.userBio == "Chưa có giới thiệu") preferencesManager.getString("no_bio") else uiState.userBio
                Text(
                    text = displayBio,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val buttonText = when (uiState.friendshipStatus) {
                        FriendshipStatus.FRIEND -> preferencesManager.getString("friends")
                        FriendshipStatus.SENT_REQUEST -> preferencesManager.getString("btn_cancel_request")
                        FriendshipStatus.RECEIVED_REQUEST -> preferencesManager.getString("friend_accept")
                        FriendshipStatus.NONE -> preferencesManager.getString("btn_add_friend")
                    }

                    val buttonColor = if (uiState.friendshipStatus == FriendshipStatus.FRIEND)
                        MaterialTheme.colorScheme.secondaryContainer
                    else if (uiState.friendshipStatus == FriendshipStatus.SENT_REQUEST)
                        Color.LightGray
                    else
                        MaterialTheme.colorScheme.primary

                    val buttonContentColor = if (uiState.friendshipStatus == FriendshipStatus.FRIEND)
                        MaterialTheme.colorScheme.onSecondaryContainer
                    else if (uiState.friendshipStatus == FriendshipStatus.SENT_REQUEST)
                        Color.Black
                    else
                        MaterialTheme.colorScheme.onPrimary

                    Button(
                        onClick = {
                            when (uiState.friendshipStatus) {
                                FriendshipStatus.NONE -> viewModel.onAddFriendClick()
                                FriendshipStatus.RECEIVED_REQUEST -> viewModel.onAcceptFriendClick()
                                FriendshipStatus.SENT_REQUEST -> viewModel.onCancelFriendRequest()
                                else -> {}
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = buttonColor,
                            contentColor = buttonContentColor
                        ),
                        enabled = true
                    ) {
                        Text(buttonText)
                    }

                    OutlinedButton(
                        onClick = viewModel::onMessageClick,
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Message, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(preferencesManager.getString("message"))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                val displayAddress = if (uiState.address == "Chưa cập nhật") preferencesManager.getString("not_updated") else uiState.address
                val displayJob = if (uiState.job == "Chưa cập nhật") preferencesManager.getString("not_updated") else uiState.job
                val displayPhone = if (uiState.phone == "Ẩn") preferencesManager.getString("hidden_info") else uiState.phone
                val displayDate = if (uiState.joinedDate == "Thành viên LocaSOS") preferencesManager.getString("joined_date") else uiState.joinedDate

                InfoSection(
                    preferencesManager = preferencesManager,
                    address = displayAddress,
                    phone = displayPhone,
                    gender = uiState.gender,
                    job = displayJob,
                    rating = uiState.rating,
                    reviewCount = uiState.reviewCount
                )
            }
        }
    }
}

@Composable
fun InfoSection(
    preferencesManager: PreferencesManager,
    address: String,
    phone: String,
    gender: String,
    job: String,
    rating: Float,
    reviewCount: Int
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        ProfileInfoItem(icon = Icons.Default.Work, label = preferencesManager.getString("current_job"), value = job)
        ProfileInfoItem(icon = Icons.Default.LocationOn, label = preferencesManager.getString("address"), value = address)
        ProfileInfoItem(icon = Icons.Default.Phone, label = preferencesManager.getString("phone_number"), value = phone)
        ProfileInfoItem(icon = Icons.Default.Person, label = preferencesManager.getString("gender"), value = preferencesManager.getString(gender))

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = preferencesManager.getString("rating_score"),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "$reviewCount ${preferencesManager.getString("reviews")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = String.format("%.1f", rating),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107))
                }
            }
        }
    }
}

@Composable
fun ProfileInfoItem(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
    }
}