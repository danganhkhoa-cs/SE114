package com.example.se114

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.se114.local.PreferencesManager
import com.example.se114.utils.CurrentChatManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {

        val preferencesManager = PreferencesManager(this)

        // 1. Kiểm tra nếu đang ở màn hình Notification -> Chặn
        if (CurrentChatManager.isNotificationScreenVisible) {
            return
        }

        // 2. Lấy dữ liệu thuần từ Data Payload (Do backend bạn gửi dạng Data Message)
        val data = remoteMessage.data
        if (data.isNotEmpty()) {
            val type = data["type"] ?: ""
            val messageContent = data["message"] ?: ""
            val senderId = data["senderId"] ?: ""
            val senderName = data["senderName"] ?: "LocaSOS" // Default nếu không có tên
            Log.d("MyFirebaseMessagingService", "onMessageReceived: $type, $messageContent, $senderId, $senderName")
            // 3. Kiểm tra logic chặn chat: Nếu đang chat với đúng người gửi này -> BỎ QUA
            if (senderId.isNotEmpty() && senderId == CurrentChatManager.currentPartnerId) {
                return
            }

            // 4. Phân loại để set Title và Body
            handleNotificationByType(type, senderName, messageContent, preferencesManager)
        }
    }

    // Hàm xử lý logic phân loại nội dung hiển thị (Title & Body)
    private fun handleNotificationByType(type: String, senderName: String, message: String, preferencesManager: PreferencesManager) {
        var title = ""
        var body = ""

        when (type) {
            // HÀM 1: Nhóm tương tác xã hội
            "LIKE", "LIKE_COMMENT", "COMMENT", "REPLY", "FRIEND_REQUEST" -> {
                title = "LocaSOS"
                body = "$senderName ${preferencesManager.getString(message)}"
            }

            // HÀM 2: Nhóm tin nhắn
            "MESSAGE" -> {
                title = senderName
                body = message
            }

            // Hàm 3: Nhóm hệ thống
            "SYSTEM" -> {
                title = preferencesManager.getString(senderName)
                body = message
            }

            // Trường hợp khác (Dự phòng)
            else -> {
                title = "LocaSOS"
                body = message
            }
        }

        // Sau khi có title và body chuẩn, gọi hàm hiển thị
        sendNotification(title, body)
    }

    private fun sendNotification(title: String, messageBody: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "locasos_channel_id"
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Tạo channel cho Android O trở lên
        val channel = NotificationChannel(
            channelId,
            "LocaSOS Notifications",
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.description = "Thông báo từ ứng dụng LocaSOS"
        channel.enableVibration(true)
        channel.enableLights(true)
        notificationManager.createNotificationChannel(channel)

        val notificationId = Random.nextInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Code gửi token lên server
    }
}