package com.example.se114.ui.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.se114.data.Post

@Composable
fun PostEventListener(
    viewModel: BasePostViewModel,
    content: @Composable (
        onLike: (Post) -> Unit,
        onSave: (Post, Boolean) -> Unit, // Boolean là trạng thái isSaved hiện tại
        onHide: (String) -> Unit,
        onReport: (String) -> Unit,
        onComment: (Post) -> Unit
    ) -> Unit
) {
    val messageState by viewModel.messageState.collectAsStateWithLifecycle()
    val preferencesManager = viewModel.preferencesManager

    // UI States cho các Dialog/Sheet
    var showCommentSheet by remember { mutableStateOf(false) }
    var selectedPostForComment by remember { mutableStateOf<Post?>(null) }
    var reportPostId by remember { mutableStateOf<String?>(null) }
    var showSnackbar by remember { mutableStateOf(false) }

    // Logic hiển thị Snackbar từ ViewModel
    LaunchedEffect(messageState.message) {
        if (messageState.message != null) {
            showSnackbar = true
            kotlinx.coroutines.delay(2000)
            showSnackbar = false
            viewModel.clearMessage()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Render nội dung màn hình chính (Home/Saved)
        content(
            { post -> viewModel.onToggleLike(post) },
            { post, isSaved -> viewModel.onToggleSave(post, isSaved) },
            { postId -> viewModel.onHidePost(postId) },
            { postId -> reportPostId = postId },
            { post ->
                selectedPostForComment = post
                showCommentSheet = true
            }
        )

        // --- CÁC THÀNH PHẦN CHUNG (Overlay) ---

        // 1. Snackbar
        if (showSnackbar && messageState.message != null) {
            Box(modifier = Modifier.fillMaxSize().padding(bottom = 16.dp), contentAlignment = Alignment.BottomCenter) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    containerColor = if (messageState.type == MessageType.ERROR) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.inverseSurface,
                    contentColor = if (messageState.type == MessageType.ERROR) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.inverseOnSurface,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(messageState.message ?: "", fontWeight = FontWeight.Medium)
                }
            }
        }

        // 2. Comment Bottom Sheet
        if (showCommentSheet && selectedPostForComment != null) {
            CommentBottomSheet(
                onDismiss = {
                    showCommentSheet = false
                    selectedPostForComment = null
                },
                postId = selectedPostForComment!!.id,
                preferencesManager = preferencesManager
            )
        }

        // 3. Report Dialog
        if (reportPostId != null) {
            ReportDialog(
                onDismiss = { reportPostId = null },
                onSubmit = { reasonKey, description ->
                    viewModel.onSubmitReport(reportPostId!!, reasonKey, description)
                },
                preferencesManager = preferencesManager,
                titleKey = "report_title",
                reasonKeys = listOf("report_fraud", "report_inappropriate", "report_trading", "report_offensive", "report_misinformation", "report_other"),
            )
        }
    }
}