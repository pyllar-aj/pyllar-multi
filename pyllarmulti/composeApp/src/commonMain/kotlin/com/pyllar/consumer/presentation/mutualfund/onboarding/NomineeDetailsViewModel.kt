package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyllar.consumer.data.remote.model.dto.NavigationAction
import com.pyllar.consumer.data.remote.model.dto.NavigationInfo
import com.pyllar.consumer.data.remote.requests.CreateNomineeRequest
import com.pyllar.consumer.data.remote.requests.CreateNomineeRequestV2
import com.pyllar.consumer.data.remote.requests.NomineeDetailsRequest
import com.pyllar.consumer.data.remote.model.dto.RedemptionOtpVerifyRequestDto
import com.pyllar.consumer.domain.repository.MutualFundRepository
import com.pyllar.consumer.domain.repository.OnboardingRepository
import com.pyllar.consumer.domain.storage.SessionStore
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.util.platformLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull

data class NomineeInfo(
    val name: String,
    val relationship: String,
    val dateOfBirth: String,
    val panNumber: String
)

class NomineeDetailsViewModel(
    private val onboardingRepository: OnboardingRepository,
    private val mutualFundRepository: MutualFundRepository,
    private val sessionStore: SessionStore
) : ViewModel() {

    private val _nomineeSubmissionResult = MutableStateFlow<Resource<JsonObject>?>(null)
    val nomineeSubmissionResult: StateFlow<Resource<JsonObject>?> = _nomineeSubmissionResult.asStateFlow()

    private val _navigationInfo = MutableStateFlow<NavigationInfo?>(null)
    val navigationInfo: StateFlow<NavigationInfo?> = _navigationInfo.asStateFlow()

    private val _otpVerificationResult = MutableStateFlow<Resource<String>?>(null)
    val otpVerificationResult: StateFlow<Resource<String>?> = _otpVerificationResult.asStateFlow()

    private val _otpGenerationResult = MutableStateFlow<Resource<String>?>(null)
    val otpGenerationResult: StateFlow<Resource<String>?> = _otpGenerationResult.asStateFlow()

    private val _tokenTrackerId = MutableStateFlow<String?>(null)
    val tokenTrackerId: StateFlow<String?> = _tokenTrackerId.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)

    init {
        viewModelScope.launch {
            try {
                val storedId = sessionStore.getValue("consent_token_tracker_id")
                if (!storedId.isNullOrBlank()) {
                    _tokenTrackerId.value = storedId
                    platformLog("NomineeDetailsViewModel: \u2139\uFE0F Loaded stored consent tokenTrackerId: $storedId")
                }
            } catch (e: Exception) {
                platformLog("NomineeDetailsViewModel: \u26A0\uFE0F Failed to load stored consent tokenTrackerId: ${e.message}")
            }
        }
    }

    fun submitNomineeDetails(
        userId: String,
        kycAttemptId: String,
        investorId: String,
        wantsToAddNominee: Boolean,
        nomineeName: String? = null,
        nomineeRelationship: String? = null,
        nomineeDateOfBirth: String? = null,
        nomineePanNumber: String? = null
    ) {
        platformLog("NomineeDetailsViewModel: \uD83D\uDE80 submitNomineeDetails called!")
        viewModelScope.launch {
            _nomineeSubmissionResult.value = Resource.Loading()
            
            try {
                val effectiveKycAttemptId = if (kycAttemptId.isBlank()) {
                    sessionStore.getValue(com.pyllar.consumer.data.local.KeyValueConstants.KYC_ATTEMPT_ID) ?: ""
                } else kycAttemptId

                val effectiveInvestorId = if (investorId.isBlank()) {
                    sessionStore.getValue(com.pyllar.consumer.data.local.KeyValueConstants.INVESTOR_ID) ?: ""
                } else investorId

                platformLog("NomineeDetailsViewModel: Using kycAttemptId=$effectiveKycAttemptId, investorId=$effectiveInvestorId")

                val request = CreateNomineeRequest(
                    userId = userId,
                    kycAttemptId = effectiveKycAttemptId,
                    investorId = effectiveInvestorId,
                    wantsToAddNominee = wantsToAddNominee,
                    nomineeName = nomineeName,
                    nomineeRelationship = nomineeRelationship,
                    nomineeDateOfBirth = nomineeDateOfBirth,
                    nomineePanNumber = nomineePanNumber
                )
                
                onboardingRepository.submitNomineeDetails(request).collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            val data = result.data
                            try {
                                sessionStore.saveValue("nominee_submitted", "true")
                                platformLog("NomineeDetailsViewModel: \u2705 [submitNomineeDetails] Nominee data saved successfully")
                                
                                val trackerId = data?.get("tokenTrackerId")?.toString()?.trim('"')
                                if (!trackerId.isNullOrBlank()) {
                                    _tokenTrackerId.value = trackerId
                                    sessionStore.saveValue("consent_token_tracker_id", trackerId)
                                    platformLog("NomineeDetailsViewModel: \u2705 Stored tokenTrackerId: $trackerId")
                                } else {
                                    platformLog("NomineeDetailsViewModel: \u26A0\uFE0F No tokenTrackerId in response")
                                }
                            } catch (e: Exception) {
                                platformLog("NomineeDetailsViewModel: \u274C Error saving data: ${e.message}")
                            }
                            
                            _nomineeSubmissionResult.value = result
                            _navigationInfo.value = result.navigation
                        }
                        is Resource.Error -> {
                            _nomineeSubmissionResult.value = result
                        }
                        is Resource.Loading -> {
                            _nomineeSubmissionResult.value = result
                        }
                    }
                }
            } catch (e: Exception) {
                _nomineeSubmissionResult.value = Resource.Error(e.message ?: "Network error occurred")
            }
        }
    }

    fun submitNomineeDetailsV2(
        userId: String,
        kycAttemptId: String,
        investorId: String,
        wantsToAddNominee: Boolean,
        nominees: List<NomineeInfo>
    ) {
        if (_isSubmitting.value) return
        _isSubmitting.value = true
        platformLog("NomineeDetailsViewModel: \uD83D\uDE80 submitNomineeDetailsV2 called with ${nominees.size} nominees!")
        viewModelScope.launch {
            try {
                _nomineeSubmissionResult.value = Resource.Loading()
                
                val effectiveKycAttemptId = if (kycAttemptId.isBlank()) {
                    sessionStore.getValue(com.pyllar.consumer.data.local.KeyValueConstants.KYC_ATTEMPT_ID) ?: ""
                } else kycAttemptId

                val effectiveInvestorId = if (investorId.isBlank()) {
                    sessionStore.getValue(com.pyllar.consumer.data.local.KeyValueConstants.INVESTOR_ID) ?: ""
                } else investorId

                platformLog("NomineeDetailsViewModelV2: Using kycAttemptId=$effectiveKycAttemptId, investorId=$effectiveInvestorId")

                val nomineeDetailsList = if (wantsToAddNominee && nominees.isNotEmpty()) {
                    nominees.mapNotNull { nominee ->
                        if (nominee.name.isNotBlank()) {
                            NomineeDetailsRequest(
                                nomineeName = nominee.name.takeIf { it.isNotBlank() },
                                nomineeRelationship = nominee.relationship.takeIf { it.isNotBlank() },
                                nomineeDateOfBirth = nominee.dateOfBirth.takeIf { it.isNotBlank() },
                                nomineePanNumber = nominee.panNumber.takeIf { it.isNotBlank() },
                                percentage = null
                            )
                        } else {
                            null
                        }
                    }
                } else {
                    emptyList()
                }
                
                val request = CreateNomineeRequestV2(
                    userId = userId,
                    kycAttemptId = effectiveKycAttemptId,
                    investorId = effectiveInvestorId,
                    wantsToAddNominee = wantsToAddNominee,
                    nomineeDetails = if (wantsToAddNominee) nomineeDetailsList else null
                )
                
                onboardingRepository.submitNomineeDetailsV2(request).collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            try {
                                val data = result.data
                                sessionStore.saveValue("nominee_submitted", "true")
                                platformLog("NomineeDetailsViewModel: \u2705 [submitNomineeDetailsV2] Nominee data saved successfully")
                                
                                // Extract trackerId from either navigation params or data object
                                val trackerId = result.navigation?.getParam("trackerId") 
                                    ?: data?.get("tokenTrackerId")?.jsonPrimitive?.content
                                    ?: data?.get("tokenTrackerId")?.toString()?.trim('"')

                                if (trackerId != null) {
                                    _tokenTrackerId.value = trackerId
                                    sessionStore.saveValue("consent_token_tracker_id", trackerId)
                                    platformLog("NomineeDetailsViewModel: \u2705 Stored trackerId: $trackerId")
                                } else {
                                    platformLog("NomineeDetailsViewModel: No trackerId found in response")
                                }
                            } catch (e: Exception) {
                                platformLog("NomineeDetailsViewModel: \u274C Error saving data: ${e.message}")
                            }
                            
                            _nomineeSubmissionResult.value = result
                            _navigationInfo.value = result.navigation
                        }
                        is Resource.Error -> {
                            _nomineeSubmissionResult.value = result
                        }
                        is Resource.Loading -> {
                            _nomineeSubmissionResult.value = result
                        }
                    }
                }
            } catch (e: Exception) {
                _nomineeSubmissionResult.value = Resource.Error(e.message ?: "Network error occurred")
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun clearResult() {
        _nomineeSubmissionResult.value = null
        _navigationInfo.value = null
    }

    fun verifyOtp(phoneNumber: String, otp: String) {
        viewModelScope.launch {
            _otpVerificationResult.value = Resource.Loading()
            try {
                val userId = sessionStore.getCurrentUserId()
                if (userId.isBlank()) {
                    _otpVerificationResult.value = Resource.Error("User information not available")
                    return@launch
                }

                val sanitizedPhone = sanitizePhoneNumber(phoneNumber)
                if (sanitizedPhone.isBlank()) {
                    _otpVerificationResult.value = Resource.Error("Phone number not available")
                    return@launch
                }

                val trackerId = _tokenTrackerId.value?.takeIf { it.isNotBlank() }
                    ?: sessionStore.getValue("consent_token_tracker_id")?.also {
                        _tokenTrackerId.value = it
                    }
                
                if (trackerId.isNullOrBlank()) {
                    platformLog("NomineeDetailsViewModel: \u26A0\uFE0F verifyOtp - No tokenTrackerId available")
                    _otpVerificationResult.value = Resource.Error("OTP session expired. Please request a new OTP.")
                    return@launch
                }

                platformLog("NomineeDetailsViewModel: \uD83D\uDCE1 [API-REQ] verifyConsentOtp - userId=$userId, phone=$sanitizedPhone, trackerId=$trackerId, otp=[MASKED]")

                val request = RedemptionOtpVerifyRequestDto(
                    id = trackerId,
                    userId = userId,
                    phoneNumber = sanitizedPhone,
                    otp = otp
                )

                mutualFundRepository.verifyConsentOtp(request).collect { result ->
                    _navigationInfo.value = result.navigation
                    when (result) {
                        is Resource.Success -> {
                            val message = "OTP verified successfully"
                            _otpVerificationResult.value = Resource.Success(message, result.navigation, result.fieldErrors)
                        }
                        is Resource.Error -> {
                            val message = result.message ?: "Incorrect OTP. Please try again."
                            _otpVerificationResult.value = Resource.Error(message, result.navigation, result.fieldErrors, result.errorType)
                        }
                        is Resource.Loading -> {
                            _otpVerificationResult.value = Resource.Loading()
                        }
                    }
                }
            } catch (e: Exception) {
                _otpVerificationResult.value = Resource.Error("Something went wrong. Please try again.")
            }
        }
    }

    fun generateOtp(phoneNumber: String) {
        viewModelScope.launch {
            _otpGenerationResult.value = Resource.Loading()
            _otpVerificationResult.value = null // Clear previous verification error/result
            try {
                val userId = sessionStore.getCurrentUserId()
                if (userId.isBlank()) {
                    _otpGenerationResult.value = Resource.Error("User information not available")
                    return@launch
                }

                val sanitizedPhone = sanitizePhoneNumber(phoneNumber)
                if (sanitizedPhone.isBlank()) {
                    _otpGenerationResult.value = Resource.Error("Phone number not available")
                    return@launch
                }

                platformLog("NomineeDetailsViewModel: \uD83D\uDCE1 [API-REQ] sendConsentOtp - userId=$userId, phone=$sanitizedPhone")

                mutualFundRepository.sendConsentOtp(userId, sanitizedPhone).collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            val trackerId = result.data?.get("tokenTrackerId")?.toString()?.trim('"')
                            if (!trackerId.isNullOrBlank()) {
                                _tokenTrackerId.value = trackerId
                                sessionStore.saveValue("consent_token_tracker_id", trackerId)
                            }

                            val message = "OTP sent successfully"
                            _otpGenerationResult.value = Resource.Success(message)
                        }
                        is Resource.Error -> {
                            _otpGenerationResult.value = Resource.Error(result.message ?: "Failed to send OTP. Please try again.")
                        }
                        is Resource.Loading -> {
                            _otpGenerationResult.value = Resource.Loading()
                        }
                    }
                }
            } catch (e: Exception) {
                _otpGenerationResult.value = Resource.Error("NETWORK_ERROR")
            }
        }
    }

    private fun sanitizePhoneNumber(phoneNumber: String): String {
        val digitsOnly = phoneNumber.filter { it.isDigit() }
        if (digitsOnly.length < 10) {
            return ""
        }
        return if (digitsOnly.length > 10) {
            digitsOnly.takeLast(10)
        } else {
            digitsOnly
        }
    }
}
