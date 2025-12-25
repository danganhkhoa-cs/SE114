package com.example.se114.ui.presentation.other_profile

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.se114.data.model.Review
import com.example.se114.local.PreferencesManager
import kotlinx.coroutines.flow.collectLatest

val AppTealDark = Color(0xFF00695C)

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

    var showMenu by remember { mutableStateOf(false) }
    var showBlockConfirmDialog by remember { mutableStateOf(false) }
    var showReviewDialog by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is OtherProfileEvent.NavigateToChat -> {
                    onNavigateToChat(event.conversationId)
                }
            }
        }
    }

    LaunchedEffect(uiState.reportToastMessage) {
        if (uiState.reportToastMessage != null) {
            Toast.makeText(context, uiState.reportToastMessage, Toast.LENGTH_SHORT).show()

            // Nếu là success hoặc duplicate thì đều đóng dialog report lại cho gọn
            showReportDialog = false

            viewModel.clearReportToastMessage()
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
                },
                actions = {
                    if (!uiState.isLoading && !uiState.isBlocked && uiState.errorMessage == null) {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            // BLOCK
                            DropdownMenuItem(
                                text = { Text(preferencesManager.getString("block_user"), color = Color.Red) },
                                onClick = {
                                    showMenu = false
                                    showBlockConfirmDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Block, null, tint = Color.Red) }
                            )

                            // REPORT USER
                            DropdownMenuItem(
                                text = { Text(preferencesManager.getString("report_user"), color = MaterialTheme.colorScheme.onSurface) },
                                onClick = {
                                    showMenu = false
                                    showReportDialog = true
                                },
                                leadingIcon = { Icon(Icons.Default.Report, null, tint = MaterialTheme.colorScheme.onSurface) }
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isBlocked) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Icon(
                        Icons.Default.Block,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = uiState.errorMessage ?: preferencesManager.getString("user_unavailable"),
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.errorMessage != null && !uiState.isBlocked) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text(text = uiState.errorMessage ?: preferencesManager.getString("error"), color = Color.Red)
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
                                    colors = listOf(AppTealDark, Color(0xFF4DB6AC))
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
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            if (uiState.userAvatar.startsWith("http")) {
                                AsyncImage(
                                    model = uiState.userAvatar,
                                    contentDescription = "User Avatar",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = uiState.userAvatar.ifEmpty { uiState.userName.take(1).uppercase() },
                                    fontSize = 48.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppTealDark
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = uiState.userName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // --- ACTION BUTTONS ---
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

                Spacer(modifier = Modifier.height(16.dp))

                val displayBio = if (uiState.userBio.isBlank() || uiState.userBio == "Chưa có giới thiệu") preferencesManager.getString("no_bio") else uiState.userBio
                Text(
                    text = displayBio,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                val displayAddress = if (uiState.address == "Chưa cập nhật") preferencesManager.getString("not_updated") else uiState.address
                val displayJob = if (uiState.job == "Chưa cập nhật") preferencesManager.getString("not_updated") else uiState.job
                val displayPhone = if (uiState.phone == "Ẩn") preferencesManager.getString("hidden_info") else uiState.phone

                InfoSection(
                    preferencesManager = preferencesManager,
                    address = displayAddress,
                    phone = displayPhone,
                    gender = uiState.gender,
                    job = displayJob,
                    rating = uiState.rating,
                    reviewCount = uiState.reviewCount,
                    onSeeReviewsClick = {
                        viewModel.loadReviews(reset = true)
                        showReviewDialog = true
                    }
                )

                Spacer(modifier = Modifier.height(24.dp))

                // --- PHẦN ĐÁNH GIÁ (VOTE) ---
                if (!uiState.isBlocked && uiState.canRate) {
                    RatingInputSection(
                        preferencesManager = preferencesManager,
                        currentRating = uiState.myRating,
                        currentComment = uiState.myComment,
                        onRate = { stars, comment ->
                            viewModel.submitRating(stars, comment)
                        },
                        onDelete = { viewModel.deleteRating() }
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                } else if (!uiState.canRate && !uiState.isBlocked) {
                    Text(
                        text = preferencesManager.getString("not_enough_messages"),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // --- DIALOGS ---

    if (showBlockConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showBlockConfirmDialog = false },
            title = { Text(preferencesManager.getString("block_user_title")) },
            text = { Text(preferencesManager.getString("block_user_msg")) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.blockUser()
                        showBlockConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(preferencesManager.getString("block"))
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockConfirmDialog = false }) {
                    Text(preferencesManager.getString("cancel"))
                }
            }
        )
    }

    if (showReviewDialog) {
        ReviewListDialog(
            reviews = uiState.reviewsList,
            authorAvatars = uiState.reviewAuthorAvatars,
            totalCount = uiState.reviewCount,
            isLoading = uiState.isReviewsLoading,
            onDismiss = { showReviewDialog = false },
            onLoadMore = { viewModel.loadReviews(reset = false) },
            preferencesManager = preferencesManager
        )
    }

    if (showReportDialog) {
        ReportUserDialog(
            preferencesManager = preferencesManager,
            onDismiss = { showReportDialog = false },
            onSubmit = { reason, description ->
                viewModel.submitReport(reason, description)
            }
        )
    }
}

// --- REPORT USER DIALOG ---
@Composable
fun ReportUserDialog(
    preferencesManager: PreferencesManager,
    onDismiss: () -> Unit,
    onSubmit: (String, String) -> Unit
) {
    val reasons = listOf(
        "spam" to preferencesManager.getString("report_reason_spam"),
        "harassment" to preferencesManager.getString("report_reason_harassment"),
        "fake" to preferencesManager.getString("report_reason_fake"),
        "inappropriate" to preferencesManager.getString("report_reason_inappropriate"),
        "other" to preferencesManager.getString("report_reason_other")
    )

    var selectedReasonKey by remember { mutableStateOf(reasons[0].first) }
    var description by remember { mutableStateOf("") }
    val isDescriptionRequired = selectedReasonKey == "other"
    val isSubmitEnabled = !isDescriptionRequired || (isDescriptionRequired && description.isNotBlank())

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = preferencesManager.getString("report_user_title"),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(16.dp))

                reasons.forEach { (key, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = (key == selectedReasonKey),
                                onClick = { selectedReasonKey = key },
                                role = Role.RadioButton
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (key == selectedReasonKey),
                            onClick = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(preferencesManager.getString("report_description")) },
                    placeholder = { Text(preferencesManager.getString("report_description_hint")) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5,
                    isError = isDescriptionRequired && description.isBlank()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text(preferencesManager.getString("cancel"))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onSubmit(selectedReasonKey, description) },
                        enabled = isSubmitEnabled,
                        colors = ButtonDefaults.buttonColors(containerColor = AppTealDark)
                    ) {
                        Text(preferencesManager.getString("report_submit"))
                    }
                }
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
    reviewCount: Int,
    onSeeReviewsClick: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        ProfileInfoItem(icon = Icons.Default.Work, label = preferencesManager.getString("current_job"), value = job)
        ProfileInfoItem(icon = Icons.Default.LocationOn, label = preferencesManager.getString("address"), value = address)
        ProfileInfoItem(icon = Icons.Default.Phone, label = preferencesManager.getString("phone_number"), value = phone)
        ProfileInfoItem(icon = Icons.Default.Person, label = preferencesManager.getString("gender"), value = preferencesManager.getString(gender))

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth().clickable { onSeeReviewsClick() },
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
                    Text(
                        text = preferencesManager.getString("see_reviews"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
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

@Composable
fun RatingInputSection(
    preferencesManager: PreferencesManager,
    currentRating: Int,
    currentComment: String,
    onRate: (Int, String) -> Unit,
    onDelete: () -> Unit
) {
    var selectedRating by remember(currentRating) { mutableIntStateOf(currentRating) }
    var comment by remember(currentComment) { mutableStateOf(currentComment) }
    val isEditing = currentRating > 0

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (isEditing) preferencesManager.getString("your_rating") else preferencesManager.getString("write_review"),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            // 5 Stars Row
            Row(horizontalArrangement = Arrangement.Center) {
                for (i in 1..5) {
                    Icon(
                        imageVector = if (i <= selectedRating) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "$i Star",
                        tint = if (i <= selectedRating) Color(0xFFFFC107) else Color.Gray,
                        modifier = Modifier
                            .size(32.dp)
                            .clickable { selectedRating = i }
                            .padding(2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                placeholder = { Text(preferencesManager.getString("write_review_hint")) },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (isEditing) {
                    TextButton(onClick = onDelete) {
                        Text(preferencesManager.getString("delete_review"), color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { onRate(selectedRating, comment) },
                    enabled = selectedRating > 0
                ) {
                    Text(preferencesManager.getString(if (isEditing) "save" else "submit_review"))
                }
            }
        }
    }
}

@Composable
fun ReviewListDialog(
    reviews: List<Review>,
    authorAvatars: Map<String, String>,
    totalCount: Int,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onLoadMore: () -> Unit,
    preferencesManager: PreferencesManager
) {
    val currentUserId = preferencesManager.userId
    val currentUserAvatar = preferencesManager.userAvatar
    val currentUserName = preferencesManager.userName

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "${preferencesManager.getString("rating_reviews")} ($totalCount)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (reviews.isEmpty() && !isLoading) {
                    Text(
                        text = preferencesManager.getString("no_reviews_yet"),
                        modifier = Modifier.padding(24.dp).align(Alignment.CenterHorizontally),
                        color = Color.Gray
                    )
                } else {
                    androidx.compose.foundation.lazy.LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        items(reviews.size) { index ->
                            ReviewItem(
                                review = reviews[index],
                                authorAvatars = authorAvatars,
                                currentUserId = currentUserId,
                                currentUserAvatar = currentUserAvatar,
                                currentUserName = currentUserName
                            )
                            HorizontalDivider(color = Color.Gray.copy(alpha = 0.2f), modifier = Modifier.padding(top = 8.dp))
                        }

                        item {
                            if (isLoading) {
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            } else if (reviews.size < totalCount) {
                                TextButton(
                                    onClick = onLoadMore,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(preferencesManager.getString("load_more_reviews"))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                    Text(preferencesManager.getString("close"))
                }
            }
        }
    }
}

@Composable
fun ReviewItem(
    review: Review,
    authorAvatars: Map<String, String>,
    currentUserId: String = "",
    currentUserAvatar: String = "",
    currentUserName: String = ""
) {
    val isMe = review.reviewerId == currentUserId && currentUserId.isNotEmpty()
    val avatarToShow = if (isMe) {
        currentUserAvatar
    } else {
        authorAvatars[review.reviewerId] ?: review.reviewerAvatar
    }
    val nameToShow = if (isMe) currentUserName else review.reviewerName

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            shape = CircleShape,
            modifier = Modifier.size(40.dp),
            color = AppTealDark.copy(alpha = 0.2f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                if (avatarToShow.startsWith("http")) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(avatarToShow)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Reviewer Avatar",
                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Text(
                        text = if (nameToShow.isNotEmpty()) nameToShow.take(1).uppercase() else "?",
                        fontWeight = FontWeight.Bold,
                        color = AppTealDark
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(nameToShow, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)

            Row(verticalAlignment = Alignment.CenterVertically) {
                repeat(5) { i ->
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = if (i < review.rating) Color(0xFFFFC107) else Color.LightGray,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            if (review.comment.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(review.comment, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}