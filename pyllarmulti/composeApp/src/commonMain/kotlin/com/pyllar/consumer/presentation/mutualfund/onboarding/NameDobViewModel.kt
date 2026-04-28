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

import com.pyllar.consumer.domain.storage.SessionStore

class NameDobViewModel(
    private val onboardingRepository: OnboardingRepository,
    private val commonRepository: CommonRepository,
    private val sessionStore: SessionStore
) : ViewModel() {
    private val _kycResult = MutableStateFlow<Resource<MinimalKycResponse>?>(null)
    val kycResult: StateFlow<Resource<MinimalKycResponse>?> = _kycResult.asStateFlow()

    private val _prefillData = MutableStateFlow<Map<String, String?>>(emptyMap())
    val prefillData = _prefillData.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)

    init {
        fetchPrepopulatedData()
    }

    private fun fetchPrepopulatedData() {
        viewModelScope.launch {
            commonRepository.fetchScreenData("NameDob").collect { result ->
                if (result is Resource.Success) {
                    val dataMap = result.data?.data
                    if (dataMap != null) {
                        val stringMap = dataMap.mapValues { 
                            val element = it.value
                            if (element is kotlinx.serialization.json.JsonPrimitive && element.isString) {
                                element.content
                            } else {
                                element?.toString()
                            }
                        }
                        _prefillData.value = stringMap
                    }
                }
            }
        }
    }

    fun createMinimalKyc(
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
        if (preVerificationId == null && _isSubmitting.value) return
        if (preVerificationId == null) _isSubmitting.value = true

        if (preVerificationId == null) {
            _kycResult.value = Resource.Loading()
            platformLog("NameDobViewModel: createMinimalKyc: Loading")
        } else {
             platformLog("NameDobViewModel: createMinimalKyc: Polling with preVerificationId=$preVerificationId")
        }

        viewModelScope.launch {
            try {
                if (token.isNotBlank()) {
                    platformLog("NameDobViewModel: Saving token to sessionStore: ${token.take(10)}...")
                    sessionStore.saveToken(token)
                }
                if (userId.isNotBlank() && userId != "anonymous") {
                    platformLog("NameDobViewModel: Saving userId to sessionStore: $userId")
                    sessionStore.saveUserId(userId)
                }
                
                val request = MinimalKycRequest(
                    userId = userId,
                    name = name,
                    panNumber = panNumber,
                    dateOfBirth = dateOfBirth,
                    emailAddress = emailAddress,
                    mobile = Mobile(countryCode = mobileCountryCode, number = mobileNumber),
                    preVerificationId = preVerificationId
                )

                onboardingRepository.createMinimalKyc(request).collect {
                    platformLog("NameDobViewModel: createMinimalKyc: Resource=$it")
                    when (it) {
                        is Resource.Success -> platformLog("NameDobViewModel: createMinimalKyc: Success data=${it.data}")
                        is Resource.Error -> platformLog("NameDobViewModel: createMinimalKyc: Error message=${it.message}")
                        is Resource.Loading -> platformLog("NameDobViewModel: createMinimalKyc: Loading state")
                    }
                    _kycResult.value = it
                }
            } finally {
                if (preVerificationId == null) _isSubmitting.value = false
            }
        }
    }
}
