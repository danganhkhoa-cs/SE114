package com.example.se114.data.repository

import com.example.se114.data.Post
import com.example.se114.data.Report
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
                val avatarMap = mutableMapOf<String, String>()

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
                                    if (url.isNotEmpty()) {
                                        avatarMap[doc.id] = url
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }.awaitAll()
                }

                // Cập nhật avatar mới vào danh sách bài viết
                posts = posts.map { post ->
                    val freshAvatar = avatarMap[post.userId]
                    if (!freshAvatar.isNullOrEmpty()) {
                        post.copy(userAvatar = freshAvatar)
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
                    // Nếu lúc nãy là Unlike -> Xóa thông báo cũ
                    removeNotification(
                        receiverId = postOwnerId!!,
                        senderId = userId,
                        postId = postId,
                        type = "LIKE"
                    )
                } else {
                    // Nếu lúc nãy là Like -> Gửi thông báo mới
                    sendNotification(
                        receiverId = postOwnerId!!,
                        senderId = userId,
                        postId = postId,
                        type = "LIKE",
                        message = "liked your post" // Message này có thể để app client tự hiển thị theo ngôn ngữ
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

    // --- NOTIFICATION LOGIC (GENERIC) ---

    /**
     * Hàm dùng chung để gửi thông báo.
     * Sau này có Comment hay Friend Request thì chỉ cần gọi hàm này và đổi tham số 'type'.
     */
    private suspend fun sendNotification(
        receiverId: String,     // Người nhận (Chủ bài viết)
        senderId: String,       // Người gửi (Người đang like/comment)
        postId: String?,        // ID bài viết (Null nếu là Friend Request)
        type: String,           // "LIKE", "COMMENT", "FRIEND_REQUEST", "REPLY"
        message: String = ""    // Nội dung phụ (VD: nội dung comment)
    ) {
        // 1. Không thông báo nếu tự tương tác với chính mình
        if (receiverId == senderId) return

        try {
            // 2. Lấy thông tin người gửi để lưu vào thông báo (Snapshot tên/avatar lúc gửi)
            // (Giúp hiển thị nhanh mà không cần query lại user, nhưng nếu user đổi avatar thì tin cũ vẫn avatar cũ)
            val senderDoc = usersCollection.document(senderId).get().await()
            val senderName = senderDoc.getString("name") ?: "Someone"
            val senderAvatar = senderDoc.getString("avatar_url") ?: ""

            // 3. Tạo data
            val notificationData = hashMapOf(
                "type" to type,
                "senderId" to senderId,
                "senderName" to senderName,
                "senderAvatar" to senderAvatar,
                "postId" to postId,
                "message" to message,
                "isRead" to false,
                "timestamp" to FieldValue.serverTimestamp() // Quan trọng: Dùng Server Timestamp
            )

            // 4. Đẩy vào Sub-collection của người nhận
            usersCollection.document(receiverId)
                .collection("notifications")
                .add(notificationData)
                .await()

        } catch (e: Exception) {
            e.printStackTrace() // Log lỗi nhưng không crash app vì thông báo chỉ là phụ
        }
    }

    /**
     * Hàm dùng chung để xóa thông báo (VD: Unlike, Hủy kết bạn)
     */
    private suspend fun removeNotification(
        receiverId: String,
        senderId: String,
        postId: String?,
        type: String
    ) {
        try {
            // Tìm thông báo khớp với sender, type và post để xóa
            var query = usersCollection.document(receiverId)
                .collection("notifications")
                .whereEqualTo("senderId", senderId)
                .whereEqualTo("type", type)

            if (postId != null) {
                query = query.whereEqualTo("postId", postId)
            }

            val snapshot = query.get().await()
            for (doc in snapshot.documents) {
                doc.reference.delete()
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
    fun startListeningToUnreadNotifications(userId: String) {
        // Nếu đã lắng nghe đúng user này rồi thì thôi, tránh trùng lặp
        if (notificationListener != null) return

        try {
            notificationListener = usersCollection.document(userId)
                .collection("notifications")
                .whereEqualTo("isRead", false)
                .limit(10) // Tối ưu: Chỉ lấy tối đa 10 tin để hiện "9+"
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        _unreadCountFlow.value = snapshot.size()
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
                snapshot.toObjects(NotificationItem::class.java).mapIndexed { index, item ->
                    item.copy(
                        id = snapshot.documents[index].id,
                        isFromBroadcast = false // Đây là tin cá nhân
                    )
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