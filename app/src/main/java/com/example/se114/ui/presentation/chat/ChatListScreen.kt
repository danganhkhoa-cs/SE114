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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.se114.data.model.Conversation
import com.example.se114.data.model.FriendshipState
import com.example.se114.data.model.UserSummary
import com.example.se114.local.PreferencesManager
import com.example.se114.ui.theme.AppTealDark
import com.example.se114.ui.theme.DarkSurface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatListScreen(
    onConversationClick: (String) -> Unit,
    onSpamClick: () -> Unit,
    onUserClick: (String) -> Unit,
    preferencesManager: PreferencesManager,
    viewModel: ChatListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDarkMode = preferencesManager.isDarkMode

    val backgroundColor = if (isDarkMode) DarkSurface else Color.White

    // --- CUSTOM UI: TEAL HEADER ---
    Box(modifier = Modifier.fillMaxSize().background(if (isDarkMode) DarkSurface else AppTealDark)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 48.dp, bottom = 24.dp)
            ) {
                // Title & Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = preferencesManager.getString("chat_title"),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Nút thêm bạn
                        IconButton(
                            onClick = { viewModel.showAddFriendDialog() },
                            modifier = Modifier.background(Color.White.copy(0.2f), CircleShape).size(40.dp)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Add Friend", tint = Color.White)
                        }

                        // Nút quản lý bạn bè + Badge thông báo đỏ
                        val requestCount = uiState.receivedFriendRequests.size

                        Box {
                            IconButton(
                                onClick = { viewModel.showFriendsManagerDialog() },
                                modifier = Modifier.background(Color.White.copy(0.2f), CircleShape).size(40.dp)
                            ) {
                                Icon(Icons.Default.Group, contentDescription = "Friends", tint = Color.White)
                            }
                            if (requestCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color(0xFFFF5252), CircleShape)
                                        .align(Alignment.TopEnd)
                                        .offset(x = (-2).dp, y = 2.dp)
                                )
                            }
                        }
                    }
                }

                // Search Bar Custom
                TextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(preferencesManager.getString("chat_search"), color = Color.White.copy(0.7f)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.White) },
                    shape = RoundedCornerShape(24.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(0.2f),
                        unfocusedContainerColor = Color.White.copy(0.2f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White
                    ),
                    singleLine = true
                )
            }

            // List Content Surface
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = backgroundColor,
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 24.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
                ) {
                    // --- MỤC TIN NHẮN CHỜ (SPAM) ---
                    if (uiState.spamConversations.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(onClick = onSpamClick)
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(Color.LightGray.copy(alpha = 0.3f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Gray)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        preferencesManager.getString("spam_messages_title"),
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDarkMode) Color.White else Color.Black
                                    )
                                    Text(
                                        "${uiState.spamConversations.size} ${preferencesManager.getString("messages_count_suffix")}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color(0xFFFF5252), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(8.dp))

                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForwardIos,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = Color.Gray
                                )
                            }
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
                        }
                    }

                    // Conversations List
                    val displayList = uiState.inboxConversations
                    if (displayList.isEmpty()) {
                        item {
                            Box(modifier = Modifier.fillMaxWidth().padding(top = 32.dp), contentAlignment = Alignment.Center) {
                                Text(preferencesManager.getString("no_messages"), color = Color.Gray)
                            }
                        }
                    } else {
                        items(displayList, key = { it.id }) { conversation ->
                            val myId = preferencesManager.userId
                            val partnerId = conversation.participants.find { it != myId } ?: ""
                            val partnerInfo = uiState.userProfiles[partnerId] ?: UserSummary("User", "")

                            ChatListItem(
                                conversation = conversation,
                                partnerInfo = partnerInfo,
                                myId = myId,
                                onClick = { onConversationClick(conversation.id) },
                                onDelete = { viewModel.deleteConversation(conversation.id) },
                                isDarkMode = isDarkMode,
                                onAvatarClick = { onUserClick(partnerId) },
                                preferencesManager = preferencesManager
                            )
                        }
                    }
                }
            }
        }
    }

    // --- DIALOGS ---
    if (uiState.isShowingAddFriendDialog) {
        val allRelatedConvs = remember(uiState) {
            (uiState.inboxConversations +
                    uiState.friendConversations +
                    uiState.spamConversations +
                    uiState.sentFriendRequests +
                    uiState.receivedFriendRequests).distinctBy { it.id }
        }

        AddFriendDialog(
            phoneQuery = uiState.searchPhoneQuery,
            onPhoneChange = viewModel::onSearchPhoneChange,
            onSearch = viewModel::searchUsersByPhone,
            onDismiss = viewModel::hideAddFriendDialog,
            searchResults = uiState.searchResults,
            onAddFriend = viewModel::sendFriendRequest,
            onCancelRequest = { userId ->
                val conv = allRelatedConvs.find { it.participants.contains(userId) }
                if (conv != null) viewModel.unfriend(conv.id)
            },
            onAcceptRequest = { userId ->
                val conv = allRelatedConvs.find { it.participants.contains(userId) }
                if (conv != null) viewModel.acceptRequest(conv.id)
            },
            isLoading = uiState.isLoading,
            error = uiState.error,
            preferencesManager = preferencesManager,
            allConversations = allRelatedConvs,
            currentUserId = preferencesManager.userId,
            onUserClick = onUserClick
        )
    }

    if (uiState.isShowingFriendsManagerDialog) {
        FriendsManagerDialog(
            receivedRequests = uiState.receivedFriendRequests,
            friends = uiState.friendConversations,
            userProfiles = uiState.userProfiles,
            currentUserId = preferencesManager.userId,
            onAccept = viewModel::acceptRequest,
            onDecline = viewModel::declineRequest,
            onUnfriend = viewModel::unfriend,
            onDismiss = viewModel::hideFriendsManagerDialog,
            preferencesManager = preferencesManager,
            onUserClick = onUserClick
        )
    }
}

@Composable
fun AddFriendDialog(
    phoneQuery: String,
    onPhoneChange: (String) -> Unit,
    onSearch: () -> Unit,
    onDismiss: () -> Unit,
    searchResults: List<UserSummary>,
    onAddFriend: (UserSummary) -> Unit,
    onCancelRequest: (String) -> Unit,
    onAcceptRequest: (String) -> Unit,
    isLoading: Boolean,
    error: String?,
    preferencesManager: PreferencesManager,
    allConversations: List<Conversation>,
    currentUserId: String,
    onUserClick: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(preferencesManager.getString("add_new_friend_title"), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = AppTealDark)
                Text(preferencesManager.getString("search_by_phone_hint"), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = phoneQuery,
                    onValueChange = onPhoneChange,
                    label = { Text(preferencesManager.getString("phone_label")) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = { IconButton(onClick = onSearch) { Icon(Icons.Default.Search, null, tint = AppTealDark) } },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = AppTealDark, focusedLabelColor = AppTealDark),
                    singleLine = true
                )
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))

                Spacer(modifier = Modifier.height(20.dp))

                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AppTealDark) }
                } else if (searchResults.isNotEmpty()) {
                    Text(preferencesManager.getString("results_label"), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
                    LazyColumn {
                        items(searchResults) { user ->
                            val existingConv = allConversations.find { it.participants.contains(user.uid) }

                            val isFriend = existingConv?.friendshipState == FriendshipState.FRIENDS
                            val isPendingSentByMe = existingConv?.friendshipState == FriendshipState.PENDING && existingConv.friendRequestSenderId == currentUserId
                            val isPendingReceived = existingConv?.friendshipState == FriendshipState.PENDING && existingConv.friendRequestSenderId != currentUserId

                            Row(
                                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onUserClick(user.uid) }.padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // --- AVATAR TRONG SEARCH RESULTS ---
                                Surface(shape = CircleShape, color = AppTealDark.copy(alpha = 0.1f), modifier = Modifier.size(48.dp)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if (user.avatar.startsWith("http")) {
                                            AsyncImage(
                                                model = user.avatar,
                                                contentDescription = "Avatar",
                                                modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Text(user.avatar.ifEmpty{user.name.take(1).uppercase()}, color = AppTealDark, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(user.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                    Text(user.phone, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                    // --- HIỂN THỊ EMAIL (NEW) ---
                                    if (user.email.isNotEmpty()) {
                                        Text(user.email, style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontSize = 11.sp)
                                    }
                                }

                                when {
                                    isFriend -> Text(preferencesManager.getString("friend_status_friend"), color = AppTealDark, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(end = 8.dp))
                                    isPendingSentByMe -> OutlinedButton(onClick = { onCancelRequest(user.uid) }, modifier = Modifier.height(36.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Gray)) { Text(preferencesManager.getString("friend_status_sent"), fontSize = 12.sp) }
                                    isPendingReceived -> Button(onClick = { onAcceptRequest(user.uid) }, modifier = Modifier.height(36.dp), colors = ButtonDefaults.buttonColors(containerColor = AppTealDark)) { Text(preferencesManager.getString("friend_status_received"), fontSize = 12.sp) }
                                    else -> Button(onClick = { onAddFriend(user) }, modifier = Modifier.height(36.dp), colors = ButtonDefaults.buttonColors(containerColor = AppTealDark)) { Text(preferencesManager.getString("btn_add_friend"), fontSize = 12.sp) }
                                }
                            }
                        }
                    }
                } else if (phoneQuery.isNotEmpty() && !isLoading && error == null) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) { Text(preferencesManager.getString("user_not_found"), color = Color.Gray) }
                }

                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) { Text(preferencesManager.getString("close"), color = Color.Gray) }
            }
        }
    }
}

@Composable
fun FriendsManagerDialog(
    receivedRequests: List<Conversation>,
    friends: List<Conversation>,
    userProfiles: Map<String, UserSummary>,
    currentUserId: String,
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit,
    onUnfriend: (String) -> Unit,
    onDismiss: () -> Unit,
    preferencesManager: PreferencesManager,
    onUserClick: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(preferencesManager.getString("tab_requests"), preferencesManager.getString("tab_friends"))

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var friendToDeleteId by remember { mutableStateOf<String?>(null) }
    var friendToDeleteName by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth().height(600.dp)) {
            Column(modifier = Modifier.padding(20.dp)) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = AppTealDark,
                    divider = {},
                    indicator = { tabPositions -> TabRowDefaults.SecondaryIndicator(Modifier.tabIndicatorOffset(tabPositions[selectedTab]), color = AppTealDark, height = 3.dp) }
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(title, fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal, color = if (selectedTab == index) AppTealDark else Color.Gray)
                                    if (index == 0 && receivedRequests.isNotEmpty()) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(modifier = Modifier.size(20.dp).background(Color.Red, CircleShape), contentAlignment = Alignment.Center) {
                                            Text(receivedRequests.size.toString(), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        0 -> { // Requests
                            if (receivedRequests.isEmpty()) {
                                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(preferencesManager.getString("no_friend_requests"), color = Color.Gray)
                                }
                            } else {
                                LazyColumn {
                                    items(receivedRequests) { conv ->
                                        val partnerId = conv.participants.find { it != currentUserId } ?: ""
                                        val partner = userProfiles[partnerId] ?: UserSummary("", "Unknown")

                                        Card(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable { onUserClick(partnerId) },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        ) {
                                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                                                    if (partner.avatar.startsWith("http")) {
                                                        AsyncImage(
                                                            model = partner.avatar,
                                                            contentDescription = "Avatar",
                                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                            contentScale = ContentScale.Crop
                                                        )
                                                    } else {
                                                        Text(partner.avatar.ifEmpty{partner.name.take(1).uppercase()}, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = AppTealDark)
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(partner.name, fontWeight = FontWeight.Bold)
                                                    Text(preferencesManager.getString("request_friend_msg"), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                                }
                                            }
                                            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Button(onClick = { onAccept(conv.id) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = AppTealDark), shape = RoundedCornerShape(8.dp)) { Text(preferencesManager.getString("btn_accept")) }
                                                OutlinedButton(onClick = { onDecline(conv.id) }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(8.dp)) { Text(preferencesManager.getString("delete"), color = Color.Gray) }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        1 -> { // Friends
                            if (friends.isEmpty()) {
                                Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Group, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(preferencesManager.getString("no_friends_list"), color = Color.Gray)
                                }
                            } else {
                                LazyColumn {
                                    items(friends) { conv ->
                                        val partnerId = conv.participants.find { it != currentUserId } ?: ""
                                        val partner = userProfiles[partnerId] ?: UserSummary("", "Unknown")

                                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onUserClick(partnerId) }, verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(AppTealDark.copy(alpha = 0.1f)), contentAlignment = Alignment.Center) {
                                                if (partner.avatar.startsWith("http")) {
                                                    AsyncImage(
                                                        model = partner.avatar,
                                                        contentDescription = "Avatar",
                                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else {
                                                    Text(partner.avatar.ifEmpty{partner.name.take(1).uppercase()}, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = AppTealDark)
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Text(partner.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium, fontSize = 16.sp)

                                            IconButton(onClick = {
                                                friendToDeleteId = conv.id
                                                friendToDeleteName = partner.name
                                                showDeleteConfirmDialog = true
                                            }) {
                                                Icon(Icons.Default.Delete, contentDescription = "Unfriend", tint = Color.Gray)
                                            }
                                        }
                                        HorizontalDivider(color = Color.LightGray.copy(0.2f))
                                    }
                                }
                            }
                        }
                    }
                }
                Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), contentAlignment = Alignment.CenterEnd) {
                    TextButton(onClick = onDismiss) { Text(preferencesManager.getString("close"), color = AppTealDark, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text(preferencesManager.getString("unfriend_title")) },
            text = { Text(String.format(preferencesManager.getString("unfriend_confirm_msg"), friendToDeleteName)) },
            confirmButton = {
                Button(
                    onClick = {
                        friendToDeleteId?.let { onUnfriend(it) }
                        showDeleteConfirmDialog = false
                        friendToDeleteId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(preferencesManager.getString("delete"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text(preferencesManager.getString("cancel"))
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun ChatListItem(
    conversation: Conversation,
    partnerInfo: UserSummary,
    myId: String,
    onClick: () -> Unit,
    onDelete: () -> Unit = {},
    isDarkMode: Boolean,
    onAvatarClick: () -> Unit = {},
    preferencesManager: PreferencesManager
) {
    val isUnread = conversation.isUnread(myId)
    val nameColor = if (isDarkMode) Color.White else Color.Black
    val messageColor = if (isUnread) (if (!isDarkMode) Color.Black else Color.White) else Color.Gray
    val timeString = try {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(conversation.lastMessageTime))
    } catch (e: Exception) { "" }

    val displayMessage = when (conversation.lastMessage) {
        "Đã gửi lời mời kết bạn" -> preferencesManager.getString("msg_sent_friend_request")
        "Bắt đầu cuộc trò chuyện" -> preferencesManager.getString("msg_start_conversation")
        else -> conversation.lastMessage
    }

    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(modifier = Modifier.size(56.dp).clickable { onAvatarClick() }, shape = CircleShape, color = AppTealDark) {
            Box(contentAlignment = Alignment.Center) {
                if (partnerInfo.avatar.startsWith("http")) {
                    AsyncImage(
                        model = partnerInfo.avatar,
                        contentDescription = "Avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(partnerInfo.avatar.ifEmpty { partnerInfo.name.take(1).uppercase() }, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(partnerInfo.name, style = MaterialTheme.typography.bodyLarge, fontWeight = if (isUnread) FontWeight.ExtraBold else FontWeight.Bold, color = nameColor, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(timeString, style = MaterialTheme.typography.bodySmall, color = messageColor, fontWeight = if (isUnread) FontWeight.Bold else FontWeight.Normal)
            }
            Text(displayMessage, style = MaterialTheme.typography.bodyMedium, color = if (isUnread) (if (!isDarkMode) Color.Black else Color.White) else messageColor, fontWeight = if (isUnread) FontWeight.ExtraBold else FontWeight.Normal, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (isUnread) { Spacer(modifier = Modifier.width(8.dp)); Box(modifier = Modifier.size(12.dp).background(Color(0xFFFF5252), CircleShape)) }
    }
}