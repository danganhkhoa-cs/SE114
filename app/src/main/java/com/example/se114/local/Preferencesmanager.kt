package com.example.se114.local

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit
import com.example.se114.local.PreferenceKeys as Keys

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(Keys.PREF_NAME, Context.MODE_PRIVATE)

    // --- STATE FOR RECOMPOSITION ---
    // Đây là các biến trạng thái (State) giúp Jetpack Compose nhận biết thay đổi để vẽ lại giao diện ngay lập tức

    var languageState = mutableStateOf(prefs.getString(Keys.KEY_LANGUAGE, "English") ?: "English")
        private set

    var darkModeState = mutableStateOf(prefs.getBoolean(Keys.KEY_DARK_MODE, false))
        private set

    // --- SETTINGS ---
    var isDarkMode: Boolean
        get() = darkModeState.value
        set(value) {
            // 1. Cập nhật State -> UI đổi màu ngay lập tức
            darkModeState.value = value
            // 2. Lưu vào bộ nhớ máy
            prefs.edit { putBoolean(Keys.KEY_DARK_MODE, value) }
        }

    var language: String
        get() = languageState.value
        set(value) {
            // 1. Cập nhật State -> UI đổi chữ ngay lập tức
            languageState.value = value
            // 2. Lưu vào bộ nhớ máy
            prefs.edit { putString(Keys.KEY_LANGUAGE, value) }
        }

    // --- LOCALIZATION HELPER ---
    fun getString(key: String): String {
        // Truy cập vào .value để Compose biết cần phải theo dõi biến này
        val isVietnamese = languageState.value == "Tiếng Việt"

        return if (isVietnamese) StringResources.VI[key] ?: key
        else StringResources.EN[key] ?: key
    }

    // --- USER DATA --- (DUMMY DATA)
    var userName: String
        get() = prefs.getString(Keys.KEY_USER_NAME, "Jonathan") ?: "Jonathan"
        set(value) = prefs.edit { putString(Keys.KEY_USER_NAME, value) }

    var userEmail: String
        get() = prefs.getString(Keys.KEY_USER_EMAIL, "jonathan75@gmail.com") ?: "jonathan75@gmail.com"
        set(value) = prefs.edit { putString(Keys.KEY_USER_EMAIL, value) }

    var userPhone: String
        get() = prefs.getString(Keys.KEY_USER_PHONE, "0123456789") ?: "0123456789"
        set(value) = prefs.edit { putString(Keys.KEY_USER_PHONE, value) }

    // Logic xử lý song ngữ cho Bio/Address/Job/Gender
    var userBio: String
        get() = getLocalizedField(
            key = Keys.KEY_USER_BIO,
            defaultEn = "Love to travel ✈️ | Foodie 🍜",
            defaultVi = "Thích đi du lịch ✈️ | Tâm hồn ăn uống 🍜"
        )
        set(value) = prefs.edit { putString(Keys.KEY_USER_BIO, value) }

    var userAddress: String
        get() = getLocalizedField(
            key = Keys.KEY_USER_ADDRESS,
            defaultEn = "123 Nguyen Hue St, District 1, HCMC",
            defaultVi = "123 Nguyễn Huệ, Quận 1, TP.HCM"
        )
        set(value) = prefs.edit { putString(Keys.KEY_USER_ADDRESS, value) }

    var userJob: String
        get() = getLocalizedField(
            key = Keys.KEY_USER_JOB,
            defaultEn = "Software Engineer",
            defaultVi = "Kỹ sư phần mềm"
        )
        set(value) = prefs.edit { putString(Keys.KEY_USER_JOB, value) }

    var userGender: String
        get() {
            val defaultEn = "Male"
            val defaultVi = "Nam"
            val saved = prefs.getString(Keys.KEY_USER_GENDER, null)

            // Nếu chưa lưu hoặc đang là default, trả về theo ngôn ngữ hiện tại
            if (saved == null || saved == defaultEn || saved == defaultVi) {
                return if (language == "Tiếng Việt") defaultVi else defaultEn
            }

            // Map các giá trị đặc biệt
            val isVi = language == "Tiếng Việt"
            return when (saved) {
                "Male", "Nam" -> if (isVi) "Nam" else "Male"
                "Female", "Nữ" -> if (isVi) "Nữ" else "Female"
                "Other", "Khác" -> if (isVi) "Khác" else "Other"
                "Prefer not to say", "Không muốn tiết lộ" -> if (isVi) "Không muốn tiết lộ" else "Prefer not to say"
                else -> saved
            }
        }
        set(value) = prefs.edit { putString(Keys.KEY_USER_GENDER, value) }

    // --- HELPER FUNCTIONS ---
    private fun getLocalizedField(key: String, defaultEn: String, defaultVi: String): String {
        val saved = prefs.getString(key, null)
        // Vì hàm này gọi 'language' (mà 'language' gọi 'languageState.value'),
        // nên Compose cũng sẽ tự động vẽ lại các trường này khi đổi ngôn ngữ.
        if (saved == null || saved == defaultEn || saved == defaultVi) {
            return if (language == "Tiếng Việt") defaultVi else defaultEn
        }
        return saved
    }

    fun clearUserData() {
        // Save current settings
        val keepDarkMode = isDarkMode
        val keepLanguage = language

        prefs.edit { clear() }

        // Restore settings
        // Lưu ý: Việc gán lại này cũng sẽ kích hoạt State update, đảm bảo UI đồng bộ
        isDarkMode = keepDarkMode
        language = keepLanguage
    }

    fun clearAll() {
        prefs.edit { clear() }
        // Reset states về default nếu cần, hoặc để nguyên tùy logic app
    }
}