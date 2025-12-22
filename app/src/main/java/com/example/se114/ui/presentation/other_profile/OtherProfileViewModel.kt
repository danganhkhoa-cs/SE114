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
    val errorMessage: String? = null,
    val isBlocked: Boolean = false
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
                // 1. Lấy thông tin user đối phương
                val document = firestore.collection("users").document(targetUserId).get().await()

                // 2. Lấy thông tin bản thân
                val myDoc = firestore.collection("users").document(myId).get().await()
                val myBlockedList = myDoc.get("blockedUsers") as? List<String> ?: emptyList()

                if (document.exists()) {
                    // 3. Logic chặn 2 chiều
                    val targetBlockedList = document.get("blockedUsers") as? List<String> ?: emptyList()
                    val iBlockedThem = myBlockedList.contains(targetUserId)
                    val theyBlockedMe = targetBlockedList.contains(myId)

                    if (iBlockedThem || theyBlockedMe) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                isBlocked = true,
                                errorMessage = preferencesManager.getString("user_unavailable")
                            )
                        }
                        return@launch
                    }

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
                            userBio = document.getString("bio") ?: preferencesManager.getString("no_bio"),
                            address = document.getString("address") ?: preferencesManager.getString("not_updated"),
                            gender = genderDisplay,
                            job = document.getString("job") ?: preferencesManager.getString("not_updated"),
                            phone = document.getString("phone") ?: preferencesManager.getString("hidden_info"),
                            joinedDate = preferencesManager.getString("joined_date"),
                            isLoading = false,
                            isBlocked = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = preferencesManager.getString("user_not_found")) }
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

            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun blockUser() {
        val targetId = _uiState.value.userId
        if (targetId.isBlank() || myId.isBlank()) return

        viewModelScope.launch {
            try {
                firestore.collection("users").document(myId)
                    .update("blockedUsers", FieldValue.arrayUnion(targetId))
                    .await()

                if (currentConversationId != null) {
                    firestore.collection("conversations").document(currentConversationId!!)
                        .update(
                            mapOf(
                                "status" to ChatStatus.REJECTED,
                                "friendshipState" to FriendshipState.NONE,
                                "friendRequestSenderId" to ""
                            )
                        ).await()
                }

                _uiState.update {
                    it.copy(
                        isBlocked = true,
                        friendshipStatus = FriendshipStatus.NONE,
                        errorMessage = preferencesManager.getString("blocked_success")
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "${preferencesManager.getString("error_blocking")}${e.message}") }
            }
        }
    }

    fun onAddFriendClick() {
        val targetId = _uiState.value.userId
        if (targetId.isBlank() || myId.isBlank()) return

        viewModelScope.launch {
            try {
                if (currentConversationId != null) {
                    firestore.collection("conversations").document(currentConversationId!!)
                        .update(
                            mapOf(
                                "friendshipState" to FriendshipState.PENDING,
                                "friendRequestSenderId" to myId,
                                "deletedBy" to FieldValue.arrayRemove(myId),
                            )
                        ).await()
                } else {
                    createConversation(targetId, preferencesManager.getString("msg_sent_friend_request"), isFriendRequest = true)
                }
                _uiState.update { it.copy(friendshipStatus = FriendshipStatus.SENT_REQUEST) }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "${preferencesManager.getString("error_friend_request")}${e.message}") }
            }
        }
    }

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
                    createConversation(targetId, preferencesManager.getString("msg_start_conversation"), isFriendRequest = false)
                    if (currentConversationId != null) {
                        _eventChannel.send(OtherProfileEvent.NavigateToChat(currentConversationId!!))
                    }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = preferencesManager.getString("error_create_chat")) }
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
            status = ChatStatus.PENDING,
            requestSenderId = myId,
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
                _uiState.update { it.copy(errorMessage = "${preferencesManager.getString("error_cancel_request")}${e.message}") }
            }
        }
    }

    fun onAcceptFriendClick() {
        if (currentConversationId == null) return
        viewModelScope.launch {
            try {
                firestore.collection("conversations").document(currentConversationId!!)
                    .update(
                        mapOf(
                            "friendshipState" to FriendshipState.FRIENDS,
                            "status" to ChatStatus.ACCEPTED
                        )
                    ).await()
                _uiState.update { it.copy(friendshipStatus = FriendshipStatus.FRIEND) }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }
}