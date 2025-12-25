package com.example.se114.data

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

// Dữ liệu gói trong sự kiện
data class PostUpdateEvent(
    val postId: String,
    val isLiked: Boolean? = null,
    val likeCount: Int? = null,
    val commentCount: Int? = null,
    val isSaved: Boolean? = null
)

@Singleton
class PostEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<PostUpdateEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    suspend fun emitEvent(event: PostUpdateEvent) {
        _events.emit(event)
    }
}