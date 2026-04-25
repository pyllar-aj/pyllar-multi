package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyllar.consumer.data.remote.model.AdditionalKycRequest
import com.pyllar.consumer.domain.repository.CommonRepository
import com.pyllar.consumer.domain.repository.OnboardingRepository
import com.pyllar.consumer.domain.storage.SessionStore
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.util.platformLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdditionalKycViewModel(
    private val onboardingRepository: OnboardingRepository,
    private val commonRepository: CommonRepository,
    private val sessionStore: SessionStore
) : ViewModel() {

    private val _submitResult = MutableStateFlow<String?>(null)
    val submitResult: StateFlow<String?> = _submitResult.asStateFlow()
    private val _nextScreen = MutableStateFlow<String?>(null)
    val nextScreen: StateFlow<String?> = _nextScreen.asStateFlow()

    private val _prefillData = MutableStateFlow<Map<String, String?>>(emptyMap())
    val prefillData: StateFlow<Map<String, String?>> = _prefillData.asStateFlow()

    private val _isLoadingScreenData = MutableStateFlow<Boolean>(true)
    val isLoadingScreenData: StateFlow<Boolean> = _isLoadingScreenData.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)

    init {
        fetchPrepopulationData()
    }

    private fun fetchPrepopulationData() {
        viewModelScope.launch {
            _isLoadingScreenData.value = true
            platformLog("AdditionalKycViewModel: \uD83D\uDD0D [fetchPrepopulationData] Fetching screen data")
            try {
                commonRepository.fetchScreenData("AdditionalKyc").collect { result ->
                    if (result is Resource.Success) {
                        val dataMap = result.data?.data
                        if (dataMap != null) {
                            val stringMap = dataMap.mapValues { it.value?.toString() }
                            _prefillData.value = stringMap
                            platformLog("AdditionalKycViewModel: \u2705 [fetchPrepopulationData] Received data")
                        }
                    } else if (result is Resource.Error) {
                        platformLog("AdditionalKycViewModel: \u274C [fetchPrepopulationData] Failed: ${result.message}")
                    }
                    if (result !is Resource.Loading) {
                        _isLoadingScreenData.value = false
                    }
                }
            } catch (e: Exception) {
                platformLog("AdditionalKycViewModel: \u274C [fetchPrepopulationData] Exception: ${e.message}")
                _isLoadingScreenData.value = false
            }
        }
    }

    fun submitAdditionalKyc(
        kycAttemptId: String,
        token: String,
        maritalStatus: String,
        occupationType: String,
        fatherName: String,
        annualIncome: String,
        isPoliticallyExposed: Boolean,
        nationalityCountry: String,
        placeOfBirth: String,
        gender: String,
        addressLine1: String,
        addressLine2: String,
        addressLine3: String,
        city: String,
        pincode: String,
        longitude: Double?,
        latitude: Double?
    ) {
        if (_isSubmitting.value) return
        _isSubmitting.value = true
        platformLog("AdditionalKycViewModel: \uD83D\uDD0D [submitAdditionalKyc] Called for kycAttemptId: $kycAttemptId")

        viewModelScope.launch {
            try {
                _submitResult.value = null
                val geolocation = if (longitude != null && latitude != null) "$latitude,$longitude" else null
                val request = AdditionalKycRequest(
                    maritalStatus = maritalStatus,
                    occupationType = occupationType,
                    fatherName = fatherName,
                    annualIncome = annualIncome,
                    isPoliticallyExposed = isPoliticallyExposed,
                    nationalityCountry = nationalityCountry,
                    placeOfBirth = placeOfBirth,
                    gender = gender,
                    city = city,
                    pincode = pincode,
                    addressLine1 = addressLine1.ifBlank { null },
                    addressLine2 = addressLine2.ifBlank { null },
                    addressLine3 = addressLine3.ifBlank { null },
                    geolocation = geolocation
                )
                
                onboardingRepository.updateAdditionalKyc(kycAttemptId, request).collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            val data = result.data
                            if (data != null) {
                                try {
                                    sessionStore.saveValue("additional_kyc_status", data.status)
                                    platformLog("AdditionalKycViewModel: \u2705 [submitAdditionalKyc] Saved successfully")
                                    
                                    val nextScr = result.navigation?.nextScreen
                                    _nextScreen.value = nextScr
                                    _submitResult.value = "KYC details updated successfully"
                                } catch (e: Exception) {
                                    platformLog("AdditionalKycViewModel: \u274C Failed to save data: ${e.message}")
                                    _submitResult.value = "Failed to save KYC data"
                                    _nextScreen.value = null
                                }
                            } else {
                                platformLog("AdditionalKycViewModel: \u26A0\uFE0F Empty response data")
                                _submitResult.value = "Empty response data"
                                _nextScreen.value = null
                            }
                        }
                        is Resource.Error -> {
                            platformLog("AdditionalKycViewModel: \u274C Error: ${result.message}")
                            _submitResult.value = "Failed: ${result.message}"
                            _nextScreen.value = null
                        }
                        is Resource.Loading -> {
                            // loading state if needed
                        }
                    }
                }
            } catch (e: Exception) {
                platformLog("AdditionalKycViewModel: \u274C [submitAdditionalKyc] Exception: ${e.message}")
                _submitResult.value = "NETWORK_ERROR"
            } finally {
                _isSubmitting.value = false
            }
        }
    }
}
