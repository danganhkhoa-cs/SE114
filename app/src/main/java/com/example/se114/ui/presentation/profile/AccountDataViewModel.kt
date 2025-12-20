package com.example.se114.ui.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.se114.local.PreferencesManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class AccountDataUiState(
    val address: String = "",
    val phone: String = "",
    val gender: String = "",
    val currentJob: String = "",

    val isShowingAddressDialog: Boolean = false,
    val isShowingGenderDialog: Boolean = false,
    val isShowingJobDialog: Boolean = false,

    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class AccountDataViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountDataUiState())
    val uiState = _uiState.asStateFlow()

    // --- REFRESH DATA ---
    fun refreshData() {
        _uiState.update {
            it.copy(
                address = preferencesManager.userAddress,
                phone = preferencesManager.userPhone,
                gender = preferencesManager.userGender,
                currentJob = preferencesManager.userJob
            )
        }
    }

    // Helper update chung
    private fun updateField(fieldName: String, value: Any, onSuccess: () -> Unit) {
        val uid = preferencesManager.userId
        if (uid.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                firestore.collection("users").document(uid)
                    .update(fieldName, value)
                    .await()

                onSuccess()
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    // --- Update ADDRESS ---
    fun updateAddress(newAddress: String) {
        updateField("address", newAddress) {
            preferencesManager.userAddress = newAddress
            _uiState.update { it.copy(address = newAddress, isShowingAddressDialog = false) }
        }
    }

    // --- Update GENDER (SỬA LẠI LOGIC LƯU STRING) ---
    fun updateGender(newGender: String) {
        val uid = preferencesManager.userId
        if (uid.isBlank()) return

        // Chuẩn hóa dữ liệu để lưu vào DB (Lưu tiếng Anh để thống nhất)
        val genderToSave = when (newGender) {
            "Nam", "Male" -> "Male"
            "Nữ", "Female" -> "Female"
            "Khác", "Other" -> "Other"
            "Không muốn tiết lộ", "Prefer not to say" -> "Prefer not to say"
            else -> "Other"
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // Lưu String vào Firestore thay vì Boolean
                firestore.collection("users").document(uid)
                    .update("gender", genderToSave)
                    .await()

                // Cập nhật Local (Giữ nguyên string hiển thị để UI update ngay lập tức)
                preferencesManager.userGender = newGender

                _uiState.update { it.copy(gender = newGender, isShowingGenderDialog = false, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    // --- Update JOB ---
    fun updateJob(newJob: String) {
        updateField("job", newJob) {
            preferencesManager.userJob = newJob
            _uiState.update { it.copy(currentJob = newJob, isShowingJobDialog = false) }
        }
    }

    // --- Dialog Controls ---
    fun showAddressDialog() { _uiState.update { it.copy(isShowingAddressDialog = true) } }
    fun hideAddressDialog() { _uiState.update { it.copy(isShowingAddressDialog = false) } }

    fun showGenderDialog() { _uiState.update { it.copy(isShowingGenderDialog = true) } }
    fun hideGenderDialog() { _uiState.update { it.copy(isShowingGenderDialog = false) } }

    fun showJobDialog() { _uiState.update { it.copy(isShowingJobDialog = true) } }
    fun hideJobDialog() { _uiState.update { it.copy(isShowingJobDialog = false) } }
}