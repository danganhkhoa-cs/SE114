package com.example.se114.ui.presentation.profile

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class AccountDataUiState(
    // Dữ liệu hiển thị
    val address: String = "",
    val phone: String = "",
    val gender: String = "",
    val currentJob: String = "",

    // Trạng thái hiển thị Dialog
    val isShowingAddressDialog: Boolean = false,
    val isShowingGenderDialog: Boolean = false,
    val isShowingJobDialog: Boolean = false
)

@HiltViewModel
class AccountDataViewModel @Inject constructor(
    // Không Inject PreferencesManager vào đây theo yêu cầu
) : ViewModel() {

    private val _uiState = MutableStateFlow(AccountDataUiState())
    val uiState = _uiState.asStateFlow()

    // Hàm này được gọi từ Screen để set dữ liệu ban đầu từ Preferences
    fun setInitialData(address: String, phone: String, gender: String, job: String) {
        _uiState.update {
            it.copy(address = address, phone = phone, gender = gender, currentJob = job)
        }
    }

    // --- Dialog Controls ---

    fun showAddressDialog() {
        _uiState.update { it.copy(isShowingAddressDialog = true) }
    }

    fun hideAddressDialog() {
        _uiState.update { it.copy(isShowingAddressDialog = false) }
    }

    fun showGenderDialog() {
        _uiState.update { it.copy(isShowingGenderDialog = true) }
    }

    fun hideGenderDialog() {
        _uiState.update { it.copy(isShowingGenderDialog = false) }
    }

    fun showJobDialog() {
        _uiState.update { it.copy(isShowingJobDialog = true) }
    }

    fun hideJobDialog() {
        _uiState.update { it.copy(isShowingJobDialog = false) }
    }

    // --- Update State (Chỉ update UI, việc lưu Prefs nằm ở Screen) ---

    fun updateAddress(newAddress: String) {
        _uiState.update { it.copy(address = newAddress, isShowingAddressDialog = false) }
    }

    fun updateGender(newGender: String) {
        _uiState.update { it.copy(gender = newGender, isShowingGenderDialog = false) }
    }

    fun updateJob(newJob: String) {
        _uiState.update { it.copy(currentJob = newJob, isShowingJobDialog = false) }
    }
}