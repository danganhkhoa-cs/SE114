package com.example.se114

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.se114.local.PreferencesManager
import com.example.se114.ui.presentation.navigation.AppNavigation
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // 1. Inject các thư viện Firebase cần thiết
    @Inject
    lateinit var firestore: FirebaseFirestore

    @Inject
    lateinit var auth: FirebaseAuth

    private lateinit var preferencesManager: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Khởi tạo PreferencesManager như cũ
        preferencesManager = PreferencesManager(this)

        // 2. Kích hoạt lắng nghe trạng thái tài khoản ngay khi App mở
        monitorAccountStatus()

        enableEdgeToEdge()
        setContent {
            AppNavigation(preferencesManager)
        }
    }

    /**
     * Hàm lắng nghe Realtime:
     * Nếu ID của user hiện tại xuất hiện trong collection 'suspended_users',
     * lập tức đăng xuất và khởi động lại App.
     */
    private fun monitorAccountStatus() {
        val currentUser = auth.currentUser ?: return

        // Lắng nghe document trùng với UID của mình trong collection cấm
        firestore.collection("suspended_users").document(currentUser.uid)
            .addSnapshotListener { snapshot, error ->
                // Nếu có lỗi hoặc người dùng tự đăng xuất thì bỏ qua
                if (error != null || auth.currentUser == null) return@addSnapshotListener

                // Kiểm tra xem document có tồn tại không
                if (snapshot != null && snapshot.exists()) {
                    // --- PHÁT HIỆN TÀI KHOẢN BỊ KHÓA ---

                    // 1. Đăng xuất khỏi Firebase Auth
                    auth.signOut()

                    // 2. Xóa dữ liệu user lưu trong máy (để UI cập nhật sạch sẽ)
                    preferencesManager.clearUserData()

                    // 3. Thông báo cho người dùng
                    Toast.makeText(
                        this,
                        "Tài khoản của bạn đã bị Admin khóa vĩnh viễn do vi phạm.",
                        Toast.LENGTH_LONG
                    ).show()

                    // 4. Khởi động lại Activity để App quay về màn hình Đăng nhập (LoginScreen)
                    // (Giả định AppNavigation sẽ check trạng thái login khi khởi tạo)
                    val intent = intent
                    finish()
                    startActivity(intent)
                }
            }
    }
}