package com.example.se114.ui.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.se114.data.model.ChatStatus
import com.example.se114.data.model.Conversation
import com.example.se114.data.model.FriendshipState
import com.example.se114.data.model.UserSummary
import com.example.se114.data.repository.PostRepository
import com.example.se114.local.PreferencesManager
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class ChatListUiState(
    val inboxConversations: List<Conversation> = emptyList(),
    val spamConversations: List<Conversation> = emptyList(),
    val friendConversations: List<Conversation> = emptyList(),

    val sentFriendRequests: List<Conversation> = emptyList(),
    val receivedFriendRequests: List<Conversation> = emptyList(),

    val userProfiles: Map<String, UserSummary> = emptyMap(),
    val totalUnreadCount: Int = 0,
    val searchQuery: String = "",

    val isShowingAddFriendDialog: Boolean = false,
    val searchPhoneQuery: String = "",
    val searchResults: List<UserSummary> = emptyList(),
    val isSearching: Boolean = false,

    val isShowingFriendsManagerDialog: Boolean = false,

    val isLoading: Boolean = false,
    val error: String? = null,

    val hasWarning: Boolean = false,
    val latestWarningMessage: String? = null
)

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val preferencesManager: PreferencesManager,
    private val repository: PostRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatListUiState())
    val uiState = _uiState.asStateFlow()

    private var allConversations: List<Conversation> = emptyList()
    private val currentUserId = preferencesManager.userId

    init {
        listenToConversations()
        listenToWarnings()
    }
    private fun listenToWarnings() {
        if (currentUserId.isBlank()) return

        // Lắng nghe sub-collection notifications của user
        firestore.collection("users").document(currentUserId)
            .collection("notifications")
            .whereEqualTo("type", "WARNING")
            .whereEqualTo("isRead", false)
            .limit(1)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null && !snapshot.isEmpty) {
                    val doc = snapshot.documents[0]
                    val content = doc.getString("content")
                    _uiState.update { it.copy(hasWarning = true, latestWarningMessage = content) }
                }
            }
    }
    fun markWarningAsRead() {
        // Logic đánh dấu đã đọc (đơn giản là ẩn đi trên UI hoặc update Firestore)
        _uiState.update { it.copy(hasWarning = false) }
        // Nếu muốn kỹ hơn: Update field isRead = true trên Firestore
    }
    private fun listenToConversations() {
        if (currentUserId.isBlank()) return

        _uiState.update { it.copy(isLoading = true) }

        firestore.collection("conversations")
            .whereArrayContains("participants", currentUserId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val conversations = snapshot.toObjects(Conversation::class.java)
                    allConversations = conversations.sortedByDescending { it.lastMessageTime }

                    fetchLatestUserProfiles(allConversations)
                    updateFilteredList()
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
    }

    private fun fetchLatestUserProfiles(conversations: List<Conversation>) {
        viewModelScope.launch {
            val partnerIds = conversations.flatMap { it.participants }
                .filter { it != currentUserId }
                .distinct()

            if (partnerIds.isEmpty()) return@launch

            val newUserMap = _uiState.value.userProfiles.toMutableMap()
            partnerIds.chunked(10).forEach { chunk ->
                try {
                    val usersSnapshot = firestore.collection("users")
                        .whereIn(FieldPath.documentId(), chunk).get().await()
                    for (doc in usersSnapshot) {
                        val name = doc.getString("name") ?: "Unknown"

                        val avatarUrl = doc.getString("avatar_url")
                        val avatar = if (!avatarUrl.isNullOrEmpty()) avatarUrl else name.take(1).uppercase()

                        val phone = doc.getString("phone") ?: ""
                        val email = doc.getString("email") ?: ""
                        newUserMap[doc.id] = UserSummary(doc.id, name, avatar, phone, email)
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
            _uiState.update { it.copy(userProfiles = newUserMap) }
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _uiState.update { it.copy(searchQuery = newQuery) }
        updateFilteredList()
    }

    private fun updateFilteredList() {
        val query = _uiState.value.searchQuery

        val friendsSource = allConversations.filter {
            it.friendshipState == FriendshipState.FRIENDS
        }

        val visibleConversations = allConversations.filter { !it.isDeletedBy(currentUserId) }

        val sentRequestsSource = allConversations.filter {
            it.friendshipState == FriendshipState.PENDING && it.friendRequestSenderId == currentUserId
        }

        val receivedRequestsSource = allConversations.filter {
            it.friendshipState == FriendshipState.PENDING && it.friendRequestSenderId != currentUserId
        }

        // --- SỬA LẠI LOGIC LỌC INBOX ---
        val inboxSource = visibleConversations.filter { conv ->
            val isChatAccepted = conv.status == ChatStatus.ACCEPTED
            val isMySentChatPending = conv.status == ChatStatus.PENDING && conv.requestSenderId == currentUserId

            // THÊM: Cho phép hiện cả những đoạn chat bị chặn (REJECTED)
            val isRejected = conv.status == ChatStatus.REJECTED

            // Điều kiện cuối cùng:
            (isChatAccepted || isMySentChatPending || isRejected) &&
                    conv.lastMessage.isNotBlank() &&
                    conv.lastMessage != "Đã gửi lời mời kết bạn"
        }

        val spamSource = visibleConversations.filter { conv ->
            conv.status == ChatStatus.PENDING && conv.requestSenderId != currentUserId && conv.lastMessage.isNotBlank() && conv.lastMessage != "Đã gửi lời mời kết bạn"
        }

        val filteredInbox = if (query.isBlank()) {
            inboxSource
        } else {
            inboxSource.filter { conv ->
                val partnerId = conv.participants.find { it != currentUserId }
                val partnerName = _uiState.value.userProfiles[partnerId]?.name ?: "Unknown"
                partnerName.contains(query, ignoreCase = true) || conv.lastMessage.contains(query, ignoreCase = true)
            }
        }

        val unreadCount = (inboxSource + spamSource).count { it.isUnread(currentUserId) }

        _uiState.update {
            it.copy(
                inboxConversations = filteredInbox,
                spamConversations = spamSource,
                friendConversations = friendsSource,
                sentFriendRequests = sentRequestsSource,
                receivedFriendRequests = receivedRequestsSource,
                totalUnreadCount = unreadCount
            )
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            firestore.collection("conversations").document(conversationId)
                .update(
                    mapOf(
                        "deletedBy" to FieldValue.arrayUnion(currentUserId),
                        "hiddenTimestamps.$currentUserId" to System.currentTimeMillis()
                    )
                )
        }
    }

    fun onSearchPhoneChange(phone: String) { _uiState.update { it.copy(searchPhoneQuery = phone) } }

    fun searchUsersByPhone() {
        val phone = _uiState.value.searchPhoneQuery.trim()
        if (phone.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSearching = true, searchResults = emptyList(), error = null) }
            try {
                // 1. Tìm user theo số điện thoại
                val querySnapshot = firestore.collection("users").whereEqualTo("phone", phone).get().await()

                // 2. Lấy Block list của mình
                val myDoc = firestore.collection("users").document(currentUserId).get().await()
                val myBlockedList = myDoc.get("blockedUsers") as? List<String> ?: emptyList()

                val results = querySnapshot.documents.mapNotNull { doc ->
                    val targetId = doc.id
                    // 3. Lấy Block list của người tìm thấy
                    val targetBlockedList = doc.get("blockedUsers") as? List<String> ?: emptyList()

                    // LOGIC CHẶN 2 CHIỀU: Nếu mình chặn họ HOẶC họ chặn mình -> Không tìm thấy
                    val isHidden = myBlockedList.contains(targetId) || targetBlockedList.contains(currentUserId)

                    if (doc.id == currentUserId || isHidden) {
                        null
                    } else {
                        UserSummary(
                            uid = targetId,
                            name = doc.getString("name") ?: "Unknown",
                            avatar = doc.getString("avatar_url") ?: "",
                            phone = doc.getString("phone") ?: "",
                            email = doc.getString("email") ?: ""
                        )
                    }
                }
                _uiState.update { it.copy(isSearching = false, searchResults = results) }
                if (results.isEmpty()) _uiState.update { it.copy(error = preferencesManager.getString("user_not_found")) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isSearching = false, error = e.message) }
            }
        }
    }
    // Hàm helper sắp xếp lại userId để conversationId giữa 2 người luôn là cố định
    private fun getConversationId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
    }
    fun sendFriendRequest(targetUser: UserSummary) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val existingConv = allConversations.find { it.participants.contains(targetUser.uid) }

                if (existingConv != null) {
                    val updates = hashMapOf<String, Any>(
                        "friendshipState" to FriendshipState.PENDING,
                        "friendRequestSenderId" to currentUserId,
                        "deletedBy" to FieldValue.arrayRemove(currentUserId),
                        "status" to if (existingConv.status == ChatStatus.ACCEPTED) ChatStatus.ACCEPTED else ChatStatus.PENDING,
                        "requestSenderId" to if (existingConv.status == ChatStatus.ACCEPTED) existingConv.requestSenderId else currentUserId
                    )

                    firestore.collection("conversations").document(existingConv.id).update(updates)
                    _uiState.update { it.copy(isLoading = false) }
                    return@launch
                }

                val newConvId = getConversationId(currentUserId, targetUser.uid)
                val myName = preferencesManager.userName
                val myAvatar = preferencesManager.userName.take(1).uppercase()
                val participantData = mapOf(currentUserId to UserSummary(currentUserId, myName, myAvatar), targetUser.uid to targetUser)

                val newConversation = Conversation(
                    id = newConvId,
                    lastMessage = "",
                    lastMessageTime = System.currentTimeMillis(),
                    status = ChatStatus.PENDING,
                    friendshipState = FriendshipState.PENDING,
                    friendRequestSenderId = currentUserId,
                    requestSenderId = currentUserId,
                    participants = listOf(currentUserId, targetUser.uid).sorted(),
                    participantData = participantData,
                    lastSenderId = currentUserId,
                    readBy = listOf(currentUserId),
                    deletedBy = emptyList()
                )
                firestore.collection("conversations").document(newConvId).set(newConversation).await()

                repository.sendNotification(
                    receiverId = targetUser.uid,
                    senderId = currentUserId,
                    postId = null,
                    type = "FRIEND_REQUEST",
                    message = "sent_friend_request"
                )

                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun acceptRequest(conversationId: String) {
        viewModelScope.launch {
            // 1. Logic Update Firestore cũ (Giữ nguyên)
            firestore.collection("conversations").document(conversationId)
                .update(
                    mapOf(
                        "friendshipState" to FriendshipState.FRIENDS,
                        "status" to ChatStatus.ACCEPTED,
                        "lastMessageTime" to System.currentTimeMillis()
                    )
                ).await()

            // 2. THÊM: Xóa thông báo kết bạn tương ứng
            // Cần tìm ra senderId (người kia) từ conversationId
            val conv = allConversations.find { it.id == conversationId }
            val partnerId = conv?.participants?.find { it != currentUserId }

            if (partnerId != null) {
                repository.removeNotification(
                    receiverId = currentUserId,
                    senderId = partnerId,
                    postId = null,
                    type = "FRIEND_REQUEST"
                )
            }
        }
    }

    fun declineRequest(conversationId: String) {
        viewModelScope.launch {
            // 1. Logic Update Firestore cũ
            firestore.collection("conversations").document(conversationId)
                .update(
                    mapOf(
                        "friendshipState" to FriendshipState.NONE,
                        "friendRequestSenderId" to ""
                    )
                ).await()

            // 2. THÊM: Xóa thông báo
            val conv = allConversations.find { it.id == conversationId }
            val partnerId = conv?.participants?.find { it != currentUserId }

            if (partnerId != null) {
                repository.removeNotification(
                    receiverId = currentUserId,
                    senderId = partnerId,
                    postId = null,
                    type = "FRIEND_REQUEST"
                )
            }
        }
    }

    fun unfriend(conversationId: String) {
        viewModelScope.launch {
            val conv = allConversations.find { it.id == conversationId }
            if (conv != null) {
                if (conv.friendshipState == FriendshipState.PENDING) {
                    firestore.collection("conversations").document(conversationId).delete().await()
                } else {
                    firestore.collection("conversations").document(conversationId)
                        .update(
                            mapOf(
                                "friendshipState" to FriendshipState.NONE,
                                "friendRequestSenderId" to ""
                            )
                        ).await()
                }
            }
            // Xóa thông báo
            val partnerId = conv?.participants?.find { it != currentUserId }
            if (partnerId != null) {
                repository.removeNotification(
                    receiverId = partnerId,
                    senderId = currentUserId,
                    postId = null,
                    type = "FRIEND_REQUEST"
                )
            }
        }
    }

    fun showAddFriendDialog() { _uiState.update { it.copy(isShowingAddFriendDialog = true, searchResults = emptyList(), searchPhoneQuery = "") } }
    fun hideAddFriendDialog() { _uiState.update { it.copy(isShowingAddFriendDialog = false, error = null) } }
    fun showFriendsManagerDialog() { _uiState.update { it.copy(isShowingFriendsManagerDialog = true) } }
    fun hideFriendsManagerDialog() { _uiState.update { it.copy(isShowingFriendsManagerDialog = false) } }
}