package com.example.se114

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.se114.local.PreferencesManager
import com.example.se114.utils.CurrentChatManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // Nếu đang ở màn hình Notification -> Chặn TẤT CẢ thông báo
        if (CurrentChatManager.isNotificationScreenVisible) {
            return
        }

        val preferencesManager = PreferencesManager(this)
        // Lấy dữ liệu từ gói tin FCM
        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "LocaSOS"
        val message = remoteMessage.notification?.body ?: remoteMessage.data["message"] ?: "Bạn có thông báo mới"

        val senderId = remoteMessage.data["senderId"]
        // Nếu người gửi tin nhắn chính là người mình đang chat trên màn hình -> BỎ QUA
        if (senderId != null && senderId == CurrentChatManager.currentPartnerId) {
            return
        }

        // Hiện thông báo
        sendNotification(title, message)
    }

    private fun sendNotification(title: String, messageBody: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        // Tạo PendingIntent để mở App khi bấm vào thông báo
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "locasos_channel_id" // Phải trùng với Manifest
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // Cấu hình giao diện thông báo
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Đổi thành icon app của bạn
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // QUAN TRỌNG: Để popup trên Android cũ
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Rung + Chuông

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            "LocaSOS Notifications",
            NotificationManager.IMPORTANCE_HIGH // QUAN TRỌNG NHẤT: Bắt buộc HIGH để có Popup
        )
        channel.description = "Thông báo từ ứng dụng LocaSOS"
        channel.enableVibration(true)
        channel.enableLights(true)

        notificationManager.createNotificationChannel(channel)

        // Hiển thị thông báo
        val notificationId = Random.nextInt()
        notificationManager.notify(notificationId, notificationBuilder.build())
    }

    override fun onNewToken(token: String) {
        // Xử lý khi có token mới (Gửi lên server nếu cần)
        super.onNewToken(token)
    }
}