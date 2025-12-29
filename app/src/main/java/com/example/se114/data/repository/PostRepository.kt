package com.example.se114.data.repository

import com.example.se114.data.Comment
import com.example.se114.data.Post
import com.example.se114.data.Report
import com.example.se114.data.model.ChatStatus
import com.example.se114.data.model.FriendshipState
import com.example.se114.ui.presentation.notification.NotificationItem
import com.example.se114.ui.presentation.notification.NotificationType
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.snapshots
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

@Singleton
class PostRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val postsCollection = firestore.collection("posts")
    private val usersCollection = firestore.collection("users")
    //Nnguồn dữ liệu chung cho cả Home và Saved
    private val _savedPostIdsFlow = MutableStateFlow<Set<String>>(emptySet())
    val savedPostIdsFlow = _savedPostIdsFlow.asStateFlow()

    // 1. Lấy danh sách bài viết (Load Feed)
    suspend fun getPosts(currentUserId: String? = null): Result<List<Post>> {
        return try {
            // Bước A: Lấy danh sách bài viết thô từ collection "posts"
            val snapshot = postsCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            var posts = snapshot.toObjects(Post::class.java).mapIndexed { index, post ->
                post.copy(id = snapshot.documents[index].id)
            }

            // Bước B: Đồng bộ Avatar mới nhất từ collection "users"
            // (Vì avatar trong bài viết có thể là ảnh cũ lúc mới đăng)
            val userIds = posts.map { it.userId }.distinct().filter { it.isNotEmpty() }

            if (userIds.isNotEmpty()) {
                val userInfoMap = mutableMapOf<String, Pair<String, String>>()

                // Firestore giới hạn 'whereIn' tối đa 10 phần tử -> chia chunk
                val chunks = userIds.chunked(10)

                // Chạy song song các chunk để tối ưu tốc độ load
                withContext(Dispatchers.IO) {
                    chunks.map { chunk ->
                        async {
                            try {
                                val usersSnap = usersCollection
                                    .whereIn(FieldPath.documentId(), chunk)
                                    .get()
                                    .await()

                                usersSnap.documents.forEach { doc ->
                                    val url = doc.getString("avatar_url") ?: ""
                                    val name = doc.getString("name") ?: ""
                                    userInfoMap[doc.id] = Pair(url, name)
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }.awaitAll()
                }

                // Cập nhật avatar mới vào danh sách bài viết
                posts = posts.map { post ->
                    val userInfo = userInfoMap[post.userId]
                    if (userInfo != null) {
                        post.copy(
                            userAvatar = userInfo.first.ifEmpty { post.userAvatar },
                            userName = userInfo.second.ifEmpty { post.userName }
                        )
                    } else {
                        post
                    }
                }
            }

            // Bước C: Kiểm tra trạng thái Like (như cũ)
            if (!currentUserId.isNullOrEmpty()) {
                val likedIdsResult = getUserLikedPostIds(currentUserId)
                if (likedIdsResult.isSuccess) {
                    val likedIds = likedIdsResult.getOrThrow().toSet()
                    posts = posts.map { post ->
                        post.copy(isLiked = post.id in likedIds)
                    }
                }
            }

            Result.success(posts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- Hàm tối ưu: Chỉ lấy các bài viết theo danh sách ID ---
    suspend fun getPostsByIds(postIds: List<String>, currentUserId: String? = null): Result<List<Post>> {
        if (postIds.isEmpty()) return Result.success(emptyList())

        return try {
            var allPosts = mutableListOf<Post>()

            // 1. Lấy dữ liệu bài viết (Chia chunk vì giới hạn của Firestore whereIn)
            val chunks = postIds.chunked(10)

            withContext(Dispatchers.IO) {
                // A. Lấy Post Documents song song
                val postsTasks = chunks.map { chunk ->
                    async {
                        val snapshot = postsCollection
                            .whereIn(FieldPath.documentId(), chunk)
                            .whereEqualTo("status", "PUBLISHED")
                            .get()
                            .await()
                        snapshot.toObjects(Post::class.java).mapIndexed { index, post ->
                            post.copy(id = snapshot.documents[index].id)
                        }
                    }
                }
                val resultLists = postsTasks.awaitAll()
                resultLists.forEach { allPosts.addAll(it) }
            }

            // Sắp xếp lại theo thứ tự lưu (vì whereIn trả về ngẫu nhiên)
            // Lấy đảo ngược của postIds vì SavedScreen thường hiển thị bài mới lưu lên đầu
            var sortedPosts = postIds.mapNotNull { id ->
                allPosts.find { it.id == id }
            }

            // 2. Đồng bộ Avatar mới nhất (Giống HomeScreen)
            val userIds = sortedPosts.map { it.userId }.distinct().filter { it.isNotEmpty() }
            if (userIds.isNotEmpty()) {
                val avatarMap = mutableMapOf<String, String>()
                val userChunks = userIds.chunked(10)

                withContext(Dispatchers.IO) {
                    userChunks.map { chunk ->
                        async {
                            try {
                                val usersSnap = usersCollection
                                    .whereIn(FieldPath.documentId(), chunk)
                                    .get().await()
                                usersSnap.documents.forEach { doc ->
                                    avatarMap[doc.id] = doc.getString("avatar_url") ?: ""
                                }
                            } catch (e: Exception) { e.printStackTrace() }
                        }
                    }.awaitAll()
                }

                sortedPosts = sortedPosts.map { post ->
                    val freshAvatar = avatarMap[post.userId]
                    if (!freshAvatar.isNullOrEmpty()) post.copy(userAvatar = freshAvatar) else post
                }
            }

            // 3. Kiểm tra trạng thái Like (Quan trọng nhất cho yêu cầu của bạn)
            if (!currentUserId.isNullOrEmpty()) {
                val likedIdsResult = getUserLikedPostIds(currentUserId)
                if (likedIdsResult.isSuccess) {
                    val likedIds = likedIdsResult.getOrThrow().toSet()
                    sortedPosts = sortedPosts.map { post ->
                        post.copy(isLiked = post.id in likedIds)
                    }
                }
            }

            Result.success(sortedPosts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 2. Tạo bài viết mới
    suspend fun createPost(post: Post): Result<Unit> {
        return try {
            val docRef = postsCollection.document() // Tự sinh ID
            val newPost = post.copy(id = docRef.id)
            docRef.set(newPost).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 3. Xử lý Like (Cập nhật Transaction + Gửi thông báo)
    suspend fun toggleLikePost(postId: String, userId: String, currentLikeStatus: Boolean): Result<Unit> {
        return try {
            // Biến để lưu ID chủ bài viết (lấy ra từ transaction)
            var postOwnerId: String? = null

            firestore.runTransaction { transaction ->
                val postRef = postsCollection.document(postId)
                val likeRef = postRef.collection("likes").document(userId)
                val userLikeRef = usersCollection.document(userId).collection("liked_posts").document(postId)

                val postSnapshot = transaction.get(postRef)

                // Lấy ID chủ bài viết để tí nữa gửi thông báo
                postOwnerId = postSnapshot.getString("userId")

                val currentCount = postSnapshot.getLong("likeCount") ?: 0

                val newCount = if (currentLikeStatus) {
                    if (currentCount > 0) currentCount - 1 else 0
                } else {
                    currentCount + 1
                }

                transaction.update(postRef, "likeCount", newCount)

                if (currentLikeStatus) {
                    // User muốn UNLIKE -> Xóa
                    transaction.delete(likeRef)
                    transaction.delete(userLikeRef)
                } else {
                    // User muốn LIKE -> Thêm
                    val data = mapOf("timestamp" to FieldValue.serverTimestamp())
                    val userLikeData = mapOf("likedAt" to FieldValue.serverTimestamp())

                    transaction.set(likeRef, data.plus("userId" to userId))
                    transaction.set(userLikeRef, userLikeData)
                }
            }.await()

            // --- XỬ LÝ THÔNG BÁO (Chạy sau khi Transaction thành công) ---
            if (postOwnerId != null) {
                if (currentLikeStatus) {
                    // UNLIKE POST -> Gọi hàm xóa (commentId = null)
                    removeNotification(
                        receiverId = postOwnerId!!,
                        senderId = userId,
                        postId = postId,
                        type = "LIKE",
                        commentId = null // Quan trọng: null để chỉ xóa like bài viết
                    )
                } else {
                    // LIKE POST
                    sendNotification(
                        receiverId = postOwnerId!!,
                        senderId = userId,
                        postId = postId,
                        type = "LIKE",
                        message = "liked_your_post",
                        commentId = null
                    )
                }
            }
            // -----------------------------------------------------------

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserLikedPostIds(userId: String): Result<List<String>> {
        return try {
            val snapshot = usersCollection.document(userId)
                .collection("liked_posts")
                .get()
                .await()
            val ids = snapshot.documents.map { it.id }
            Result.success(ids)
        } catch (e: Exception) {
            // Nếu lỗi (hoặc user chưa login), trả về list rỗng để app không crash
            Result.success(emptyList())
        }
    }

    // 4. Xử lý Save (Thêm/Xóa vào users/{userId}/saved_posts)
    suspend fun toggleSavePost(postId: String, userId: String, currentSaveStatus: Boolean): Result<Unit> {
        return try {
            val savedPostRef = usersCollection.document(userId).collection("saved_posts").document(postId)

            if (currentSaveStatus) {
                savedPostRef.delete().await()
                // Cập nhật Flow: Xóa ID khỏi danh sách
                _savedPostIdsFlow.update { it - postId }
            } else {
                val data = mapOf("savedAt" to FieldValue.serverTimestamp())
                savedPostRef.set(data).await()
                // Cập nhật Flow: Thêm ID vào danh sách
                _savedPostIdsFlow.update { it + postId }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Cập nhật hàm getUserSavedPostIds: Khi lấy về -> Gán vào Flow luôn
    suspend fun getUserSavedPostIds(userId: String): Result<List<String>> {
        return try {
            val snapshot = usersCollection.document(userId)
                .collection("saved_posts")
                .orderBy("savedAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val ids = snapshot.documents.map { it.id }

            // Cập nhật Flow ngay khi lấy từ server về
            _savedPostIdsFlow.value = ids.toSet()

            Result.success(ids)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- REPORT LOGIC ---
    suspend fun createReport(report: Report): Result<String> { // Trả về String message lỗi hoặc thành công
        return try {
            // 1. CHỐNG SPAM: Kiểm tra xem user này đã report bài này chưa
            val existingReport = firestore.collection("reports")
                .whereEqualTo("reporterId", report.reporterId)
                .whereEqualTo("postId", report.postId)
                .whereEqualTo("status", "PENDING") // Chỉ chặn nếu đơn cũ chưa xử lý
                .get()
                .await()

            if (!existingReport.isEmpty) {
                return Result.failure(Exception("duplicate")) // Trả về lỗi định danh là duplicate
            }

            // 2. Tạo report mới
            val docRef = firestore.collection("reports").document()
            val finalReport = report.copy(id = docRef.id)
            docRef.set(finalReport).await()

            Result.success("Success")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Hàm lấy token từ thiết bị và lưu lên Firestore
    suspend fun updateFcmToken(userId: String) {
        try {
            // 1. Lấy token hiện tại của thiết bị
            val token = FirebaseMessaging.getInstance().token.await()

            // 2. Update vào document user (dùng field fcm_token vừa tạo ở Bước 1)
            usersCollection.document(userId)
                .update("fcm_token", token)
                .await()

        } catch (e: Exception) {
            e.printStackTrace() // Log lỗi nếu cần
        }
    }

    // --- FRIEND REQUEST HANDLING ---

    // Hàm helper tạo ID hội thoại (để tìm đúng document cần update)
    private fun getConversationId(uid1: String, uid2: String): String {
        return if (uid1 < uid2) "${uid1}_${uid2}" else "${uid2}_${uid1}"
    }

    /**
     * Chấp nhận lời mời kết bạn
     */
    suspend fun acceptFriendRequestAction(currentUserId: String, targetUserId: String) {
        val conversationId = getConversationId(currentUserId, targetUserId)
        try {
            firestore.collection("conversations").document(conversationId)
                .update(
                    mapOf(
                        "friendshipState" to FriendshipState.FRIENDS,
                        "status" to ChatStatus.ACCEPTED,
                        "lastMessageTime" to System.currentTimeMillis()
                    )
                ).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Từ chối lời mời kết bạn
     */
    suspend fun declineFriendRequestAction(currentUserId: String, targetUserId: String) {
        val conversationId = getConversationId(currentUserId, targetUserId)
        try {
            firestore.collection("conversations").document(conversationId)
                .update(
                    mapOf(
                        "friendshipState" to FriendshipState.NONE,
                        "friendRequestSenderId" to ""
                    )
                ).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- COMMENT LOGIC ---

    // 1. Lấy danh sách Comment (Realtime + Phân cấp 1 tầng)
    fun getCommentsFlow(postId: String, currentUserId: String): Flow<List<Comment>> {
        return postsCollection.document(postId)
            .collection("comments")
            .orderBy("timestamp", Query.Direction.ASCENDING) // Lấy cũ nhất trước (để xếp thứ tự chat)
            .snapshots()
            .map { snapshot ->
                // A. Parse dữ liệu thô
                val allComments = snapshot.documents.map { doc ->
                    doc.toObject(Comment::class.java)!!.copy(id = doc.id)
                }

                allComments
            }.map { rawComments ->
                // C. Kiểm tra Like (Map với list liked_comments của user nếu cần, hoặc query con)
                // Để hiệu năng tốt nhất, ta chỉ map Parent - Child.
                // Trạng thái Like của comment: Cần query `users/{id}/liked_comments` hoặc `comments/{id}/likes/{uid}`.
                // Giải pháp: Tạm thời để isLiked = false, ViewModel sẽ update sau hoặc load list like riêng.

                // D. Phân cấp Parent - Child
                val rootComments = rawComments.filter { it.parentId == null }.toMutableList()
                val replyComments = rawComments.filter { it.parentId != null }

                rootComments.map { root ->
                    val myReplies = replyComments.filter { it.parentId == root.id }
                    root.copy(replies = myReplies)
                }
            }
    }

    // Hàm hỗ trợ lấy trạng thái like cho danh sách comment (Gọi 1 lần khi load)
    suspend fun getLikedCommentIds(userId: String, postId: String): List<String> {
        return try {
            // Cách tối ưu: User lưu danh sách comment đã like trong subcollection của mình
            // users/{userId}/liked_comments (docs id là commentId)
            usersCollection.document(userId)
                .collection("liked_comments")
                .whereEqualTo("postId", postId)
                .get()
                .await()
                .documents.map { it.id }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // 2. Gửi Comment
    // Trong PostRepository.kt

    suspend fun addComment(
        postId: String,
        content: String,
        parentId: String?,
        userId: String,
        userName: String,
        userAvatar: String
    ): Result<Unit> {
        return try {
            val docRef = postsCollection.document(postId).collection("comments").document()

            val comment = Comment(
                id = docRef.id,
                postId = postId,
                userId = userId,
                userName = userName,
                userAvatar = userAvatar,
                content = content,
                parentId = parentId,
                timestamp = Timestamp.now()
            )

            // 1. Transaction: Lưu comment và tăng biến đếm
            firestore.runTransaction { transaction ->
                val postRef = postsCollection.document(postId)
                val snapshot = transaction.get(postRef)
                val newCommentCount = (snapshot.getLong("commentCount") ?: 0) + 1

                transaction.set(docRef, comment)
                transaction.update(postRef, "commentCount", newCommentCount)
            }.await()

            // --- NOTIFICATION LOGIC (LOGIC MỚI) ---

            // A. Lấy thông tin cần thiết
            val postSnapshot = postsCollection.document(postId).get().await()
            val postOwnerId = postSnapshot.getString("userId") ?: ""

            var parentAuthorId: String? = null
            if (parentId != null) {
                // Nếu là Reply, lấy thông tin người viết comment cha
                val parentCommentSnap = postsCollection.document(postId)
                    .collection("comments").document(parentId).get().await()
                parentAuthorId = parentCommentSnap.getString("userId")
            }

            // B. Gửi thông báo cho CHỦ BÀI VIẾT (postOwnerId)
            if (postOwnerId != userId) { // Không gửi nếu tự comment bài mình
                // Case 1: Người khác TRẢ LỜI comment của CHỦ BÀI VIẾT
                // -> Gửi REPLY ("Đã trả lời bình luận của bạn")
                if (parentId != null && parentAuthorId == postOwnerId) {
                    sendNotification(
                        receiverId = postOwnerId,
                        senderId = userId,
                        postId = postId,
                        type = "REPLY",
                        message = "replied_your_comment",
                        commentId = parentId // Lưu ID để định danh
                    )
                }
                // Case 2: Comment mới HOẶC Trả lời comment của người thứ 3
                // -> Gửi COMMENT ("Đã bình luận vào bài viết")
                else {
                    sendNotification(
                        receiverId = postOwnerId,
                        senderId = userId,
                        postId = postId,
                        type = "COMMENT",
                        message = "commented_your_post"
                    )
                }
            }

            // C. Gửi thông báo cho NGƯỜI ĐƯỢC REP (parentAuthorId)
            // Chỉ gửi nếu người này tồn tại VÀ không phải là các đối tượng đã xử lý ở trên
            if (parentId != null && parentAuthorId != null) {
                val isNotSelf = parentAuthorId != userId
                val isNotPostOwner = parentAuthorId != postOwnerId // Vì chủ bài viết đã được xử lý ở bước B rồi

                if (isNotSelf && isNotPostOwner) {
                    sendNotification(
                        receiverId = parentAuthorId,
                        senderId = userId,
                        postId = postId,
                        type = "REPLY",
                        message = content,
                        commentId = parentId
                    )
                }
            }
            // --------------------------

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // 3. Like Comment
    suspend fun toggleLikeComment(postId: String, commentId: String, userId: String, isLiked: Boolean): Result<Unit> {
        return try {
            val commentRef = postsCollection.document(postId).collection("comments").document(commentId)
            val userLikeRef = usersCollection.document(userId).collection("liked_comments").document(commentId)

            // Biến để hứng ID tác giả comment từ trong Transaction
            var commentOwnerId: String? = null

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(commentRef)

                // Lấy ID tác giả comment
                commentOwnerId = snapshot.getString("userId")

                val currentCount = snapshot.getLong("likeCount") ?: 0

                if (isLiked) {
                    // Unlike
                    transaction.update(commentRef, "likeCount", (currentCount - 1).coerceAtLeast(0))
                    transaction.delete(userLikeRef)
                } else {
                    // Like
                    transaction.update(commentRef, "likeCount", currentCount + 1)
                    transaction.set(userLikeRef, mapOf("postId" to postId, "timestamp" to FieldValue.serverTimestamp()))
                }
            }.await()

            // --- NOTIFICATION LOGIC [MỚI] ---
            if (commentOwnerId != null && commentOwnerId != userId) {
                if (isLiked) {
                    // UNLIKE COMMENT -> Xóa với ID cụ thể
                    removeNotification(
                        receiverId = commentOwnerId!!,
                        senderId = userId,
                        postId = postId,
                        type = "LIKE_COMMENT",
                        commentId = commentId
                    )
                } else {
                    // LIKE COMMENT
                    sendNotification(
                        receiverId = commentOwnerId!!,
                        senderId = userId,
                        postId = postId,
                        type = "LIKE_COMMENT",
                        message = "liked_your_comment",
                        commentId = commentId
                    )
                }
            }
            // --------------------------------

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * XÓA THÔNG BÁO (Delete)
     * Dùng để xóa tin sau khi đã xử lý xong (Accept/Decline)
     */
    suspend fun deleteNotification(userId: String, notificationId: String) {
        try {
            usersCollection.document(userId)
                .collection("notifications")
                .document(notificationId)
                .delete()
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- NOTIFICATION LOGIC (GENERIC) ---

    /**
     * Hàm dùng chung để gửi thông báo.
     * Sau này có Comment hay Friend Request thì chỉ cần gọi hàm này và đổi tham số 'type'.
     */
    suspend fun sendNotification(
        receiverId: String,
        senderId: String,
        postId: String?,
        type: String,
        message: String = "",
        commentId: String? = null
    ) {
        if (receiverId == senderId) return

        try {
            val senderDoc = usersCollection.document(senderId).get().await()
            val senderName = senderDoc.getString("name") ?: "Someone"
            val senderAvatar = senderDoc.getString("avatar_url") ?: ""

            val notificationData = hashMapOf(
                "type" to type,
                "senderId" to senderId,
                "senderName" to senderName,
                "senderAvatar" to senderAvatar,
                "postId" to postId,
                "message" to message,
                "isRead" to false,
                "timestamp" to FieldValue.serverTimestamp()
            )

            // Lưu commentId nếu có
            if (commentId != null) {
                notificationData["commentId"] = commentId
            }

            usersCollection.document(receiverId)
                .collection("notifications")
                .add(notificationData)
                .await()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Hàm dùng chung để xóa thông báo (VD: Unlike, Hủy kết bạn)
     */
    suspend fun removeNotification(
        receiverId: String,
        senderId: String,
        postId: String?,
        type: String,
        commentId: String? = null // <--- THÊM THAM SỐ NÀY
    ) {
        try {
            // 1. Tạo query cơ bản
            var query = usersCollection.document(receiverId)
                .collection("notifications")
                .whereEqualTo("senderId", senderId)
                .whereEqualTo("type", type)

            if (postId != null) {
                query = query.whereEqualTo("postId", postId)
            }

            // 2. Nếu có commentId, query chính xác luôn
            if (commentId != null) {
                query = query.whereEqualTo("commentId", commentId)
            }

            // 3. Thực hiện lấy dữ liệu và xóa
            val snapshot = query.get().await()

            for (doc in snapshot.documents) {
                // LOGIC LỌC KỸ HƠN (Client-side filtering):
                // Nếu ta đang muốn xóa Like Bài Viết (commentId == null),
                // ta phải chắc chắn document này KHÔNG chứa commentId.
                // (Vì Firestore query cơ bản không hỗ trợ "whereFieldDoesNotExist")
                val docCommentId = doc.getString("commentId")

                if (commentId == null) {
                    // Trường hợp xóa Like Post: Chỉ xóa nếu doc không có commentId
                    if (docCommentId == null) {
                        doc.reference.delete()
                    }
                } else {
                    // Trường hợp xóa Like Comment: Đã filter ở query rồi, cứ thế xóa
                    doc.reference.delete()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- REALTIME NOTIFICATION LISTENER ---

    // Flow chứa số lượng tin chưa đọc để ViewModel lắng nghe
    private val _unreadCountFlow = MutableStateFlow(0)
    val unreadCountFlow = _unreadCountFlow.asStateFlow()

    private var notificationListener: ListenerRegistration? = null

    /**
     * Hàm lắng nghe số lượng thông báo chưa đọc.
     * Tự động chạy ngay khi user mở App/Login.
     * Hỗ trợ mọi loại thông báo (Like, Comment, System...) miễn là isRead = false.
     */
    // PostRepository.kt

    fun startListeningToUnreadNotifications(userId: String) {
        if (notificationListener != null) return

        try {
            notificationListener = usersCollection.document(userId)
                .collection("notifications")
                .whereEqualTo("isRead", false)
                .limit(10) // Tối ưu: Chỉ lấy tối đa 10 tin để hiện "9+"
                .whereNotEqualTo("type", "MESSAGE")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        // Lọc thủ công ở đây (Client-side filtering)
                        val validCount = snapshot.documents.count { doc ->
                            val type = doc.getString("type")
                            type != "MESSAGE"
                        }

                        // Nếu bạn muốn hiển thị 9+ thì logic có thể phức tạp hơn xíu,
                        // nhưng cơ bản là gán vào flow
                        _unreadCountFlow.value = validCount
                    }
                }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Lấy danh sách thông báo Social (Real-time)
     * Sắp xếp: Mới nhất lên đầu
     */
    fun getSocialNotificationsFlow(userId: String): Flow<List<NotificationItem>> {
        return usersCollection.document(userId)
            .collection("notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(50)
            .snapshots()
            .map { snapshot ->
                // Thay đổi từ toObjects().mapIndexed sang documents.mapNotNull
                snapshot.documents.mapNotNull { doc ->
                    val item = doc.toObject(NotificationItem::class.java)

                    // Điều kiện lọc: item không null VÀ type khác "MESSAGE"
                    if (item != null && item.type != NotificationType.MESSAGE) {
                        item.copy(
                            id = doc.id,
                            isFromBroadcast = false
                        )
                    } else {
                        null // Bỏ qua phần tử này
                    }
                }
            }
    }

    /**
     * Đánh dấu 1 thông báo là đã đọc
     */
    suspend fun markNotificationAsRead(userId: String, notificationId: String) {
        try {
            usersCollection.document(userId)
                .collection("notifications")
                .document(notificationId)
                .update("isRead", true)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- MARK ALL LOGIC ---

    /**
     * 1. SOCIAL: Đánh dấu tất cả là đã đọc
     * Logic: Tìm tất cả tin chưa đọc -> Batch Update
     */
    suspend fun markAllSocialNotificationsAsRead(userId: String) {
        try {
            val snapshot = usersCollection.document(userId)
                .collection("notifications")
                .whereEqualTo("isRead", false)
                .get()
                .await()

            if (snapshot.isEmpty) return

            val batch = firestore.batch()
            for (doc in snapshot.documents) {
                batch.update(doc.reference, "isRead", true)
            }
            batch.commit().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 2. SYSTEM: Đánh dấu tất cả là đã đọc
     * Logic: Nhận vào danh sách ID các tin chưa đọc -> Batch Write tạo doc rỗng vào read_system_notifications
     */
    suspend fun markAllSystemNotificationsAsRead(userId: String, unreadIds: List<String>) {
        if (unreadIds.isEmpty()) return
        try {
            val batch = firestore.batch()
            val collectionRef = usersCollection.document(userId).collection("read_system_notifications")

            // Duyệt qua từng ID chưa đọc và thêm lệnh ghi vào batch
            for (id in unreadIds) {
                val docRef = collectionRef.document(id)
                batch.set(docRef, mapOf("readAt" to FieldValue.serverTimestamp()))
            }

            batch.commit().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Đánh dấu 1 danh sách ID cụ thể là đã đọc (Batch Update)
     * Dùng cho: Social List và Private System List
     */
    suspend fun markBatchNotificationsAsRead(userId: String, notificationIds: List<String>) {
        if (notificationIds.isEmpty()) return
        try {
            val batch = firestore.batch()
            val collectionRef = usersCollection.document(userId).collection("notifications")

            for (id in notificationIds) {
                val docRef = collectionRef.document(id)
                batch.update(docRef, "isRead", true)
            }
            batch.commit().await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- SYSTEM NOTIFICATION LOGIC ---

    /**
     * 1. Đăng ký nhận thông báo từ Topic (Gọi 1 lần khi mở App)
     */
    fun subscribeToSystemTopic() {
        FirebaseMessaging.getInstance().subscribeToTopic("global_alerts")
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    // Log lỗi nếu cần
                }
            }
    }

    /**
     * 2. Lấy danh sách thông báo hệ thống (Chung cho tất cả user)
     */
    fun getSystemNotificationsFlow(): Flow<List<NotificationItem>> {
        return firestore.collection("system_notifications")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(20)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { doc ->
                    NotificationItem(
                        id = doc.id,
                        type = NotificationType.SYSTEM,
                        senderName = doc.getString("title") ?: "System Admin",
                        message = doc.getString("message") ?: "",
                        timestamp = doc.getTimestamp("timestamp") ?: Timestamp.now(),
                        isRead = false,
                        isFromBroadcast = true // <--- ĐÁNH DẤU LÀ BROADCAST
                    )
                }
            }
    }

    /**
     * 1. Đánh dấu thông báo hệ thống là đã đọc
     * Logic: Tạo một document rỗng trong sub-collection "read_system_notifications" của user
     */
    suspend fun markSystemNotificationAsRead(userId: String, notificationId: String) {
        try {
            usersCollection.document(userId)
                .collection("read_system_notifications")
                .document(notificationId)
                .set(mapOf("readAt" to FieldValue.serverTimestamp()))
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 2. Lấy danh sách ID các thông báo hệ thống mà user ĐÃ ĐỌC
     * Dùng để so khớp (map) với danh sách thông báo lấy từ server
     */
    fun getReadSystemIdsFlow(userId: String): kotlinx.coroutines.flow.Flow<Set<String>> {
        return usersCollection.document(userId)
            .collection("read_system_notifications")
            .snapshots()
            .map { snapshot ->
                snapshot.documents.map { it.id }.toSet()
            }
    }

    /**
     * 3. Helper tính toán Badge tổng (Social + System)
     * Lưu ý: Logic tính toán thực tế nên để ở ViewModel, nhưng ta cần Flow system unread count ở đây
     */
    // Flow đếm tổng số thông báo hệ thống (Raw)
    fun getSystemTotalCountFlow(): kotlinx.coroutines.flow.Flow<Int> {
        return firestore.collection("system_notifications")
            .snapshots() // Lưu ý: Cách này sẽ tốn read nếu list dài, nhưng tạm chấp nhận cho admin notifs (số lượng ít)
            .map { it.size() }
    }

    /**
     * Hủy lắng nghe (Gọi khi Logout)
     */
    fun stopListeningToNotifications() {
        notificationListener?.remove()
        notificationListener = null
        _unreadCountFlow.value = 0
    }
}