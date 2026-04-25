package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyllar.consumer.data.remote.model.MinimalKycRequest
import com.pyllar.consumer.data.remote.model.MinimalKycResponse
import com.pyllar.consumer.data.remote.model.Mobile
import com.pyllar.consumer.domain.repository.CommonRepository
import com.pyllar.consumer.domain.repository.OnboardingRepository
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.util.platformLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MinDetailsViewModel(
    private val onboardingRepository: OnboardingRepository,
    private val commonRepository: CommonRepository
) : ViewModel() {
    
    private val _minDetailsState = MutableStateFlow<Resource<MinimalKycResponse>?>(null)
    val minDetailsState: StateFlow<Resource<MinimalKycResponse>?> = _minDetailsState.asStateFlow()

    private val _prefillData = MutableStateFlow<Map<String, String?>>(emptyMap())
    val prefillData = _prefillData.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)

    init {
        fetchPrepopulatedData()
    }

    private fun fetchPrepopulatedData() {
        viewModelScope.launch {
            commonRepository.fetchScreenData("MinDetails").collect { result ->
                if (result is Resource.Success) {
                    val dataMap = result.data?.data
                    if (dataMap != null) {
                        // Assuming ScreenDataResponseDto.data is Map<String, String> or Map<String, Any>
                        // Let's coerce to string map for UI prepopulation
                        val stringMap = dataMap.mapValues { it.value?.toString() }
                        _prefillData.value = stringMap
                    }
                }
            }
        }
    }

    fun submitMinimalDetails(
        userId: String,
        name: String,
        panNumber: String,
        dateOfBirth: String,
        emailAddress: String,
        mobileCountryCode: String,
        mobileNumber: String,
        token: String,
        preVerificationId: String? = null
    ) {
        if (_isSubmitting.value) return
        _isSubmitting.value = true
        viewModelScope.launch {
            try {
                platformLog("MinDetailsViewModel: Starting minimal details submission for pre-verified user, preVerificationId: $preVerificationId")

                _minDetailsState.value = Resource.Loading()

                val request = MinimalKycRequest(
                    userId = userId,
                    name = name,
                    panNumber = panNumber,
                    dateOfBirth = dateOfBirth,
                    emailAddress = emailAddress,
                    mobile = Mobile(countryCode = mobileCountryCode, number = mobileNumber),
                    preVerificationId = preVerificationId
                )

                onboardingRepository.createMinimalDetails(request).collect { result ->
                    platformLog("MinDetailsViewModel: MinDetails API result: $result")
                    _minDetailsState.value = result
                }
            } catch (e: Exception) {
                platformLog("MinDetailsViewModel: Error submitting minimal details - ${e.message}")
                _minDetailsState.value = Resource.Error("Failed to submit details: ${e.message}")
            } finally {
                _isSubmitting.value = false
            }
        }
    }
    
    fun clearState() {
        _minDetailsState.value = null
    }
}
