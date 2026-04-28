package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyllar.consumer.data.remote.model.dto.CheckPanResponseDto
import com.pyllar.consumer.domain.repository.OnboardingRepository
import com.pyllar.consumer.domain.storage.SessionStore
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.util.platformLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class PanKycViewModel(
    private val onboardingRepository: OnboardingRepository,
    private val sessionStore: SessionStore
) : ViewModel() {

    private val _panCheckResult = MutableStateFlow<Resource<CheckPanResponseDto>?>(null)
    val panCheckResult: StateFlow<Resource<CheckPanResponseDto>?> = _panCheckResult

    private val _pan = MutableStateFlow("")
    val pan: StateFlow<String> = _pan

    fun updatePan(newPan: String) {
        if (newPan.length <= 10) {
            _pan.value = newPan
        }
    }

    private fun isValidPan(pan: String): Boolean {
        return Regex("^[A-Z]{5}[0-9]{4}[A-Z]").matches(pan)
    }

    fun checkPan() {
        val currentPan = _pan.value
        if (!isValidPan(currentPan)) {
            _panCheckResult.value = Resource.Error("Invalid PAN format. Example: ABCDE1234F")
            platformLog("PanKycViewModel: checkPan: Invalid PAN format. PAN=$currentPan")
            return
        }

        viewModelScope.launch {
            _panCheckResult.value = Resource.Loading()
            platformLog("PanKycViewModel: checkPan: Loading")
            try {
                val userId = sessionStore.getCurrentUserId()
                if (userId.isBlank()) {
                    _panCheckResult.value = Resource.Error("User ID not found")
                    return@launch
                }
                
                onboardingRepository.checkPan(currentPan, userId).collect { result ->
                    platformLog("PanKycViewModel: checkPan: Resource=$result")
                    if (result is Resource.Success) {
                        sessionStore.saveValue(com.pyllar.consumer.data.local.KeyValueConstants.PAN, currentPan)
                    }
                    _panCheckResult.value = result
                }
            } catch (e: Exception) {
                platformLog("PanKycViewModel: checkPan: Exception ${e.message}")
                _panCheckResult.value = Resource.Error("Network error: ${e.message}")
            }
        }
    }

    fun clearResult() {
        _panCheckResult.value = null
    }
}
