package com.example.se114.ui.presentation.other_profile

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.se114.data.Report
import com.example.se114.data.model.ChatStatus
import com.example.se114.data.model.Conversation
import com.example.se114.data.model.FriendshipState
import com.example.se114.data.model.Review
import com.example.se114.data.model.UserSummary
import com.example.se114.data.repository.PostRepository
import com.example.se114.local.PreferencesManager
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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
    val isBlocked: Boolean = false,
    val reportToastMessage: String? = null, // Dùng biến này để bắn Toast (Success hoặc Duplicate)
    val hasUserReviewed: Boolean = false,

    // --- Rating Fields ---
    val canRate: Boolean = false, // Đủ điều kiện nhắn tin hay chưa
    val myRating: Int = 0,        // Số sao mình đã đánh giá (0 nếu chưa)
    val myComment: String = "",   // Comment của mình
    val reviewsList: List<Review> = emptyList(),
    // Map lưu avatar mới nhất của người review: Map<UserId, AvatarUrl>
    val reviewAuthorAvatars: Map<String, String> = emptyMap(),
    val isReviewsLoading: Boolean = false,
    val totalReviews: Long = 0,   // Tổng số review của user này
    val averageRating: Float = 0f, // Điểm trung bình
    val lastReviewDoc: com.google.firebase.firestore.DocumentSnapshot? = null, // Để phân trang
    val isTargetBanned: Boolean = false


)

sealed class OtherProfileEvent {
    data class NavigateToChat(val conversationId: String) : OtherProfileEvent()
}

@HiltViewModel
class OtherProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val firestore: FirebaseFirestore,
    private val preferencesManager: PreferencesManager,
    private val repository: PostRepository
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

    private fun checkIfUserReviewed(targetUserId: String) {
        val myId = preferencesManager.userId
        firestore.collection("users").document(targetUserId)
            .collection("reviews").document(myId)
            .addSnapshotListener { snapshot, _ ->
                // Nếu document tồn tại nghĩa là người dùng đã review
                val exists = snapshot != null && snapshot.exists()
                _uiState.update { it.copy(hasUserReviewed = exists) }
            }
    }

    private fun loadUserProfile(targetUserId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, userId = targetUserId) }

            try {
                // 1. Lấy thông tin user đối phương
                val document = firestore.collection("users").document(targetUserId).get().await()

                // 2. Lấy thông tin bản thân (để check block VÀ đồng bộ Avatar mới nhất)
                val myDoc = firestore.collection("users").document(myId).get().await()

                // --- FIX ĐỒNG BỘ: Cập nhật lại Cache (Preferences) từ dữ liệu mới nhất trên Server ---
                if (myDoc.exists()) {
                    val myLatestAvatar = myDoc.getString("avatar_url") ?: ""
                    val myLatestName = myDoc.getString("name") ?: ""

                    // Lưu vào preferences để UI (ReviewListDialog) đọc được ảnh mới nhất
                    preferencesManager.userAvatar = myLatestAvatar
                    preferencesManager.userName = myLatestName
                }
                // -----------------------------------------------------------------------------------

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

                    // --- LẤY THÔNG TIN RATING ---
                    val ratingSum = document.getLong("ratingSum") ?: 0L
                    val ratingCount = document.getLong("ratingCount") ?: 0L
                    val avg = if (ratingCount > 0) ratingSum.toFloat() / ratingCount else 0f

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
                            isBlocked = false,
                            rating = avg,
                            reviewCount = ratingCount.toInt(),
                            averageRating = avg,
                            totalReviews = ratingCount
                        )
                    }

                    // Check rating status
                    checkMyReview(targetUserId)
                    checkCanRateCondition(targetUserId)
                    checkIfUserReviewed(targetUserId)

                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = preferencesManager.getString("user_not_found")) }
                }
            } catch (e: Exception) {
                // --- SỬA ĐOẠN NÀY ---
                e.printStackTrace()

                // Kiểm tra nếu lỗi là do không có quyền (người kia bị Ban)
                val isPermissionDenied = e.message?.contains("PERMISSION_DENIED") == true ||
                        e.message?.contains("Missing or insufficient permissions") == true

                if (isPermissionDenied) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isBlocked = true, // <--- Coi như bị Block để hiện UI icon khóa xám
                            errorMessage = preferencesManager.getString("user_unavailable") // "Người dùng không khả dụng"
                        )
                    }
                } else {
                    // Lỗi khác thì hiện như cũ
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                }

            }
        }
    }

    // --- LOGIC RATING ---
    private fun checkMyReview(targetId: String) {
        viewModelScope.launch {
            try {
                val reviewDoc = firestore.collection("users").document(targetId)
                    .collection("reviews").document(myId).get().await()

                if (reviewDoc.exists()) {
                    val review = reviewDoc.toObject(Review::class.java)
                    if (review != null) {
                        _uiState.update {
                            it.copy(myRating = review.rating, myComment = review.comment)
                        }
                    }
                }
            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    private fun checkCanRateCondition(targetId: String) {
        viewModelScope.launch {
            try {
                // Tìm conversation giữa 2 người
                val snapshot = firestore.collection("conversations")
                    .whereArrayContains("participants", myId)
                    .get().await()

                val conversation = snapshot.documents.find {
                    val participants = it.get("participants") as? List<String> ?: emptyList()
                    participants.contains(targetId)
                }

                if (conversation != null) {
                    // Query messages để đếm (Check 50 tin gần nhất)
                    val messagesSnap = firestore.collection("conversations").document(conversation.id)
                        .collection("messages")
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .limit(50)
                        .get().await()

                    var myCount = 0
                    var targetCount = 0

                    for (doc in messagesSnap) {
                        val senderId = doc.getString("senderId")
                        if (senderId == myId) myCount++
                        else if (senderId == targetId) targetCount++
                    }

                    // Điều kiện: Cả 2 đều phải có ít nhất 1 tin VÀ tổng > 10
                    val isQualified = (myCount + targetCount >= 10) && (myCount > 0 && targetCount > 0)
                    _uiState.update { it.copy(canRate = isQualified) }
                } else {
                    _uiState.update { it.copy(canRate = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(canRate = false) }
            }
        }
    }

    fun submitRating(rating: Int, comment: String) {
        val targetId = _uiState.value.userId
        if (targetId.isBlank() || rating == 0) return

        viewModelScope.launch {
            try {
                firestore.runTransaction { transaction ->
                    val userRef = firestore.collection("users").document(targetId)
                    val reviewRef = userRef.collection("reviews").document(myId)

                    val userDoc = transaction.get(userRef)
                    val reviewDoc = transaction.get(reviewRef)

                    var currentSum = userDoc.getLong("ratingSum") ?: 0L
                    var currentCount = userDoc.getLong("ratingCount") ?: 0L

                    if (reviewDoc.exists()) {
                        // Nếu đã đánh giá -> Cập nhật (Trừ điểm cũ, cộng điểm mới)
                        val oldRating = reviewDoc.getLong("rating") ?: 0L
                        currentSum = currentSum - oldRating + rating
                    } else {
                        // Đánh giá mới -> Cộng thêm
                        currentSum += rating
                        currentCount += 1
                    }

                    val newReview = Review(
                        reviewerId = myId,
                        reviewerName = preferencesManager.userName,
                        reviewerAvatar = preferencesManager.userAvatar,
                        rating = rating,
                        comment = comment,
                        timestamp = System.currentTimeMillis()
                    )

                    transaction.set(reviewRef, newReview)
                    transaction.update(userRef, "ratingSum", currentSum, "ratingCount", currentCount)
                }.await()

                // Update UI Local
                _uiState.update {
                    it.copy(myRating = rating, myComment = comment)
                }
                loadUserProfile(targetId) // Reload để cập nhật số sao trung bình hiển thị
                loadReviews(reset = true) // Reload list review

            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Error rating: ${e.message}") }
            }
        }
    }

    fun loadReviews(reset: Boolean = false) {
        val targetId = _uiState.value.userId
        if (targetId.isBlank()) return
        if (reset) {
            _uiState.update { it.copy(reviewsList = emptyList(), lastReviewDoc = null) }
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isReviewsLoading = true) }
            try {
                var query = firestore.collection("users").document(targetId)
                    .collection("reviews")
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(10)

                val lastDoc = _uiState.value.lastReviewDoc
                if (!reset && lastDoc != null) {
                    query = query.startAfter(lastDoc)
                }

                val snapshot = query.get().await()
                if (!snapshot.isEmpty) {
                    val newReviews = snapshot.toObjects(Review::class.java)
                    val newLastDoc = snapshot.documents.lastOrNull()

                    // --- LOGIC MỚI: FETCH AVATAR MỚI NHẤT CỦA CÁC REVIEWER ---
                    val userIdsToFetch = newReviews.map { it.reviewerId }.distinct()
                        .filter { !_uiState.value.reviewAuthorAvatars.containsKey(it) }

                    val newAvatarsMap = _uiState.value.reviewAuthorAvatars.toMutableMap()

                    if (userIdsToFetch.isNotEmpty()) {
                        userIdsToFetch.chunked(10).forEach { chunkIds ->
                            try {
                                val usersSnap = firestore.collection("users")
                                    .whereIn(com.google.firebase.firestore.FieldPath.documentId(), chunkIds)
                                    .get().await()
                                for (doc in usersSnap) {
                                    val avatar = doc.getString("avatar_url") ?: ""
                                    newAvatarsMap[doc.id] = avatar
                                }
                            } catch (e: Exception) { e.printStackTrace() }
                        }
                    }

                    _uiState.update {
                        it.copy(
                            reviewsList = if (reset) newReviews else it.reviewsList + newReviews,
                            lastReviewDoc = newLastDoc,
                            reviewAuthorAvatars = newAvatarsMap,
                            isReviewsLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isReviewsLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isReviewsLoading = false) }
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

                repository.sendNotification(
                    receiverId = targetId, // Lấy từ state, không cần truyền vào hàm
                    senderId = myId,
                    postId = null,
                    type = "FRIEND_REQUEST",
                    message = "sent_friend_request"
                )

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

    private fun getConversationId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
    }

    private suspend fun createConversation(targetId: String, initialMessage: String, isFriendRequest: Boolean) {
        val newConvId = getConversationId(myId, targetId)
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
                // Logic cũ
                firestore.collection("conversations").document(currentConversationId!!)
                    .update(
                        mapOf(
                            "friendshipState" to FriendshipState.NONE,
                            "friendRequestSenderId" to ""
                        )
                    ).await()
                _uiState.update { it.copy(friendshipStatus = FriendshipStatus.NONE) }

                // THÊM: Xóa thông báo bên phía người nhận
                val targetId = _uiState.value.userId
                if (targetId.isNotEmpty()) {
                    repository.removeNotification(
                        receiverId = targetId,  // Người nhận là họ
                        senderId = myId,        // Người gửi là mình
                        postId = null,
                        type = "FRIEND_REQUEST"
                    )
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "${preferencesManager.getString("error_cancel_request")}${e.message}") }
            }
        }
    }

    fun onAcceptFriendClick() {
        if (currentConversationId == null) return
        viewModelScope.launch {
            try {
                // Logic cũ
                firestore.collection("conversations").document(currentConversationId!!)
                    .update(
                        mapOf(
                            "friendshipState" to FriendshipState.FRIENDS,
                            "status" to ChatStatus.ACCEPTED
                        )
                    ).await()
                _uiState.update { it.copy(friendshipStatus = FriendshipStatus.FRIEND) }

                // THÊM: Xóa thông báo kết bạn
                val targetId = _uiState.value.userId
                if (targetId.isNotEmpty()) {
                    repository.removeNotification(
                        receiverId = myId,      // Mình là người nhận
                        senderId = targetId,    // Họ là người gửi
                        postId = null,
                        type = "FRIEND_REQUEST"
                    )
                }

            } catch (e: Exception) { e.printStackTrace() }
        }
    }

    // --- REPORT USER FUNCTION ---
    fun submitReport(reason: String, description: String) {
        val targetId = _uiState.value.userId
        if (targetId.isBlank() || myId.isBlank()) return

        viewModelScope.launch {
            try {
                // 1. CHỐNG SPAM: Kiểm tra trùng
                val existingReport = firestore.collection("reports")
                    .whereEqualTo("reporterId", myId)
                    .whereEqualTo("reportedUserId", targetId)
                    .whereEqualTo("status", "PENDING")
                    .get()
                    .await()

                if (!existingReport.isEmpty) {
                    // PHÁT HIỆN TRÙNG -> Gán vào reportToastMessage để hiện Toast (không gán vào errorMessage)
                    _uiState.update {
                        it.copy(reportToastMessage = preferencesManager.getString("report_duplicate_user"))
                    }
                    return@launch
                }

                // 2. Tạo report mới
                val newDocRef = firestore.collection("reports").document()
                val report = Report(
                    id = newDocRef.id,
                    reporterId = myId,
                    reportedUserId = targetId,
                    postId = null,
                    reason = reason,
                    description = description,
                    status = "PENDING"
                )

                newDocRef.set(report).await()

                // THÀNH CÔNG -> Cũng gán vào reportToastMessage
                _uiState.update {
                    it.copy(reportToastMessage = preferencesManager.getString("report_submitted_success"))
                }
            } catch (e: Exception) {
                // Lỗi hệ thống -> Gán vào errorMessage (để hiện log đỏ hoặc Toast tùy ý, ở đây giữ lỗi đỏ cho dev dễ thấy)
                _uiState.update {
                    it.copy(errorMessage = "${preferencesManager.getString("report_error")}${e.message}")
                }
            }
        }
    }

    fun clearReportToastMessage() {
        _uiState.update { it.copy(reportToastMessage = null) }
    }
}