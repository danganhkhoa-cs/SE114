package com.example.se114.data.repository

import com.example.se114.data.Post
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val postsCollection = firestore.collection("posts")

    // 1. Lấy danh sách bài viết
    suspend fun getPosts(): Result<List<Post>> {
        return try {
            val snapshot = postsCollection
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()
            val posts = snapshot.toObjects(Post::class.java)
            // Gán lại ID từ document ID để đảm bảo chính xác
            val finalPosts = posts.mapIndexed { index, post ->
                post.copy(id = snapshot.documents[index].id)
            }
            Result.success(finalPosts)
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

    // 3. Xử lý Like (Giả lập + TODO)
    suspend fun toggleLikePost(postId: String, userId: String, currentLikeStatus: Boolean): Result<Unit> {
        // TODO: Implement Transaction để update likeCount và sub-collection 'likes'
        return Result.success(Unit)
    }

    // 4. Xử lý Save (Giả lập + TODO)
    suspend fun toggleSavePost(postId: String, userId: String, currentSaveStatus: Boolean): Result<Unit> {
        // TODO: Implement thêm/xóa doc vào sub-collection 'saved_posts' của user
        return Result.success(Unit)
    }

    // 5. Lấy danh sách ID bài đã lưu
    suspend fun getUserSavedPostIds(userId: String): Result<List<String>> {
        // TODO: Query thực tế từ collection users/{userId}/saved_posts
        return Result.success(emptyList())
    }
}