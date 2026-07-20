package com.pyllar.consumer.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyllar.consumer.data.remote.model.dto.RedemptionRequest
import com.pyllar.consumer.data.remote.model.dto.RedemptionResponse
import com.pyllar.consumer.data.remote.model.dto.RedemptionOtpVerifyRequestDto
import com.pyllar.consumer.domain.repository.RedemptionRepository
import com.pyllar.consumer.domain.storage.SessionStore
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.util.platformLog
import com.pyllar.consumer.util.toUserFriendlyErrorMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class WithdrawAmountViewModel(
    private val redemptionRepository: RedemptionRepository,
    private val sessionStore: SessionStore
) : ViewModel() {

    private val _redemptionResult = MutableStateFlow<Resource<RedemptionResponse>?>(null)
    val redemptionResult: StateFlow<Resource<RedemptionResponse>?> = _redemptionResult.asStateFlow()

    private val _isCreatingRedemption = MutableStateFlow(false)
    val isCreatingRedemption: StateFlow<Boolean> = _isCreatingRedemption.asStateFlow()

    private val _otpVerificationResult = MutableStateFlow<Resource<String>?>(null)
    val otpVerificationResult: StateFlow<Resource<String>?> = _otpVerificationResult.asStateFlow()

    private val _otpGenerationResult = MutableStateFlow<Resource<String>?>(null)
    val otpGenerationResult: StateFlow<Resource<String>?> = _otpGenerationResult.asStateFlow()

    private val _tokenTrackerId = MutableStateFlow<String?>(null)
    val tokenTrackerId: StateFlow<String?> = _tokenTrackerId.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                // Read from session or shared preferences in KMP
                val existingId = sessionStore.getAuthToken() // Placeholder, you should store this properly in Session
                // For KMP, we will use an in-memory approach or pass it via state since KeyValueConstants and SharedPreferences might differ
                // Let's rely on _tokenTrackerId logic as the primary way.
            } catch (e: Exception) {
                platformLog("⚠️ [init] Failed to load stored token tracker id: ${e.message}")
            }
        }
    }

    fun generateRedemptionOtp(userId: String) {
        viewModelScope.launch {
            _otpGenerationResult.value = Resource.Loading()
            try {
                platformLog("🔍 [generateRedemptionOtp] Generating OTP for user: $userId")
                redemptionRepository.generateRedemptionOtp(userId).collectLatest { result ->
                    when (result) {
                        is Resource.Success -> {
                            val trackerId = result.data?.tokenTrackerId
                            
                            if (!trackerId.isNullOrBlank()) {
                                _tokenTrackerId.value = trackerId
                                platformLog("✅ [generateRedemptionOtp] Stored tokenTrackerId: $trackerId")
                            } else {
                                platformLog("⚠️ [generateRedemptionOtp] Response missing tokenTrackerId")
                            }

                            val successMessage = result.message ?: "OTP sent to your phone"
                            platformLog("✅ [generateRedemptionOtp] OTP generated successfully.")
                            _otpGenerationResult.value = Resource.Success(successMessage)
                        }
                        is Resource.Error -> {
                            platformLog("❌ [generateRedemptionOtp] OTP generation failed: ${result.message}")
                            val errorMsg = sanitizeErrorMessage(result.message ?: "", "Error occurred. Please try again after some time")
                            _otpGenerationResult.value = Resource.Error(errorMsg)
                        }
                        is Resource.Loading -> { }
                    }
                }
            } catch (e: Exception) {
                platformLog("❌ [generateRedemptionOtp] Error: ${e.message}")
                _otpGenerationResult.value = Resource.Error("Something went wrong. Please try again.")
            }
        }
    }

    fun createRedemption(request: RedemptionRequest) {
        viewModelScope.launch {
            _isCreatingRedemption.value = true
            redemptionRepository.createRedemption(request).collectLatest { result ->
                _redemptionResult.value = result
                _isCreatingRedemption.value = false
            }
        }
    }

    fun verifyRedemptionOtp(userId: String, phoneNumber: String, otp: String, tokenTrackerIdParam: String?) {
        viewModelScope.launch {
            _otpVerificationResult.value = Resource.Loading()
            try {
                platformLog("🔍 [verifyRedemptionOtp] Verifying OTP for user: $userId, phone: $phoneNumber")
                val trackerId = tokenTrackerIdParam?.takeIf { it.isNotBlank() }
                    ?: _tokenTrackerId.value
                    ?: "" // Cannot verify without trackerId

                if (trackerId.isBlank()) {
                    platformLog("❌ [verifyRedemptionOtp] No tokenTrackerId found")
                    _otpVerificationResult.value = Resource.Error("Session expired. Please request OTP again.")
                    return@launch
                }

                val request = RedemptionOtpVerifyRequestDto(
                    id = trackerId,
                    userId = userId,
                    phoneNumber = phoneNumber,
                    otp = otp
                )

                redemptionRepository.verifyRedemptionOtp(request).collectLatest { result ->
                    when (result) {
                        is Resource.Success -> {
                            platformLog("✅ [verifyRedemptionOtp] OTP verified successfully")
                            _otpVerificationResult.value = Resource.Success("OTP verified successfully")
                        }
                        is Resource.Error -> {
                            platformLog("❌ [verifyRedemptionOtp] OTP verification failed: ${result.message}")
                            val errorMsg = sanitizeErrorMessage(result.message ?: "", "Error occurred. Please try again after some time")
                            _otpVerificationResult.value = Resource.Error(errorMsg)
                        }
                        is Resource.Loading -> { }
                    }
                }
            } catch (e: Exception) {
                platformLog("❌ [verifyRedemptionOtp] Error: ${e.message}")
                _otpVerificationResult.value = Resource.Error("Something went wrong. Please try again.")
            }
        }
    }

    private fun sanitizeErrorMessage(rawMessage: String, fallback: String): String {
        if (rawMessage.isBlank()) return fallback
        
        val friendly = rawMessage.toUserFriendlyErrorMessage()
        // If the global utility converted it into a generic network or server error, use that
        if (friendly != rawMessage.trim()) {
            return friendly
        }
        
        val lowerMsg = rawMessage.lowercase()
        return when {
            lowerMsg.contains("incorrect otp") -> "Incorrect OTP. Please try again."
            lowerMsg.contains("maximum attempts") -> "Maximum attempts reached. Please try again later."
            lowerMsg.contains("no units available") -> "You have no units available to withdraw."
            lowerMsg.contains("instant redemption range") || lowerMsg.contains("min_instant_redemption_amount") -> "The withdrawal amount is outside the allowed instant redemption range."
            lowerMsg.contains("greater than the min withdrawal amount") -> "Amount should be greater than the minimum withdrawal amount."
            lowerMsg.contains("less than the max redeemable amount") -> "Amount should be less than the maximum redeemable amount."
            else -> fallback
        }
    }
}
