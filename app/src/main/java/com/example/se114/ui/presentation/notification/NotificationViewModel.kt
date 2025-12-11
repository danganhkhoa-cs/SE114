package com.example.se114.ui.presentation.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

// --- DATA CLASSES & ENUMS (Chuyển từ Screen sang đây) ---

enum class NotificationType {
    LIKE, COMMENT, REPLY, FRIEND_REQUEST,
    SOS_SUPPORT_ACCEPTED, EMERGENCY_APPROVED, EMERGENCY_REJECTED
}

enum class NotificationTab {
    SOCIAL, EMERGENCY
}

data class NotificationItem(
    val id: String,
    val type: NotificationType,
    val userName: String,
    val userAvatar: String? = null,
    val message: String,
    val timestamp: Long,
    val isRead: Boolean = false,
    val postId: String? = null,
    val requestId: String? = null
)

// --- UI STATE ---

data class NotificationUiState(
    val selectedTab: NotificationTab = NotificationTab.SOCIAL,
    val socialNotifications: List<NotificationItem> = emptyList(),
    val emergencyNotifications: List<NotificationItem> = emptyList()
)

// --- VIEW MODEL ---

@HiltViewModel
class NotificationViewModel @Inject constructor(
    // Inject Repository nếu cần sau này
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadDummyData()
    }

    private fun loadDummyData() {
        // Giả lập dữ liệu ban đầu
        val social = listOf(
            NotificationItem("1", NotificationType.LIKE, "Nguyễn Văn A", message = "liked your post", timestamp = System.currentTimeMillis() - 3600000, isRead = false),
            NotificationItem("2", NotificationType.COMMENT, "Trần Thị B", message = "commented on your post: 'Great content!'", timestamp = System.currentTimeMillis() - 7200000, isRead = false),
            NotificationItem("3", NotificationType.FRIEND_REQUEST, "Lê Văn C", message = "sent you a friend request", timestamp = System.currentTimeMillis() - 86400000, isRead = true),
            NotificationItem("4", NotificationType.REPLY, "Phạm Thị D", message = "replied to your comment", timestamp = System.currentTimeMillis() - 172800000, isRead = true)
        )

        val emergency = listOf(
            NotificationItem("5", NotificationType.SOS_SUPPORT_ACCEPTED, "Rescue Team Alpha", message = "accepted to support your SOS post", timestamp = System.currentTimeMillis() - 1800000, isRead = false),
            NotificationItem("6", NotificationType.EMERGENCY_APPROVED, "Emergency Control Center", message = "Your emergency request has been approved", timestamp = System.currentTimeMillis() - 5400000, isRead = false),
            NotificationItem("7", NotificationType.EMERGENCY_REJECTED, "Emergency Control Center", message = "Your emergency request requires more information", timestamp = System.currentTimeMillis() - 259200000, isRead = true)
        )

        _uiState.update { it.copy(socialNotifications = social, emergencyNotifications = emergency) }
    }

    // --- USER ACTIONS ---

    fun onTabSelected(tab: NotificationTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun markAllAsRead() {
        _uiState.update { state ->
            if (state.selectedTab == NotificationTab.SOCIAL) {
                state.copy(socialNotifications = state.socialNotifications.map { it.copy(isRead = true) })
            } else {
                state.copy(emergencyNotifications = state.emergencyNotifications.map { it.copy(isRead = true) })
            }
        }
    }

    fun clearAll() {
        _uiState.update { state ->
            if (state.selectedTab == NotificationTab.SOCIAL) {
                state.copy(socialNotifications = emptyList())
            } else {
                state.copy(emergencyNotifications = emptyList())
            }
        }
    }

    fun markItemAsRead(item: NotificationItem) {
        _uiState.update { state ->
            // Cập nhật trong list tương ứng
            val updateList = { list: List<NotificationItem> ->
                list.map { if (it.id == item.id) it.copy(isRead = true) else it }
            }

            if (state.socialNotifications.any { it.id == item.id }) {
                state.copy(socialNotifications = updateList(state.socialNotifications))
            } else {
                state.copy(emergencyNotifications = updateList(state.emergencyNotifications))
            }
        }
    }

    fun acceptFriendRequest(item: NotificationItem) {
        // Xử lý logic accept, sau đó đánh dấu đã đọc hoặc xóa
        markItemAsRead(item)
    }

    fun rejectFriendRequest(item: NotificationItem) {
        // Xử lý logic reject
        markItemAsRead(item)
    }
}