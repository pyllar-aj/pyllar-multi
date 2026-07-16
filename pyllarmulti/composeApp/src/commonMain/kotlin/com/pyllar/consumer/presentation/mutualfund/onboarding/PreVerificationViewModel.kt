package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.domain.repository.PreVerificationRepository
import com.pyllar.consumer.data.remote.dto.PreVerificationResponseDto
import com.pyllar.consumer.domain.repository.CommonRepository
import com.pyllar.consumer.domain.storage.SessionStore
import com.pyllar.consumer.data.local.KeyValueConstants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import com.pyllar.consumer.util.platformLog
import com.pyllar.consumer.util.toUserFriendlyErrorMessage

enum class VerificationStatus {
    IDLE,
    IN_PROGRESS,
    SUCCESS,
    MANUAL_AVAILABLE,
    MANUAL_IN_PROGRESS,
    PERMANENTLY_FAILED,
    UNKNOWN_FAILURE
}

data class PreVerificationUiState(
    val verificationResult: Resource<PreVerificationResponseDto>? = null,
    val errorMessage: String? = null,
    val isManualVerificationAvailable: Boolean = false,
    val verificationStatus: VerificationStatus = VerificationStatus.IDLE,
    val nextScreen: String? = null,
    val serverMessage: String? = null,
    val prepopulatedData: Map<String, String?> = emptyMap(),
    val panFetchResult: Resource<com.pyllar.consumer.data.remote.dto.PanFetchDataDto>? = null,
    val panVerifyOtpResult: Resource<com.pyllar.consumer.data.remote.dto.PanVerifyOtpDataDto>? = null
)

class PreVerificationViewModel(
    private val preVerificationRepository: PreVerificationRepository,
    private val commonRepository: CommonRepository,
    private val sessionStore: SessionStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(PreVerificationUiState())
    val uiState: StateFlow<PreVerificationUiState> = _uiState.asStateFlow()

    private val _isCheckingReadiness = MutableStateFlow(false)

    private var pollingJob: Job? = null
    private var currentPreVerificationId: String? = null

    init {
        fetchPrepopulatedData()
    }

    private fun fetchPrepopulatedData() {
        viewModelScope.launch {
            platformLog("PreVerificationVM: 🔍 [fetchPrepopulatedData] Fetching screen data")
            try {
                commonRepository.fetchScreenData("PreVerification").collect { result ->
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
                            platformLog("PreVerificationVM: ✅ Received data")
                            
                            // Try to retrieve and persist redirect_url to ensure it is populated
                            val redirectUrl = stringMap["redirectUrl"] ?: stringMap["redirect_url"]
                            if (!redirectUrl.isNullOrBlank()) {
                                platformLog("PreVerificationVM: 💾 Found redirectUrl in prepopulated data: $redirectUrl. Saving to sessionStore.")
                                sessionStore.saveValue(KeyValueConstants.RE_URL, redirectUrl)
                            }
                            
                            val navigation = result.navigation
                            val nextScreen = if (navigation?.shouldNavigate() == true) navigation.nextScreen else null
                            platformLog("PreVerificationVM: Navigation resolution - nextScreen='$nextScreen', action='${navigation?.action}'")
                            
                            _uiState.value = _uiState.value.copy(
                                prepopulatedData = stringMap,
                                nextScreen = nextScreen
                            )
                        } else {
                            val navigation = result.navigation
                            val nextScreen = if (navigation?.shouldNavigate() == true) navigation.nextScreen else null
                            if (nextScreen != null) {
                                platformLog("PreVerificationVM: ✅ Received navigation with null dataMap - nextScreen='$nextScreen'")
                                _uiState.value = _uiState.value.copy(nextScreen = nextScreen)
                            }
                        }
                    } else if (result is Resource.Error) {
                        platformLog("PreVerificationVM: ❌ Failed to fetch data: ${result.message}")
                    }
                }
            } catch (e: Exception) {
                platformLog("PreVerificationVM: ❌ Exception: ${e.message}")
            }
        }
    }

    fun checkInvestorReadiness(panNumber: String) {
        if (_isCheckingReadiness.value) return
        _isCheckingReadiness.value = true
        platformLog("PreVerificationVM: \uD83D\uDD0D Starting investor readiness check for PAN: ${panNumber.take(3)}***")

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    verificationResult = Resource.Loading(),
                    errorMessage = null,
                    isManualVerificationAvailable = false,
                    verificationStatus = VerificationStatus.IN_PROGRESS,
                    nextScreen = null,
                    serverMessage = null
                )

                preVerificationRepository.checkInvestorReadiness(panNumber).collectLatest { result ->
                    when (result) {
                        is Resource.Success -> {
                            platformLog("PreVerificationVM: \u2705 Readiness check response: ${result.data}")
                            sessionStore.saveValue(KeyValueConstants.PAN, panNumber)
                            val preVerificationId = result.data?.data?.id ?: result.data?.id
                            platformLog("PreVerificationVM: \uD83C\uDD94 ID received: $preVerificationId")
                            if (preVerificationId != null) {
                                currentPreVerificationId = preVerificationId
                                _uiState.value = _uiState.value.copy(verificationResult = result)
                                platformLog("PreVerificationVM: \uD83D\uDD04 Starting polling for readiness check completion: $preVerificationId")
                                startPolling(preVerificationId)
                            } else {
                                _uiState.value = _uiState.value.copy(
                                    verificationResult = Resource.Error("Invalid response - no verification ID"),
                                    errorMessage = "Invalid response from server".toUserFriendlyErrorMessage(),
                                    verificationStatus = VerificationStatus.UNKNOWN_FAILURE
                                )
                            }
                        }
                        is Resource.Error -> {
                            platformLog("PreVerificationVM: \u274C Readiness check failed: ${result.message}")
                            _uiState.value = _uiState.value.copy(
                                verificationResult = result,
                                errorMessage = result.message?.toUserFriendlyErrorMessage(),
                                verificationStatus = VerificationStatus.UNKNOWN_FAILURE
                            )
                        }
                        is Resource.Loading -> {
                        }
                    }
                }
            } catch (e: Exception) {
                platformLog("PreVerificationVM: \uD83D\uDCA5 Exception during readiness check: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    verificationResult = Resource.Error("Network error: ${e.message}"),
                    errorMessage = "Failed to check readiness. Please try again.",
                    verificationStatus = VerificationStatus.UNKNOWN_FAILURE
                )
            } finally {
                _isCheckingReadiness.value = false
            }
        }
    }

    fun performPreVerification(
        panNumber: String,
        name: String,
        accountNumber: String,
        ifscCode: String
    ) {
        platformLog("PreVerificationVM: Starting pre-verification for PAN: ${maskPan(panNumber)}")
        
        stopPolling()
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                verificationResult = Resource.Loading(),
                errorMessage = null,
                isManualVerificationAvailable = false,
                verificationStatus = VerificationStatus.IN_PROGRESS
            )
            
            try {
                preVerificationRepository.startAutomaticVerification(
                    panNumber = panNumber,
                    name = name,
                    accountNumber = accountNumber,
                    ifscCode = ifscCode,
                    accountType = "savings"
                ).collectLatest { result ->
                    when (result) {
                        is Resource.Success -> {
                            sessionStore.saveValue(KeyValueConstants.PAN, panNumber)
                            val preVerificationId = result.data?.data?.id
                            if (preVerificationId != null) {
                                currentPreVerificationId = preVerificationId
                                platformLog("PreVerificationVM: Pre-verification started with ID: $preVerificationId")
                                
                                val navigation = result.data.navigation
                                when {
                                    navigation?.shouldNavigate() == true -> {
                                        platformLog("PreVerificationVM: \uD83D\uDE80 Server says navigate immediately")
                                        handleVerificationSuccess(result.data)
                                    }
                                    navigation?.shouldStay() == true -> {
                                        platformLog("PreVerificationVM: \u23F8\uFE0F Server says stay - no polling needed")
                                        handleVerificationSuccess(result.data)
                                    }
                                    navigation?.shouldPoll() == true -> {
                                        platformLog("PreVerificationVM: \uD83D\uDD04 Server says start polling")
                                        startPolling(preVerificationId)
                                    }
                                    result.data.data.isCompleted() -> {
                                        platformLog("PreVerificationVM: Verification already completed")
                                        handleVerificationSuccess(result.data)
                                    }
                                    else -> {
                                        platformLog("PreVerificationVM: No server navigation instruction - starting polling")
                                        startPolling(preVerificationId)
                                    }
                                }
                            } else {
                                _uiState.value = _uiState.value.copy(
                                    verificationResult = Resource.Error("Invalid response - no verification ID"),
                                    errorMessage = "Invalid response from server",
                                    verificationStatus = VerificationStatus.UNKNOWN_FAILURE
                                )
                            }
                        }
                        is Resource.Error -> {
                            platformLog("PreVerificationVM: Pre-verification failed: ${result.message}")
                            _uiState.value = _uiState.value.copy(
                                verificationResult = Resource.Error(result.message ?: "Unknown error"),
                                errorMessage = result.message?.toUserFriendlyErrorMessage(),
                                verificationStatus = VerificationStatus.UNKNOWN_FAILURE
                            )
                        }
                        is Resource.Loading -> {
                        }
                    }
                }
                
            } catch (e: Exception) {
                platformLog("PreVerificationVM: Pre-verification exception: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    verificationResult = Resource.Error(e.message ?: "Unknown error"),
                    errorMessage = getErrorMessage(e),
                    verificationStatus = VerificationStatus.UNKNOWN_FAILURE
                )
            }
        }
    }

    private fun startPolling(preVerificationId: String) {
        platformLog("PreVerificationVM: Starting polling for pre-verification ID: $preVerificationId")
        
        pollingJob = viewModelScope.launch {
            preVerificationRepository.pollVerificationStatus(
                preVerificationId = preVerificationId,
                maxAttempts = 30,
                intervalSeconds = 10L
            ).collectLatest { result ->
                when (result) {
                    is Resource.Loading -> {
                        platformLog("PreVerificationVM: Polling in progress...")
                        _uiState.value = _uiState.value.copy(
                            verificationStatus = VerificationStatus.IN_PROGRESS
                        )
                    }
                    is Resource.Success -> {
                        platformLog("PreVerificationVM: \uD83D\uDCE9 Polling response received. Status: ${result.data?.status ?: result.data?.data?.status}")
                        val navigation = result.navigation
                        platformLog("PreVerificationVM: \uD83E\uDDF3 Navigation: ${navigation?.nextScreen ?: "None"} (Action: ${navigation?.action ?: "None"})")
                        
                        when {
                            navigation?.shouldNavigate() == true -> {
                                platformLog("PreVerificationVM: \uD83D\uDE80 Server says navigate during polling")
                                stopPolling()
                                handleVerificationSuccess(result.data, navigation)
                            }
                            navigation?.shouldPoll() == true -> {
                                platformLog("PreVerificationVM: \uD83D\uDD04 Server says continue polling")
                                _uiState.value = _uiState.value.copy(
                                    verificationStatus = VerificationStatus.IN_PROGRESS,
                                    serverMessage = navigation.getMessage()
                                )
                            }
                            navigation?.shouldStay() == true -> {
                                platformLog("PreVerificationVM: \u23F8\uFE0F Server says stay - stopping polling")
                                stopPolling()
                                handleVerificationSuccess(result.data, navigation)
                            }
                            result.data?.data?.isCompleted() == true -> {
                                platformLog("PreVerificationVM: \u2705 Verification completed")
                                stopPolling()
                                handleVerificationSuccess(result.data, navigation)
                            }
                            else -> {
                                platformLog("PreVerificationVM: \u23F3 Polling update - no server action, continuing")
                            }
                        }
                    }
                    is Resource.Error -> {
                        platformLog("PreVerificationVM: Polling error: ${result.message}")
                        stopPolling()
                        _uiState.value = _uiState.value.copy(
                            verificationResult = Resource.Error(result.message ?: "Polling failed"),
                            errorMessage = result.message?.toUserFriendlyErrorMessage(),
                            verificationStatus = VerificationStatus.UNKNOWN_FAILURE
                        )
                    }
                }
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
        platformLog("PreVerificationVM: Polling stopped")
    }

    private fun handleVerificationSuccess(
        response: PreVerificationResponseDto?,
        externalNavigation: com.pyllar.consumer.data.remote.model.dto.NavigationInfo? = null
    ) {
        val hasData = response?.data != null || response?.id != null
        if (!hasData) {
            _uiState.value = _uiState.value.copy(
                verificationResult = Resource.Error("Invalid response from server"),
                errorMessage = "Invalid response from server"
            )
            return
        }

        val data = response.data
        val navigation = externalNavigation ?: response?.navigation
        val status = data?.status ?: response?.status
        platformLog("PreVerificationVM: handleVerificationSuccess: nextScreen='${navigation?.nextScreen}', status='$status'")

        val verifiedAccounts = data?.getVerifiedBankAccounts() ?: emptyList()
        val manualVerificationAccounts = data?.getBankAccountsRequiringManualVerification() ?: emptyList()
        
        val nextScreen = if (navigation?.shouldNavigate() == true) {
            navigation.nextScreen
        } else {
            null
        }

        val isManualVerificationRequired = navigation?.requiresManualVerification() == true ||
                manualVerificationAccounts.isNotEmpty()

        when {
            navigation?.shouldNavigate() == true -> {
                platformLog("PreVerificationVM: \uD83D\uDE80 Server-driven navigation: ${navigation.nextScreen}")
                _uiState.value = _uiState.value.copy(
                    verificationResult = Resource.Success(response),
                    errorMessage = null,
                    isManualVerificationAvailable = false,
                    verificationStatus = VerificationStatus.SUCCESS,
                    nextScreen = nextScreen,
                    serverMessage = navigation.getMessage()
                )
            }
            navigation?.shouldPoll() == true -> {
                platformLog("PreVerificationVM: \uD83D\uDD04 Server says continue polling")
                _uiState.value = _uiState.value.copy(
                    verificationResult = Resource.Loading(),
                    errorMessage = null,
                    isManualVerificationAvailable = false,
                    verificationStatus = VerificationStatus.IN_PROGRESS,
                    nextScreen = null,
                    serverMessage = navigation.getMessage()
                )
            }
            navigation?.shouldStay() == true -> {
                platformLog("PreVerificationVM: \u23F8\uFE0F Server says stay on current screen")
                _uiState.value = _uiState.value.copy(
                    verificationResult = Resource.Success(response),
                    errorMessage = navigation.getMessage(),
                    isManualVerificationAvailable = isManualVerificationRequired,
                    verificationStatus = VerificationStatus.MANUAL_AVAILABLE,
                    nextScreen = null,
                    serverMessage = navigation.getMessage()
                )
            }
            verifiedAccounts.isNotEmpty() -> {
                platformLog("PreVerificationVM: \u2705 Pre-verification successful")
                _uiState.value = _uiState.value.copy(
                    verificationResult = Resource.Success(response),
                    errorMessage = null,
                    isManualVerificationAvailable = false,
                    verificationStatus = VerificationStatus.SUCCESS,
                    nextScreen = nextScreen,
                    serverMessage = navigation?.getMessage()
                )
            }
            isManualVerificationRequired -> {
                platformLog("PreVerificationVM: \uD83D\uDD04 Manual verification required")
                val message = navigation?.getMessage() ?: 
                    "Bank account verification failed. Manual verification with documents required."
                
                _uiState.value = _uiState.value.copy(
                    verificationResult = Resource.Success(response),
                    errorMessage = message,
                    isManualVerificationAvailable = true,
                    verificationStatus = VerificationStatus.MANUAL_AVAILABLE,
                    nextScreen = null,
                    serverMessage = message
                )
            }
            data?.isInProgress() == true -> {
                platformLog("PreVerificationVM: \u23F3 Verification in progress")
                _uiState.value = _uiState.value.copy(
                    verificationResult = Resource.Loading(),
                    errorMessage = null,
                    isManualVerificationAvailable = false,
                    verificationStatus = VerificationStatus.IN_PROGRESS,
                    nextScreen = null
                )
            }
            else -> {
                platformLog("PreVerificationVM: \u2753 No server navigation - using fallback logic")
                val fallbackScreen = determineFallbackScreen(data, response)
                _uiState.value = _uiState.value.copy(
                    verificationResult = Resource.Success(response),
                    errorMessage = "Verification completed but navigation unclear",
                    isManualVerificationAvailable = false,
                    verificationStatus = VerificationStatus.SUCCESS,
                    nextScreen = fallbackScreen,
                    serverMessage = "Using fallback navigation"
                )
            }
        }
    }

    private fun determineFallbackScreen(data: com.pyllar.consumer.data.remote.dto.PreVerificationDataDto?, response: PreVerificationResponseDto): String {
        val isInvestorReady = data?.isInvestorReadyToInvest() ?: (response.readiness?.status == "verified")
        val hasVerifiedBankAccount = data?.getVerifiedBankAccounts()?.isNotEmpty() ?: false
        
        return when {
            isInvestorReady && hasVerifiedBankAccount -> {
                "SipAmount"
            }
            isInvestorReady && !hasVerifiedBankAccount -> {
                "BankDetails"
            }
            else -> {
                "NameDob"
            }
        }
    }

    fun performManualVerification(bankAccountProof: String) {
        val currentState = _uiState.value
        if (!currentState.isManualVerificationAvailable) {
            platformLog("PreVerificationVM: Manual verification not available")
            return
        }

        platformLog("PreVerificationVM: Starting manual verification with proof")
        
        _uiState.value = _uiState.value.copy(
            verificationResult = Resource.Loading(),
            errorMessage = null,
            isManualVerificationAvailable = false,
            verificationStatus = VerificationStatus.MANUAL_IN_PROGRESS
        )
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    private fun getErrorMessage(exception: Exception): String {
        return when {
            exception.message?.contains("network", ignoreCase = true) == true -> 
                "Network error. Please check your connection and try again."
            exception.message?.contains("timeout", ignoreCase = true) == true -> 
                "Request timed out. Please try again."
            exception.message?.contains("authentication", ignoreCase = true) == true -> 
                "Authentication failed. Please try again later."
            else -> 
                "Verification failed. Please check your details and try again."
        }
    }

    fun initiatePanFetch(mobileNumber: String, force: Boolean = false) {
        platformLog("PreVerificationVM: 📡 initiatePanFetch requested for $mobileNumber, force=$force")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                panFetchResult = Resource.Loading()
            )
            try {
                preVerificationRepository.initiatePanFetch(mobileNumber, force).collectLatest { result ->
                    _uiState.value = _uiState.value.copy(panFetchResult = result)
                }
            } catch (e: Exception) {
                platformLog("PreVerificationVM: ❌ Exception in initiatePanFetch: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    panFetchResult = Resource.Error(e.message ?: "Failed to initiate PAN fetch")
                )
            }
        }
    }

    fun clearPanFetchResult() {
        _uiState.value = _uiState.value.copy(panFetchResult = null)
    }

    fun verifyOtpAndFetchPan(mobileNumber: String, prefillId: Long, otp: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                panVerifyOtpResult = Resource.Loading()
            )
            try {
                preVerificationRepository.verifyOtpAndFetchPan(mobileNumber, prefillId, otp).collectLatest { result ->
                    _uiState.value = _uiState.value.copy(panVerifyOtpResult = result)
                }
            } catch (e: Exception) {
                platformLog("PreVerificationVM: ❌ Exception in verifyOtpAndFetchPan: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    panVerifyOtpResult = Resource.Error(e.message ?: "Failed to verify OTP")
                )
            }
        }
    }

    fun clearPanVerifyOtpResult() {
        _uiState.value = _uiState.value.copy(panVerifyOtpResult = null)
    }

    private fun maskPan(pan: String): String {
        if (pan.length < 10) return pan
        return pan.substring(0, 3) + "******" + pan.substring(9)
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
}
