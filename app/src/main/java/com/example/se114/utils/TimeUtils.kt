package com.example.se114.utils

import com.example.se114.local.PreferencesManager
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
object TimeUtils {
    fun getTimeAgo(timestamp: Timestamp, preferenceManager: PreferencesManager): String {
        val now = Date().time
        val time = timestamp.toDate().time
        val diff = now - time

        return when {
            diff < TimeUnit.MINUTES.toMillis(1) -> preferenceManager.getString("time_just_now")
            diff < TimeUnit.MINUTES.toMillis(60) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)} ${preferenceManager.getString("time_minutes_ago")}"
            diff < TimeUnit.HOURS.toMillis(24) -> "${TimeUnit.MILLISECONDS.toHours(diff)} ${preferenceManager.getString("time_hours_ago")}"
            diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)} ${preferenceManager.getString("time_days_ago")}"
            else -> {
                // Nếu quá 7 ngày thì hiển thị ngày tháng năm (VD: 12/05/2024)
                val date = timestamp.toDate()
                val format = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                format.format(date)
            }
        }
    }
}