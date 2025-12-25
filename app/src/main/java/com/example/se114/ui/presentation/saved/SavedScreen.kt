package com.example.se114.ui.presentation.saved

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.se114.local.PreferencesManager
import com.example.se114.ui.presentation.home.PostCard // Import PostCard
import com.example.se114.ui.theme.AppTealDark

@Composable
fun SavedScreen(
    viewModel: SavedViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val preferencesManager = remember { PreferencesManager(context) }

    LaunchedEffect(Unit) {
        viewModel.loadSavedPosts()
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        // Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = AppTealDark,
            shadowElevation = 4.dp,
            shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp)) {
                Text(preferencesManager.getString("saved_posts"), style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.align(Alignment.Center))
            }
        }

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else if (uiState.savedPosts.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.BookmarkRemove, null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(preferencesManager.getString("empty_saved_posts"), fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(uiState.savedPosts, key = { it.id }) { post ->
                    PostCard(
                        post = post,
                        isSaved = true,
                        preferencesManager = preferencesManager,
                        onLikeClick = { /* Logic like ở saved có thể thêm sau */ },
                        onSaveClick = { viewModel.onUnsave(post.id) },
                        onHideClick = { },
                        onReportSubmitted = { },
                        onCommentClick = { },
                        onAvatarClick = { }
                    )
                }
            }
        }
    }
}