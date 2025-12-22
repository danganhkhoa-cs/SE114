package com.example.se114.data.model

data class Review(
    val reviewerId: String = "",
    val reviewerName: String = "",
    val reviewerAvatar: String = "",
    val rating: Int = 0,
    val comment: String = "",
    val timestamp: Long = 0
)