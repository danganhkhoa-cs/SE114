package com.example.se114.ui.presentation.other_profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.se114.data.model.ChatStatus
import com.example.se114.data.model.Conversation
import com.example.se114.data.model.FriendshipState
import com.example.se114.data.model.UserSummary
import com.example.se114.local.PreferencesManager
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

enum class FriendshipStatus {
    NONE,           // Chưa là gì của nhau
    FRIEND,         // Đã là bạn
    SENT_REQUEST,   // Mình đã gửi lời mời
    RECEIVED_REQUEST // Họ gửi lời mời cho mình
}

data class OtherProfileUiState(
    val userId: String = "",
    val userName: String = "",
    val userAvatar: String = "",
    val userBio: String = "",
    val address: String = "",
    val gender: String = "",
    val job: String = "",
    val phone: String = "",
    val joinedDate: String = "",
    val rating: Float = 5.0f,
    val reviewCount: Int = 0,

    val friendshipStatus: FriendshipStatus = FriendshipStatus.NONE,

    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

sealed class OtherProfileEvent {
    data class NavigateToChat(val conversationId: String) : OtherProfileEvent()
}

@HiltViewModel
class OtherProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val firestore: FirebaseFirestore,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(OtherProfileUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventChannel = Channel<OtherProfileEvent>()
    val eventFlow = _eventChannel.receiveAsFlow()

    private val myId = preferencesManager.userId
    private var currentConversationId: String? = null

    init {
        val userId = savedStateHandle.get<String>("userId")
        if (userId != null) {
            loadUserProfile(userId)
            checkFriendshipStatus(userId)
        }
    }

    private fun loadUserProfile(targetUserId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, userId = targetUserId) }

            try {
                val document = firestore.collection("users").document(targetUserId).get().await()

                if (document.exists()) {
                    val name = document.getString("name") ?: "Unknown"
                    val avatarUrl = document.getString("avatar_url")
                    val avatarDisplay = if (!avatarUrl.isNullOrEmpty()) avatarUrl else name.take(1).uppercase()

                    val genderVal = document.get("gender")
                    val genderDisplay = when (genderVal) {
                        is String -> genderVal
                        else -> genderVal?.toString() ?: "not_update"
                    }

                    _uiState.update {
                        it.copy(
                            userName = name,
                            userAvatar = avatarDisplay,
                            userBio = document.getString("bio") ?: "Chưa có giới thiệu",
                            address = document.getString("address") ?: "Chưa cập nhật",
                            gender = genderDisplay,
                            job = document.getString("job") ?: "Chưa cập nhật",
                            phone = document.getString("phone") ?: "Ẩn",
                            joinedDate = "Thành viên LocaSOS",
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Người dùng không tồn tại") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    private fun checkFriendshipStatus(targetUserId: String) {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("conversations")
                    .whereArrayContains("participants", myId)
                    .get()
                    .await()

                val conversation = snapshot.documents
                    .mapNotNull { it.toObject(Conversation::class.java) }
                    .find { it.participants.contains(targetUserId) }

                if (conversation != null) {
                    currentConversationId = conversation.id

                    // Logic mới dựa trên friendshipState và friendRequestSenderId
                    val status = when {
                        conversation.friendshipState == FriendshipState.FRIENDS -> FriendshipStatus.FRIEND
                        conversation.friendshipState == FriendshipState.PENDING && conversation.friendRequestSenderId == myId -> FriendshipStatus.SENT_REQUEST
                        conversation.friendshipState == FriendshipState.PENDING && conversation.friendRequestSenderId == targetUserId -> FriendshipStatus.RECEIVED_REQUEST
                        else -> FriendshipStatus.NONE
                    }
                    _uiState.update { it.copy(friendshipStatus = status) }
                } else {
                    currentConversationId = null
                    _uiState.update { it.copy(friendshipStatus = FriendshipStatus.NONE) }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Gửi lời mời kết bạn (Nút Kết bạn)
    fun onAddFriendClick() {
        val targetId = _uiState.value.userId
        if (targetId.isBlank() || myId.isBlank()) return

        viewModelScope.launch {
            try {
                if (currentConversationId != null) {
                    // Đã có conversation -> Update trạng thái friend request
                    firestore.collection("conversations").document(currentConversationId!!)
                        .update(
                            mapOf(
                                "friendshipState" to FriendshipState.PENDING,
                                "friendRequestSenderId" to myId,
                                // Nếu đang bị ẩn thì hiện lại
                                "deletedBy" to FieldValue.arrayRemove(myId),
                                // Có thể gửi kèm tin nhắn hệ thống nếu muốn
                                // "lastMessage" to "Đã gửi lời mời kết bạn"
                            )
                        ).await()
                } else {
                    // Chưa có conversation -> Tạo mới
                    createConversation(targetId, "Đã gửi lời mời kết bạn", isFriendRequest = true)
                }

                _uiState.update { it.copy(friendshipStatus = FriendshipStatus.SENT_REQUEST) }

            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Lỗi kết bạn: ${e.message}") }
            }
        }
    }

    // Nút Nhắn tin -> Chỉ tạo hội thoại và nhảy vào, KHÔNG ẢNH HƯỞNG TRẠNG THÁI BẠN BÈ
    fun onMessageClick() {
        val targetId = _uiState.value.userId
        if (targetId.isBlank()) return

        viewModelScope.launch {
            try {
                if (currentConversationId != null) {
                    try {
                        firestore.collection("conversations").document(currentConversationId!!)
                            .update("deletedBy", FieldValue.arrayRemove(myId))
                    } catch (e: Exception) {}

                    _eventChannel.send(OtherProfileEvent.NavigateToChat(currentConversationId!!))
                } else {
                    createConversation(targetId, "Bắt đầu cuộc trò chuyện", isFriendRequest = false)

                    if (currentConversationId != null) {
                        _eventChannel.send(OtherProfileEvent.NavigateToChat(currentConversationId!!))
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Không thể tạo cuộc trò chuyện") }
            }
        }
    }

    private suspend fun createConversation(targetId: String, initialMessage: String, isFriendRequest: Boolean) {
        val newConvId = "${myId}_${targetId}_${System.currentTimeMillis()}"
        val myName = preferencesManager.userName
        val myAvatar = preferencesManager.userName.take(1).uppercase()

        val targetName = _uiState.value.userName
        val targetAvatar = _uiState.value.userAvatar

        val participantData = mapOf(
            myId to UserSummary(myId, myName, myAvatar),
            targetId to UserSummary(targetId, targetName, targetAvatar)
        )

        val newConversation = Conversation(
            id = newConvId,
            lastMessage = initialMessage,
            lastMessageTime = System.currentTimeMillis(),
            // ChatStatus luôn là PENDING nếu chưa từng chat (Spam), trừ khi là bạn bè
            status = ChatStatus.PENDING,
            requestSenderId = myId,

            // Logic Friendship Tách Biệt
            friendshipState = if (isFriendRequest) FriendshipState.PENDING else FriendshipState.NONE,
            friendRequestSenderId = if (isFriendRequest) myId else "",

            participants = listOf(myId, targetId),
            participantData = participantData,
            lastSenderId = myId,
            readBy = listOf(myId),
            deletedBy = emptyList()
        )

        firestore.collection("conversations").document(newConvId).set(newConversation).await()
        currentConversationId = newConvId
    }

    // Hủy lời mời kết bạn (Chỉ reset trạng thái Friendship, không xóa đoạn chat)
    fun onCancelFriendRequest() {
        if (currentConversationId == null) return
        viewModelScope.launch {
            try {
                firestore.collection("conversations").document(currentConversationId!!)
                    .update(
                        mapOf(
                            "friendshipState" to FriendshipState.NONE,
                            "friendRequestSenderId" to ""
                        )
                    ).await()

                _uiState.update { it.copy(friendshipStatus = FriendshipStatus.NONE) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Lỗi hủy lời mời: ${e.message}") }
            }
        }
    }

    // Chấp nhận kết bạn -> Update FriendshipState thành FRIENDS
    fun onAcceptFriendClick() {
        if (currentConversationId == null) return
        viewModelScope.launch {
            try {
                firestore.collection("conversations").document(currentConversationId!!)
                    .update(
                        mapOf(
                            "friendshipState" to FriendshipState.FRIENDS,
                            "status" to ChatStatus.ACCEPTED // Nếu là bạn thì chắc chắn được chat
                        )
                    ).await()
                _uiState.update { it.copy(friendshipStatus = FriendshipStatus.FRIEND) }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
}