package com.example.se114.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.edit

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_USER_NAME = "user_name"
        private const val KEY_USER_BIO = "user_bio"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_USER_PHONE = "user_phone"
        private const val KEY_USER_ADDRESS = "user_address"
        private const val KEY_USER_GENDER = "user_gender"
        private const val KEY_USER_JOB = "user_job"
    }

    // --- STATE FOR RECOMPOSITION ---
    // Observable state for language changes
    var languageState = mutableStateOf(language)
        private set

    // --- SETTINGS ---

    var isDarkMode: Boolean
        get() = prefs.getBoolean(KEY_DARK_MODE, false)
        set(value) = prefs.edit { putBoolean(KEY_DARK_MODE, value) }

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, "English") ?: "English"
        set(value) {
            prefs.edit { putString(KEY_LANGUAGE, value) }
            languageState.value = value // Update state to trigger UI refresh
        }

    // --- LOCALIZATION HELPER ---
    fun getString(key: String): String {
        val isVietnamese = language == "Tiếng Việt"
        return if (isVietnamese) STRINGS_VI[key] ?: key else STRINGS_EN[key] ?: key
    }

    // --- FIXED ACCOUNT DATA (AUTO TRANSLATE) ---

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "Jonathan") ?: "Jonathan"
        set(value) = prefs.edit { putString(KEY_USER_NAME, value) }

    var userBio: String
        get() {
            val defaultEn = "Love to travel ✈️ | Foodie 🍜"
            val defaultVi = "Thích đi du lịch ✈️ | Tâm hồn ăn uống 🍜"
            val saved = prefs.getString(KEY_USER_BIO, null)
            if (saved == null || saved == defaultEn || saved == defaultVi) {
                return if (language == "Tiếng Việt") defaultVi else defaultEn
            }
            return saved
        }
        set(value) = prefs.edit { putString(KEY_USER_BIO, value) }

    var userEmail: String
        get() = prefs.getString(KEY_USER_EMAIL, "jonathan75@gmail.com") ?: "jonathan75@gmail.com"
        set(value) = prefs.edit { putString(KEY_USER_EMAIL, value) }

    var userPhone: String
        get() = prefs.getString(KEY_USER_PHONE, "0123456789") ?: "0123456789"
        set(value) = prefs.edit { putString(KEY_USER_PHONE, value) }

    var userAddress: String
        get() {
            val defaultEn = "123 Nguyen Hue St, District 1, HCMC"
            val defaultVi = "123 Nguyễn Huệ, Quận 1, TP.HCM"
            val saved = prefs.getString(KEY_USER_ADDRESS, null)
            if (saved == null || saved == defaultEn || saved == defaultVi) {
                return if (language == "Tiếng Việt") defaultVi else defaultEn
            }
            return saved
        }
        set(value) = prefs.edit { putString(KEY_USER_ADDRESS, value) }

    var userGender: String
        get() {
            val defaultEn = "Male"
            val defaultVi = "Nam"
            val saved = prefs.getString(KEY_USER_GENDER, null)
            if (saved == null || saved == defaultEn || saved == defaultVi) {
                return if (language == "Tiếng Việt") defaultVi else defaultEn
            }
            return when(saved) {
                "Male", "Nam" -> if(language == "Tiếng Việt") "Nam" else "Male"
                "Female", "Nữ" -> if(language == "Tiếng Việt") "Nữ" else "Female"
                "Other", "Khác" -> if(language == "Tiếng Việt") "Khác" else "Other"
                "Prefer not to say", "Không muốn tiết lộ" -> if(language == "Tiếng Việt") "Không muốn tiết lộ" else "Prefer not to say"
                else -> saved ?: "Male"
            }
        }
        set(value) = prefs.edit { putString(KEY_USER_GENDER, value) }

    var userJob: String
        get() {
            val defaultEn = "Software Engineer"
            val defaultVi = "Kỹ sư phần mềm"
            val saved = prefs.getString(KEY_USER_JOB, null)
            if (saved == null || saved == defaultEn || saved == defaultVi) {
                return if (language == "Tiếng Việt") defaultVi else defaultEn
            }
            return saved
        }
        set(value) = prefs.edit { putString(KEY_USER_JOB, value) }

    fun clearUserData() {
        val keepDarkMode = isDarkMode
        val keepLanguage = language
        prefs.edit { clear() }
        isDarkMode = keepDarkMode
        language = keepLanguage
    }

    fun clearAll() {
        prefs.edit { clear() }
    }

    // --- DICTIONARY DATA ---
    private val STRINGS_EN = mapOf(
        // Home Tabs
        "tab_everyone" to "Everyone",
        "tab_foryou" to "For You",

        // General
        "cancel" to "Cancel",
        "save" to "Save",
        "confirm" to "Confirm",
        "back" to "Back",
        "close" to "Close",
        "next" to "Next",
        "edit" to "Edit",
        "copy" to "Copy",
        "yes" to "Yes",
        "no" to "No",

        // Profile Screen
        "profile_title" to "Profile",
        "account_settings" to "Account Settings",
        "account_data" to "Account Data",
        "help_support" to "Help & Support",
        "settings" to "Settings",
        "logout" to "Logout",
        "change_avatar" to "Change Avatar",
        "edit_profile" to "Edit Profile",
        "name" to "Name",
        "bio" to "Bio / Status",
        "name_empty_error" to "Name cannot be empty",
        "logout_confirm_title" to "Logout Confirmation",
        "logout_confirm_msg" to "Are you sure you want to logout?",
        "avatar_updated" to "Avatar updated!",

        // Account Settings Screen
        "email" to "Email",
        "password" to "Password",
        "phone_number" to "Phone Number",
        "change_password" to "Change Password",
        "current_password" to "Current Password",
        "new_password" to "New Password",
        "confirm_password" to "Confirm Password",
        "passwords_not_match" to "Passwords do not match",
        "password_length_error" to "Password must be at least 6 characters",
        "verify_password" to "Verify Password",
        "verify_password_msg" to "Please enter your password to change phone number",
        "password_required" to "Password is required",
        "change_phone" to "Change Phone Number",
        "current" to "Current",
        "new_phone" to "New Phone Number",
        "phone_required" to "Phone number is required",
        "invalid_phone" to "Invalid phone number format",

        // Account Data Screen
        "address" to "Address",
        "gender" to "Gender",
        "current_job" to "Current Job",
        "edit_in_settings" to "Edit in Account Settings",
        "edit_address" to "Edit Address",
        "enter_address" to "Enter your address",
        "edit_job" to "Edit Current Job",
        "enter_job" to "Enter your current job",
        "field_empty_error" to "This field cannot be empty",
        "select_gender" to "Select Gender",
        "male" to "Male",
        "female" to "Female",
        "other" to "Other",
        "prefer_not_to_say" to "Prefer not to say",

        // Help & Support Screen
        "need_help" to "Need Help?",
        "contact_support_msg" to "Contact our support team for assistance",
        "contact_info" to "Contact Information",
        "phone_copied" to "Phone number copied!",
        "email_copied" to "Email address copied!",
        "support_hours" to "Support Hours",
        "hours_detail" to "Monday - Friday: 8:00 AM - 8:00 PM\nSaturday - Sunday: 9:00 AM - 6:00 PM",

        // Settings Screen
        "language" to "Language",
        "theme" to "Theme",
        "blocked_users" to "Blocked Users",
        "manage_blocked" to "Manage blocked accounts",
        "delete_account" to "Delete Account",
        "delete_account_msg" to "Permanently delete your account",
        "select_language" to "Select Language",
        "select_theme" to "Select Theme",
        "light_mode" to "Light Mode",
        "dark_mode" to "Dark Mode",
        "light_mode_desc" to "Bright and clean interface",
        "dark_mode_desc" to "Easy on the eyes, gentle colors",
        "apply" to "Apply",
        "no_blocked_users" to "No blocked users",
        "unblock" to "Unblock",
        "delete_account_title" to "Delete Account",
        "delete_warning_title" to "⚠️ This action cannot be undone!",
        "delete_warning_desc" to "• All your data will be permanently deleted\n• Your posts and comments will be removed\n• You cannot recover your account",
        "continue" to "Continue",
        "enter_password_continue" to "Enter your password to continue",
        "type_delete" to "Type 'DELETE' to confirm",
        "delete_forever" to "Delete Forever",
        "type_delete_exact" to "Please type DELETE exactly"
    )

    private val STRINGS_VI = mapOf(
        // Home Tabs
        "tab_everyone" to "Mọi người",
        "tab_foryou" to "Dành cho bạn",

        // General
        "cancel" to "Hủy",
        "save" to "Lưu",
        "confirm" to "Xác nhận",
        "back" to "Quay lại",
        "close" to "Đóng",
        "next" to "Tiếp theo",
        "edit" to "Sửa",
        "copy" to "Sao chép",
        "yes" to "Có",
        "no" to "Không",

        // Profile Screen
        "profile_title" to "Hồ sơ",
        "account_settings" to "Cài đặt tài khoản",
        "account_data" to "Dữ liệu cá nhân",
        "help_support" to "Trợ giúp & Hỗ trợ",
        "settings" to "Cài đặt",
        "logout" to "Đăng xuất",
        "logout_confirm_title" to "Xác nhận đăng xuất",
        "logout_confirm_msg" to "Bạn có chắc chắn muốn đăng xuất không?",
        "avatar_updated" to "Đã cập nhật ảnh đại diện!",
        "change_avatar" to "Đổi ảnh đại diện",
        "edit_profile" to "Chỉnh sửa hồ sơ",
        "name" to "Tên",
        "bio" to "Tiểu sử / Trạng thái",
        "name_empty_error" to "Tên không được để trống",

        // Account Settings Screen
        "email" to "Email",
        "password" to "Mật khẩu",
        "phone_number" to "Số điện thoại",
        "change_password" to "Đổi mật khẩu",
        "current_password" to "Mật khẩu hiện tại",
        "new_password" to "Mật khẩu mới",
        "confirm_password" to "Xác nhận mật khẩu",
        "passwords_not_match" to "Mật khẩu không khớp",
        "password_length_error" to "Mật khẩu phải có ít nhất 6 ký tự",
        "verify_password" to "Xác thực mật khẩu",
        "verify_password_msg" to "Vui lòng nhập mật khẩu để đổi số điện thoại",
        "password_required" to "Vui lòng nhập mật khẩu",
        "change_phone" to "Đổi số điện thoại",
        "current" to "Hiện tại",
        "new_phone" to "Số điện thoại mới",
        "phone_required" to "Vui lòng nhập số điện thoại",
        "invalid_phone" to "Định dạng số điện thoại không hợp lệ",

        // Account Data Screen
        "address" to "Địa chỉ",
        "gender" to "Giới tính",
        "current_job" to "Công việc hiện tại",
        "edit_in_settings" to "Chỉnh sửa trong Cài đặt tài khoản",
        "edit_address" to "Sửa địa chỉ",
        "enter_address" to "Nhập địa chỉ của bạn",
        "edit_job" to "Sửa công việc hiện tại",
        "enter_job" to "Nhập công việc hiện tại",
        "field_empty_error" to "Trường này không được để trống",
        "select_gender" to "Chọn giới tính",
        "male" to "Nam",
        "female" to "Nữ",
        "other" to "Khác",
        "prefer_not_to_say" to "Không muốn tiết lộ",

        // Help & Support Screen
        "need_help" to "Cần trợ giúp?",
        "contact_support_msg" to "Liên hệ đội ngũ hỗ trợ để được giúp đỡ",
        "contact_info" to "Thông tin liên hệ",
        "phone_copied" to "Đã sao chép số điện thoại!",
        "email_copied" to "Đã sao chép địa chỉ email!",
        "support_hours" to "Giờ làm việc",
        "hours_detail" to "Thứ 2 - Thứ 6: 8:00 Sáng - 8:00 Tối\nThứ 7 - Chủ Nhật: 9:00 Sáng - 6:00 Tối",

        // Settings Screen
        "language" to "Ngôn ngữ",
        "theme" to "Giao diện",
        "blocked_users" to "Người dùng đã chặn",
        "manage_blocked" to "Quản lý danh sách chặn",
        "delete_account" to "Xóa tài khoản",
        "delete_account_msg" to "Xóa vĩnh viễn tài khoản của bạn",
        "select_language" to "Chọn ngôn ngữ",
        "select_theme" to "Chọn giao diện",
        "light_mode" to "Chế độ Sáng",
        "dark_mode" to "Chế độ Tối",
        "light_mode_desc" to "Giao diện sáng sủa và sạch sẽ",
        "dark_mode_desc" to "Dịu mắt, tông màu nhẹ nhàng",
        "apply" to "Áp dụng",
        "no_blocked_users" to "Không có người dùng bị chặn",
        "unblock" to "Bỏ chặn",
        "delete_account_title" to "Xóa Tài Khoản",
        "delete_warning_title" to "⚠️ Hành động này không thể hoàn tác!",
        "delete_warning_desc" to "• Tất cả dữ liệu của bạn sẽ bị xóa vĩnh viễn\n• Bài đăng và bình luận sẽ bị gỡ bỏ\n• Bạn không thể khôi phục tài khoản",
        "continue" to "Tiếp tục",
        "enter_password_continue" to "Nhập mật khẩu để tiếp tục",
        "type_delete" to "Gõ 'DELETE' để xác nhận",
        "delete_forever" to "Xóa Vĩnh Viễn",
        "type_delete_exact" to "Vui lòng gõ chính xác chữ DELETE"
    )
}