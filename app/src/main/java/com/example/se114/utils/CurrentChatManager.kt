package com.example.se114.utils

object CurrentChatManager {
    // Lưu ID của người đang chat cùng. Null nếu không ở trong màn hình chat.
    var currentPartnerId: String? = null

    // Biến kiểm tra xem người dùng có đang ở màn hình Notification không
    var isNotificationScreenVisible: Boolean = false
}