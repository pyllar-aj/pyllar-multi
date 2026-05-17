package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyllar.consumer.domain.repository.CommonRepository
import com.pyllar.consumer.domain.repository.OnboardingRepository
import com.pyllar.consumer.domain.storage.SessionStore
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.util.platformLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class BankDetailsViewModel(
    private val onboardingRepository: OnboardingRepository,
    private val commonRepository: CommonRepository,
    private val sessionStore: SessionStore
) : ViewModel() {

    private val _prefillData = MutableStateFlow<Map<String, String?>>(emptyMap())
    val prefillData: StateFlow<Map<String, String?>> = _prefillData.asStateFlow()

    private val _submitResult = MutableStateFlow<Resource<com.pyllar.consumer.data.remote.model.dto.BankDetailsResponseDto>?>(null)
    val submitResult: StateFlow<Resource<com.pyllar.consumer.data.remote.model.dto.BankDetailsResponseDto>?> = _submitResult.asStateFlow()

    private val _initiateResult = MutableStateFlow<Resource<com.pyllar.consumer.data.remote.model.dto.VerificationInitiateResponseDto>?>(null)
    val initiateResult: StateFlow<Resource<com.pyllar.consumer.data.remote.model.dto.VerificationInitiateResponseDto>?> = _initiateResult.asStateFlow()

    private val _statusResult = MutableStateFlow<Resource<com.pyllar.consumer.data.remote.model.dto.VerificationStatusResponseDto>?>(null)
    val statusResult: StateFlow<Resource<com.pyllar.consumer.data.remote.model.dto.VerificationStatusResponseDto>?> = _statusResult.asStateFlow()

    init {
        fetchPrepopulationData()
    }

    private fun fetchPrepopulationData() {
        viewModelScope.launch {
            platformLog("BankDetailsViewModel: 🔍 Fetching screen data")
            try {
                commonRepository.fetchScreenData("BankDetails").collect { result ->
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
                            platformLog("BankDetailsViewModel: ✅ Received data")
                        }
                    }
                }
            } catch (e: Exception) {
                platformLog("BankDetailsViewModel: ❌ Exception: ${e.message}")
            }
        }
    }

    fun submitBankDetails(
        userId: String,
        accountNumber: String,
        ifscCode: String
    ) {
        viewModelScope.launch {
            _submitResult.value = Resource.Loading()
            onboardingRepository.submitBankDetails(userId, accountNumber, ifscCode, "savings").collectLatest { result ->
                if (result is Resource.Success) {
                    val investorId = result.data?.investorId
                    if (!investorId.isNullOrBlank()) {
                        sessionStore.saveValue("investor_id", investorId)
                    }
                }
                _submitResult.value = result
            }
        }
    }

    fun initiateBankVerification(userId: String) {
        viewModelScope.launch {
            _initiateResult.value = Resource.Loading()
            val name = sessionStore.getCurrentFullName().ifBlank { "User" }
            onboardingRepository.initiateBankVerification(userId, name).collectLatest { result ->
                _initiateResult.value = result
            }
        }
    }

    fun pollVerificationStatus(userId: String, verificationId: String) {
        viewModelScope.launch {
            _statusResult.value = Resource.Loading()
            onboardingRepository.getVerificationStatus(verificationId, userId).collectLatest { result ->
                _statusResult.value = result
            }
        }
    }
    
    fun clearResults() {
        _submitResult.value = null
        _initiateResult.value = null
        _statusResult.value = null
    }
}
