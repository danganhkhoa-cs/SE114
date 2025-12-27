package com.example.se114.ui.presentation.navigation

import androidx.lifecycle.ViewModel
import com.example.se114.data.repository.PostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val repository: PostRepository
) : ViewModel() {

    fun logout() {
        // Gọi hàm stop lắng nghe từ instance Singleton chuẩn của Hilt
        repository.stopListeningToNotifications()
    }
}