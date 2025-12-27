package com.example.se114.ui.presentation.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.se114.data.repository.PostRepository
import com.example.se114.local.PreferencesManager
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// --- DATA CLASSES & ENUMS (Chuyển từ Screen sang đây) ---

enum class NotificationType {
    LIKE, LIKE_COMMENT, COMMENT, REPLY, FRIEND_REQUEST,
    SYSTEM
}

enum class NotificationTab {
    SOCIAL, SYSTEM
}

data class NotificationItem(
    val id: String = "", // Mặc định rỗng để Firestore tự điền nếu cần
    val type: NotificationType = NotificationType.SYSTEM, // Cần gán default để dùng toObject()
    val senderName: String = "",
    val senderAvatar: String? = null,
    val senderId: String? = null,
    val message: String = "",
    val timestamp: Timestamp = Timestamp.now(),

    @get:PropertyName("isRead")
    @set:PropertyName("isRead")
    var isRead: Boolean = false,

    val postId: String? = null,
    val requestId: String? = null,

    // --- Chỉ dùng trong App, không cần map từ DB ---
    @get:Exclude // Báo Firebase bỏ qua trường này khi map
    val isFromBroadcast: Boolean = false
)



// --- UI STATE ---

data class NotificationUiState(
    val selectedTab: NotificationTab = NotificationTab.SOCIAL,
    val socialNotifications: List<NotificationItem> = emptyList(),
    val systemNotifications: List<NotificationItem> = emptyList()
)

// --- VIEW MODEL ---

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val repository: PostRepository,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observeAllNotifications()
    }

    private fun observeAllNotifications() {
        val userId = preferencesManager.userId
        if (userId.isEmpty()) return

        viewModelScope.launch {
            // Kết hợp 3 nguồn dữ liệu:
            // 1. Tin cá nhân (Gồm cả Social + Private System)
            // 2. Tin Broadcast (Chỉ System)
            // 3. Danh sách ID Broadcast đã đọc
            combine(
                repository.getSocialNotificationsFlow(userId),
                repository.getSystemNotificationsFlow(),
                repository.getReadSystemIdsFlow(userId)
            ) { personalList, broadcastList, readBroadcastIds ->

                // A. Xử lý Broadcast List: Map trạng thái isRead dựa trên collection riêng
                val processedBroadcasts = broadcastList.map { item ->
                    item.copy(isRead = readBroadcastIds.contains(item.id))
                }

                // B. Phân loại Personal List
                val (privateSystemList, socialList) = personalList.partition {
                    it.type == NotificationType.SYSTEM
                }

                // C. Cập nhật UI State
                _uiState.update { state ->
                    state.copy(
                        // Tab Social: Chỉ chứa tin KHÔNG PHẢI System
                        socialNotifications = socialList,

                        // Tab System: Gồm Broadcasts + Private System (Warnings)
                        // Sắp xếp lại theo thời gian giảm dần
                        systemNotifications = (processedBroadcasts + privateSystemList)
                            .sortedByDescending { it.timestamp }
                    )
                }
            }.collect() // Collect để giữ flow chạy
        }
    }

    // --- USER ACTIONS ---

    fun onTabSelected(tab: NotificationTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun markItemAsRead(item: NotificationItem) {
        if (item.isRead) return

        // 1. Update UI
        _uiState.update { state ->
            val updateList = { list: List<NotificationItem> ->
                list.map { if (it.id == item.id) it.copy(isRead = true) else it }
            }
            if (state.systemNotifications.any { it.id == item.id }) {
                state.copy(systemNotifications = updateList(state.systemNotifications))
            } else {
                state.copy(socialNotifications = updateList(state.socialNotifications))
            }
        }

        // 2. Update DB
        val userId = preferencesManager.userId
        if (userId.isNotEmpty()) {
            viewModelScope.launch {
                if (item.isFromBroadcast) {
                    // Nếu là Broadcast -> Tạo doc trong read_system_notifications
                    repository.markSystemNotificationAsRead(userId, item.id)
                } else {
                    // Nếu là Private (Social hoặc Private System) -> Update field isRead
                    repository.markNotificationAsRead(userId, item.id)
                }
            }
        }
    }

    fun markAllAsRead() {
        val currentState = _uiState.value
        val userId = preferencesManager.userId
        if (userId.isEmpty()) return

        // 1. Optimistic Update (UI update ngay cho mượt)
        _uiState.update { state ->
            if (state.selectedTab == NotificationTab.SOCIAL) {
                state.copy(socialNotifications = state.socialNotifications.map { it.copy(isRead = true) })
            } else {
                state.copy(systemNotifications = state.systemNotifications.map { it.copy(isRead = true) })
            }
        }

        // 2. Database Update
        viewModelScope.launch {
            if (currentState.selectedTab == NotificationTab.SOCIAL) {
                // --- TAB SOCIAL ---
                // Chỉ lấy các ID đang hiển thị ở Social để update (Tránh update nhầm Warning bên kia)
                val unreadIds = currentState.socialNotifications
                    .filter { !it.isRead }
                    .map { it.id }

                if (unreadIds.isNotEmpty()) {
                    repository.markBatchNotificationsAsRead(userId, unreadIds)
                }

            } else {
                // --- TAB SYSTEM ---
                val unreadItems = currentState.systemNotifications.filter { !it.isRead }

                // Tách làm 2 nhóm: Broadcast và Private
                val (broadcasts, privates) = unreadItems.partition { it.isFromBroadcast }

                // Nhóm A: Broadcast -> Tạo doc đã đọc
                if (broadcasts.isNotEmpty()) {
                    repository.markAllSystemNotificationsAsRead(userId, broadcasts.map { it.id })
                }

                // Nhóm B: Private Warnings -> Update field isRead
                if (privates.isNotEmpty()) {
                    repository.markBatchNotificationsAsRead(userId, privates.map { it.id })
                }
            }
        }
    }

    fun acceptFriendRequest(item: NotificationItem) {
        val userId = preferencesManager.userId
        val targetId = item.senderId

        if (userId.isEmpty() || targetId.isNullOrEmpty()) return

        // 1. Optimistic Update (Xóa ngay trên UI cho mượt)
        _uiState.update { state ->
            state.copy(
                socialNotifications = state.socialNotifications.filter { it.id != item.id },
                systemNotifications = state.systemNotifications.filter { it.id != item.id }
            )
        }

        // 2. Gọi API (Background)
        viewModelScope.launch {
            // A. Thực hiện logic kết bạn trên Firestore
            repository.acceptFriendRequestAction(userId, targetId)

            // B. Xóa vĩnh viễn thông báo trong Database
            repository.deleteNotification(userId, item.id)
        }
    }

    fun rejectFriendRequest(item: NotificationItem) {
        val userId = preferencesManager.userId
        val targetId = item.senderId

        if (userId.isEmpty() || targetId.isNullOrEmpty()) return

        // 1. Optimistic Update
        _uiState.update { state ->
            state.copy(
                socialNotifications = state.socialNotifications.filter { it.id != item.id },
                systemNotifications = state.systemNotifications.filter { it.id != item.id }
            )
        }

        // 2. Gọi API
        viewModelScope.launch {
            // A. Thực hiện logic từ chối
            repository.declineFriendRequestAction(userId, targetId)

            // B. Xóa thông báo
            repository.deleteNotification(userId, item.id)
        }
    }
}