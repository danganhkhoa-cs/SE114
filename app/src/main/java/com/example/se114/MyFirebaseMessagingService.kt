package com.example.se114

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    // Hàm này tự động chạy khi token của thiết bị thay đổi (cài lại app, xóa data...)
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            // Lưu token mới ngay lập tức
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUser.uid)
                .update("fcm_token", token)
        }
    }

    // Hàm nhận tin nhắn khi app đang mở (Foreground)
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        // Hiện tại để trống, Giai đoạn 3 chúng ta sẽ xử lý hiển thị sau
    }
}