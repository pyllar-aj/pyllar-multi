package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyllar.consumer.data.remote.model.MinimalKycRequest
import com.pyllar.consumer.data.remote.model.MinimalKycResponse
import com.pyllar.consumer.data.remote.model.Mobile
import com.pyllar.consumer.data.remote.model.dto.NavigationAction
import com.pyllar.consumer.data.remote.model.dto.NavigationInfo
import com.pyllar.consumer.data.remote.model.dto.FieldError
import com.pyllar.consumer.navigation.ScreenNames
import com.pyllar.consumer.domain.repository.PreVerificationRepository
import com.pyllar.consumer.domain.repository.OnboardingRepository
import com.pyllar.consumer.domain.repository.CommonRepository
import com.pyllar.consumer.domain.storage.SessionStore
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.util.platformLog
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.longOrNull

/**
 * Combines the two API calls that used to live on separate screens
 * (PreVerification's PAN readiness check, then NameDob's name/dob submission)
 * into a single sequential flow for [UserInfoScreen].
 */
class UserInfoViewModel(
    private val preVerificationRepository: PreVerificationRepository,
    private val onboardingRepository: OnboardingRepository,
    private val commonRepository: CommonRepository,
    private val sessionStore: SessionStore
) : ViewModel() {

    companion object {
        private const val TAG = "UserInfoViewModel"
        private const val READINESS_POLL_INTERVAL_MS = 5000L
        private const val READINESS_MAX_ATTEMPTS = 24 // ~2 minutes
        private const val DETAILS_POLL_INTERVAL_MS = 5000L
        private const val DETAILS_MAX_ATTEMPTS = 24 // ~2 minutes
    }

    enum class Stage { PAN, DETAILS }

    sealed class SubmitState {
        object Idle : SubmitState()
        data class CheckingPan(val message: String?) : SubmitState()
        data class SubmittingDetails(val message: String?) : SubmitState()
        data class Success(val navigation: NavigationInfo?, val data: MinimalKycResponse?) : SubmitState()
        data class Failed(val message: String?, val fieldErrors: List<FieldError>?, val stage: Stage) : SubmitState()
    }

    private val _submitState = MutableStateFlow<SubmitState>(SubmitState.Idle)
    val submitState: StateFlow<SubmitState> = _submitState.asStateFlow()

    private val _prefillData = MutableStateFlow<Map<String, Any?>>(emptyMap())
    val prefillData: StateFlow<Map<String, Any?>> = _prefillData.asStateFlow()

    private val _isLoadingPrefill = MutableStateFlow(true)
    val isLoadingPrefill: StateFlow<Boolean> = _isLoadingPrefill.asStateFlow()

    private var submitJob: Job? = null

    init {
        fetchPrepopulatedData()
    }

    /** "NameDob" screen data already carries name, dob and pan prefill — reuse it as-is. */
    private fun fetchPrepopulatedData() {
        viewModelScope.launch {
            commonRepository.fetchScreenData("NameDob").collect { result ->
                when (result) {
                    is Resource.Success -> {
                        result.data?.data?.let { jsonMap ->
                            _prefillData.value = jsonMap.mapValues { (_, element) ->
                                if (element is JsonPrimitive) element.content else element.toString()
                            }
                        }
                        _isLoadingPrefill.value = false
                    }
                    is Resource.Error -> {
                        _isLoadingPrefill.value = false
                    }
                    is Resource.Loading -> {
                        _isLoadingPrefill.value = true
                    }
                }
            }
        }
    }

    fun isBusy(): Boolean {
        val state = _submitState.value
        return state is SubmitState.CheckingPan || state is SubmitState.SubmittingDetails
    }

    fun submit(
        userId: String,
        name: String,
        pan: String,
        dob: String,
        emailAddress: String,
        mobileCountryCode: String,
        mobileNumber: String,
        token: String
    ) {
        if (isBusy()) return
        submitJob?.cancel()
        submitJob = viewModelScope.launch {
            _submitState.value = SubmitState.CheckingPan(null)

            var readinessResult: Resource<com.pyllar.consumer.data.remote.dto.PreVerificationResponseDto>? = null
            preVerificationRepository.checkInvestorReadiness(pan).collect {
                if (it !is Resource.Loading) {
                    readinessResult = it
                }
            }

            val readiness = readinessResult
            if (readiness == null || readiness is Resource.Error) {
                platformLog("$TAG: Readiness check failed: ${readiness?.message}")
                _submitState.value = SubmitState.Failed(readiness?.message, readiness?.fieldErrors, Stage.PAN)
                return@launch
            }

            val preVerificationId = readiness.data?.data?.id ?: readiness.data?.id
            if (preVerificationId == null) {
                _submitState.value = SubmitState.Failed("Invalid response from server", null, Stage.PAN)
                return@launch
            }

            // The server's nextScreen tells us which of the two submission APIs to call:
            // MIN_DETAILS for pre-verified users (MinDetailsScreenV2's createMinimalDetailsV2),
            // otherwise the full KYC flow (NameDobScreenV2's createNameDobV3).
            val nextScreen = pollUntilPanReady(preVerificationId) ?: return@launch

            val useMinDetails = nextScreen == ScreenNames.MIN_DETAILS
            submitDetails(useMinDetails, userId, name, pan, dob, emailAddress, mobileCountryCode, mobileNumber, token, null)
        }
    }

    /**
     * Polls the readiness-check result until the server stops asking us to poll, returning
     * the resolved nextScreen (defaulting to NAME_DOB if the server completes without one).
     * Returns null if polling failed/timed out/needs manual review — state is already set.
     */
    private suspend fun pollUntilPanReady(preVerificationId: String): String? {
        var attempts = 0
        while (attempts < READINESS_MAX_ATTEMPTS) {
            var statusResult: Resource<com.pyllar.consumer.data.remote.dto.PreVerificationResponseDto>? = null
            preVerificationRepository.fetchVerificationStatus(preVerificationId).collect {
                if (it !is Resource.Loading) {
                    statusResult = it
                }
            }

            when (val result = statusResult) {
                is Resource.Success -> {
                    val navigation = result.data?.navigation ?: result.navigation
                    when {
                        navigation?.shouldPoll() == true -> {
                            _submitState.value = SubmitState.CheckingPan(navigation.getMessage())
                        }
                        navigation?.shouldStay() == true -> {
                            _submitState.value = SubmitState.Failed(
                                navigation.getMessage() ?: "PAN verification needs manual review. Please contact support.",
                                null,
                                Stage.PAN
                            )
                            return null
                        }
                        navigation?.shouldNavigate() == true -> {
                            return navigation.nextScreen?.takeIf { it.isNotBlank() } ?: ScreenNames.NAME_DOB
                        }
                        result.data?.data?.isCompleted() == true -> {
                            return ScreenNames.NAME_DOB
                        }
                        else -> { /* keep polling */ }
                    }
                }
                is Resource.Error -> {
                    platformLog("$TAG: Readiness polling failed: ${result.message}")
                    _submitState.value = SubmitState.Failed(result.message, result.fieldErrors, Stage.PAN)
                    return null
                }
                else -> {}
            }
            attempts++
            delay(READINESS_POLL_INTERVAL_MS)
        }
        _submitState.value = SubmitState.Failed(
            "PAN verification is taking longer than expected. Please try again.",
            null,
            Stage.PAN
        )
        return null
    }

    private suspend fun submitDetails(
        useMinDetails: Boolean,
        userId: String,
        name: String,
        pan: String,
        dob: String,
        emailAddress: String,
        mobileCountryCode: String,
        mobileNumber: String,
        token: String,
        initialPreVerificationId: String?
    ) {
        _submitState.value = SubmitState.SubmittingDetails(null)
        var currentPreVerificationId = initialPreVerificationId
        var pollDelayMs = DETAILS_POLL_INTERVAL_MS
        var attempts = 0

        while (true) {
            val request = MinimalKycRequest(
                userId = userId,
                name = name,
                panNumber = pan,
                dateOfBirth = dob,
                emailAddress = emailAddress,
                mobile = Mobile(
                    countryCode = mobileCountryCode,
                    number = mobileNumber
                ),
                preVerificationId = currentPreVerificationId
            )
            
            val call = if (useMinDetails) {
                onboardingRepository.createMinimalDetails(request)
            } else {
                onboardingRepository.createMinimalKyc(request)
            }

            var submitResult: Resource<MinimalKycResponse>? = null
            call.collect { result ->
                if (result !is Resource.Loading) {
                    submitResult = result
                }
            }

            when (val result = submitResult) {
                is Resource.Success -> {
                    val navigation = result.navigation
                    if (navigation?.action == NavigationAction.POLL) {
                        _submitState.value = SubmitState.SubmittingDetails(
                            (navigation.params?.get("message") as? JsonPrimitive)?.contentOrNull
                                ?: "Verifying details…"
                        )
                        currentPreVerificationId = (navigation.params?.get("preVerificationId") as? JsonPrimitive)?.contentOrNull
                            ?: result.data?.kycAttemptId
                        pollDelayMs = (navigation.params?.get("delayMs") as? JsonPrimitive)?.longOrNull
                            ?: (navigation.params?.get("delay_seconds") as? JsonPrimitive)?.longOrNull?.let { it * 1000L }
                            ?: (navigation.params?.get("retry_after_sec") as? JsonPrimitive)?.longOrNull?.let { it * 1000L }
                            ?: (navigation.params?.get("retry_after_ms") as? JsonPrimitive)?.longOrNull
                            ?: (navigation.params?.get("poll_interval_ms") as? JsonPrimitive)?.longOrNull
                            ?: DETAILS_POLL_INTERVAL_MS
                    } else {
                        _submitState.value = SubmitState.Success(navigation, result.data)
                    }
                }
                is Resource.Error -> {
                    platformLog("$TAG: Details submission failed: ${result.message}")
                    _submitState.value = SubmitState.Failed(result.message, result.fieldErrors, Stage.DETAILS)
                }
                else -> {}
            }

            if (_submitState.value is SubmitState.Success || _submitState.value is SubmitState.Failed) return

            attempts++
            if (attempts > DETAILS_MAX_ATTEMPTS) {
                _submitState.value = SubmitState.Failed(
                    "Verification is taking longer than expected. Please try again.",
                    null,
                    Stage.DETAILS
                )
                return
            }
            delay(pollDelayMs)
        }
    }
}
