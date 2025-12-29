package com.example.se114.ui.presentation.chat

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.se114.data.model.ChatMessage
import com.example.se114.local.PreferencesManager
import com.example.se114.ui.theme.AppTealDark
import com.example.se114.ui.theme.DarkSurface
import android.util.Patterns
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.platform.LocalUriHandler
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import androidx.compose.material.icons.filled.LocationOn // Icon vị trí
import com.example.se114.utils.CurrentChatManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    conversationId: String,
    onBackClick: () -> Unit,
    onUserClick: (String) -> Unit, // Callback mới để xem Profile
    preferencesManager: PreferencesManager,
    viewModel: ChatDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isDarkMode = preferencesManager.isDarkMode
    val myId = preferencesManager.userId
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val isGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (isGranted) {
            // Nếu quyền được cấp, gọi hàm lấy vị trí (định nghĩa bên dưới)
            getCurrentLocationAndSend(context, fusedLocationClient, viewModel)
        } else {
            Toast.makeText(context, "Cần quyền vị trí để sử dụng tính năng này", Toast.LENGTH_SHORT).show()
        }
    }
    // Colors
    val headerColor = if (isDarkMode) Color.Black else AppTealDark
    val backgroundColor = if (isDarkMode) DarkSurface else Color(0xFFF5F7F8)
    val inputAreaColor = if (isDarkMode) Color.Black else Color.White
    val inputFieldColor = if (isDarkMode) Color(0xFF333333) else Color(0xFFF0F2F5)
    val inputTextColor = if (isDarkMode) Color.White else Color.Black

    // Menu state
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // Load conversation data
    LaunchedEffect(conversationId) {
        viewModel.loadConversation(conversationId)
    }

    // Identify partner ID for clicking
    val partnerId = uiState.conversation?.participants?.find { it != myId } ?: ""
    // Cập nhật ID người đang chat vào biến toàn cục
    LaunchedEffect(partnerId) {
        if (partnerId.isNotEmpty()) {
            CurrentChatManager.currentPartnerId = partnerId
        }
    }
    // Hiển thị Toast khi có lỗi gửi tin nhắn
    LaunchedEffect(uiState.sendError) {
        uiState.sendError?.let { error ->
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    // Dọn dẹp khi rời màn hình (Set về null)
    DisposableEffect(Unit) {
        onDispose {
            CurrentChatManager.currentPartnerId = null
        }
    }

    // Auto Scroll
    val listState = rememberLazyListState()
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

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

                    val partnerName = uiState.partnerProfile?.name
                        ?: uiState.conversation?.participantData?.get(partnerId)?.name
                        ?: "Chat"

                    // Logic fallback UI
                    val partnerAvatar = uiState.partnerProfile?.avatar
                        ?: uiState.conversation?.participantData?.get(partnerId)?.avatar
                        ?: ""

                    // Xử lý hiển thị cuối cùng (nếu rỗng thì lấy chữ cái đầu)
                    val displayAvatar = if (partnerAvatar.isNotEmpty()) partnerAvatar else partnerName.take(1).uppercase()

                    // Row chứa Avatar và Tên, có thể click để xem Profile
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                if (partnerId.isNotEmpty()) {
                                    onUserClick(partnerId)
                                }
                            }
                            .padding(vertical = 4.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            if (displayAvatar.startsWith("http")) {
                                AsyncImage(
                                    model = displayAvatar,
                                    contentDescription = "Avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = displayAvatar,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AppTealDark // Thêm màu cho chữ
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = partnerName,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                maxLines = 1
                            )
                        }
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(if(isDarkMode) Color(0xFF333333) else Color.White)
                        ) {
                            DropdownMenuItem(
                                text = { Text(preferencesManager.getString("delete_chat_menu"), color = Color.Red) },
                                onClick = {
                                    showMenu = false
                                    showDeleteConfirm = true
                                },
                                leadingIcon = { Icon(Icons.Default.Delete, null, tint = Color.Red) }
                            )
                        }
                    }
                }
            }
        },
        containerColor = backgroundColor,
        bottomBar = {
            if (uiState.isPartnerBanned) {
                Surface(color = Color.Red.copy(alpha = 0.1f), modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Tài khoản này đã bị vô hiệu hóa hoặc không tồn tại.",
                        color = Color.Red,
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            else if (uiState.isPending) {
                Surface(color = inputAreaColor, tonalElevation = 16.dp, shadowElevation = 16.dp) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = preferencesManager.getString("stranger_chat_warning_msg"),
                            fontSize = 13.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Button(
                                onClick = { viewModel.declineConversation(); onBackClick() },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color.LightGray, contentColor = Color.Black),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text(preferencesManager.getString("btn_decline_chat")) }
                            Button(
                                onClick = viewModel::acceptConversation,
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = AppTealDark, contentColor = Color.White),
                                shape = RoundedCornerShape(12.dp)
                            ) { Text(preferencesManager.getString("btn_accept_chat")) }
                        }
                    }
                }
            } else {
                Surface(color = inputAreaColor, tonalElevation = 8.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // --- NÚT GỬI VỊ TRÍ (THÊM MỚI) ---
                        IconButton(
                            onClick = {
                                // Kiểm tra quyền trước khi bấm
                                if (ActivityCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.ACCESS_FINE_LOCATION
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    // Chưa có quyền thì xin
                                    locationPermissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                } else {
                                    // Đã có quyền thì lấy vị trí luôn
                                    getCurrentLocationAndSend(context, fusedLocationClient, viewModel)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = "Location",
                                tint = AppTealDark
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))
                        OutlinedTextField(
                            value = uiState.messageInput,
                            onValueChange = viewModel::onMessageInputChange,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp, max = 100.dp),
                            placeholder = { Text(preferencesManager.getString("chat_type_message"), fontSize = 15.sp, color = Color.Gray) },
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
                            onClick = { viewModel.sendMessage() },
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
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(uiState.messages) { msg ->
                    MessageBubble(
                        message = msg,
                        isMe = msg.senderId == myId,
                        isDarkMode = isDarkMode
                    )
                }
            }
        }

        if (showDeleteConfirm) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = false },
                title = { Text(preferencesManager.getString("delete_chat_confirm_title")) },
                text = { Text(preferencesManager.getString("delete_chat_confirm_msg")) },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteChatOneSided {
                                showDeleteConfirm = false
                                onBackClick()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text(preferencesManager.getString("delete"))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = false }) {
                        Text(preferencesManager.getString("cancel"))
                    }
                }
            )
        }
    }
}

@Composable
fun MessageBubble(
    message: ChatMessage,
    isMe: Boolean,
    isDarkMode: Boolean
) {
    val bubbleColor = if (isMe) AppTealDark else (if(isDarkMode) Color(0xFF333333) else Color(0xFFE4E6EB))
    val textColor = if (isMe) Color.White else (if(isDarkMode) Color.White else Color.Black)

    // Màu cho đường link: Nếu là mình (nền xanh) thì link màu vàng nhạt/trắng, nếu là bạn (nền xám) thì link màu xanh dương
    val linkColor = if (isMe) Color(0xFFFFEB3B) else Color(0xFF2196F3)

    val uriHandler = LocalUriHandler.current

    // Xử lý chuỗi tin nhắn để tìm và format link
    val annotatedString = buildAnnotatedString {
        val text = message.content
        append(text)

        // Sử dụng Regex của Android để tìm URL
        val matcher = Patterns.WEB_URL.matcher(text)
        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()

            // 1. Thêm style (gạch chân + đổi màu)
            addStyle(
                style = SpanStyle(
                    color = linkColor,
                    textDecoration = TextDecoration.Underline
                ),
                start = start,
                end = end
            )

            // 2. Gắn tag URL để lát nữa bắt sự kiện click
            addStringAnnotation(
                tag = "URL",
                annotation = matcher.group(),
                start = start,
                end = end
            )
        }
    }

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
            // Thay thế Text bằng ClickableText
            ClickableText(
                text = annotatedString,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = TextStyle(
                    color = textColor,
                    fontSize = 15.sp,
                    lineHeight = 20.sp
                ),
                onClick = { offset ->
                    // Kiểm tra xem vị trí click có phải là URL không
                    annotatedString.getStringAnnotations(tag = "URL", start = offset, end = offset)
                        .firstOrNull()?.let { annotation ->
                            var url = annotation.item
                            // Đảm bảo URL có http/https để tránh crash
                            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                url = "https://$url"
                            }
                            try {
                                uriHandler.openUri(url)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                }
            )
        }
    }
}
private fun getCurrentLocationAndSend(
    context: android.content.Context,
    fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient,
    viewModel: ChatDetailViewModel
) {
    try {
        // Kiểm tra lại quyền lần cuối cho chắc (yêu cầu của Android Lint)
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(context, "Đang lấy vị trí...", Toast.LENGTH_SHORT).show()

            // Lấy vị trí ưu tiên độ chính xác cao
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        // Tạo đường link Google Maps chuẩn
                        // Format: https://www.google.com/maps/search/?api=1&query=LAT,LNG
                        val mapLink = "https://www.google.com/maps/search/?api=1&query=${location.latitude},${location.longitude}"

                        // Gọi ViewModel gửi đi
                        viewModel.sendLocationMessage(mapLink)
                    } else {
                        Toast.makeText(context, "Không thể xác định vị trí. Hãy bật GPS.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(context, "Lỗi lấy vị trí: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}