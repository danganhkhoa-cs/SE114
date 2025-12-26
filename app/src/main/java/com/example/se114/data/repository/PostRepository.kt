package com.example.se114.data.repository

import com.example.se114.data.Post
import com.example.se114.data.Report
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
                .whereEqualTo("status", "PUBLISHED")
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

    // 3. Xử lý Like (Transaction: Update Count + Sub-collection)
    suspend fun toggleLikePost(postId: String, userId: String, currentLikeStatus: Boolean): Result<Unit> {
        return try {
            firestore.runTransaction { transaction ->
                val postRef = postsCollection.document(postId)
                val likeRef = postRef.collection("likes").document(userId)

                // Tham chiếu đến collection liked_posts của User
                val userLikeRef = usersCollection.document(userId).collection("liked_posts").document(postId)

                val postSnapshot = transaction.get(postRef)
                val currentCount = postSnapshot.getLong("likeCount") ?: 0

                val newCount = if (currentLikeStatus) {
                    if (currentCount > 0) currentCount - 1 else 0
                } else {
                    currentCount + 1
                }

                transaction.update(postRef, "likeCount", newCount)

                if (currentLikeStatus) {
                    // Đang like -> User muốn unlike -> Xóa cả 2 nơi
                    transaction.delete(likeRef)     // Xóa ở bài viết
                    transaction.delete(userLikeRef) // Xóa ở user profile
                } else {
                    // Chưa like -> User muốn like -> Thêm cả 2 nơi
                    val data = mapOf("timestamp" to FieldValue.serverTimestamp())
                    val userLikeData = mapOf("likedAt" to FieldValue.serverTimestamp())

                    transaction.set(likeRef, data.plus("userId" to userId))
                    transaction.set(userLikeRef, userLikeData)
                }
            }.await()
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
}