package com.pyllar.consumer.presentation.mutualfund.onboarding

import com.pyllar.consumer.util.platformLog
import com.pyllar.consumer.data.remote.requests.PollMandateRequest
import com.pyllar.consumer.data.remote.model.dto.MandateWrapper
import com.pyllar.consumer.data.remote.model.dto.MandateStatus
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CoroutineScope

/**
 * UI state for MandateAuthScreen
 */
data class MandateAuthUiState(
    val isLoading: Boolean = false,
    val mandateStatus: MandateStatus? = null,
    val message: String? = null,
    val requiresPolling: Boolean = false,
    val shouldNavigateToDashboard: Boolean = false,
    val error: String? = null,
    val mandateWrapper: MandateWrapper? = null,
    val planSetupProgress: Int = 20,
    val planPollingStarted: Boolean = false,
    val planPollingResolved: Boolean = false,
    val planPollingTimedOut: Boolean = false,
    val isPlanReady: Boolean = false
)

/**
 * ViewModel for MandateAuthScreen that handles mandate sync and polling.
 * Ported to commonMain for KMP.
 */
class MandateAuthModel(
    private val repository: com.pyllar.consumer.domain.repository.FundDetailsRepository,
    private val viewModelScope: CoroutineScope
) {

    private val _uiState = MutableStateFlow(MandateAuthUiState())
    val uiState: StateFlow<MandateAuthUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    /**
     * Handle create-daily-sip response with MandateWrapper
     */
    fun handleCreateDailySipResponse(mandateWrapper: MandateWrapper) {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            mandateWrapper = mandateWrapper,
            message = "Daily SIP created successfully"
        )

        // If we have mandate IDs, start polling
        if (mandateWrapper.mandateId != null && mandateWrapper.finMandateId != null) {
            startMandateSync(
                userId = "", // This should be passed from the calling context
                mandateId = mandateWrapper.mandateId!!,
                mandateRef = mandateWrapper.finMandateId!!
            )
        }
    }

    /**
     * Start mandate sync and polling
     */
    fun startMandateSync(userId: String, mandateId: Long, mandateRef: Long) {
        platformLog("MandateAuthModel: \uD83D\uDE80 Starting mandate sync for userId: $userId, mandateId: $mandateId, mandateRef: $mandateRef")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                requiresPolling = true,
                error = null
            )

            // Start polling immediately
            startPolling(userId, mandateId, mandateRef)
        }
    }

    /**
     * Start background polling for mandate status
     * Polls for a maximum of 3 minutes (180 seconds) before timing out
     */
    private fun startPolling(userId: String, mandateId: Long, mandateRef: Long) {
        platformLog("MandateAuthModel: \uD83D\uDD04 Starting polling loop with 3-minute timeout")
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            var secondsElapsed = 0
            val maxSeconds = 180 // 3 minutes
            
            while (secondsElapsed < maxSeconds) {
                try {
                    platformLog("MandateAuthModel: \uD83D\uDCE1 Making sync-mandate API call... (${secondsElapsed}s elapsed)")
                    val request = PollMandateRequest(
                        userId = userId,
                        mandateId = mandateId,
                        mandateRef = mandateRef
                    )

                    repository.syncMandate(request).collect { result ->
                        when (result) {
                            is Resource.Success -> {
                                val status = result.data
                                platformLog("MandateAuthModel: \u2705 Received mandate status: $status")

                                // Check if mandate is in final state
                                if (status != null && isFinalStatus(status)) {
                                    platformLog("MandateAuthModel: \uD83C\uDFAF Mandate reached final status: $status - showing status then navigating")
                                    _uiState.value = _uiState.value.copy(
                                        isLoading = false, // Stop loading to show final status
                                        mandateStatus = status,
                                        message = null,
                                        requiresPolling = false,
                                        shouldNavigateToDashboard = true,
                                        error = null
                                    )
                                } else {
                                    // Continue polling - keep loading state
                                    _uiState.value = _uiState.value.copy(
                                        isLoading = true, // Keep loading while polling
                                        mandateStatus = status,
                                        message = null,
                                        requiresPolling = true,
                                        error = null
                                    )
                                }
                            }
                            is Resource.Error -> {
                                platformLog("MandateAuthModel: \u274C API call failed: ${result.message}")
                            }
                            is Resource.Loading -> {
                                // Keep loading state
                            }
                        }
                    }

                    if (!_uiState.value.requiresPolling || secondsElapsed >= maxSeconds) {
                        break
                    }

                    platformLog("MandateAuthModel: \u231B Mandate still processing, waiting 5 seconds...")
                    delay(5000L) // Poll every 5 seconds
                    secondsElapsed += 5
                } catch (e: Exception) {
                    platformLog("MandateAuthModel: \uD83D\uDCA5 Exception during polling: ${e.message}, retrying in 5 seconds...")
                    delay(5000L) // Wait 5 seconds before retry
                    secondsElapsed += 5
                }
            }

            if (secondsElapsed >= maxSeconds) {
                platformLog("MandateAuthModel: \u23F1\uFE0F Polling timeout reached after 3 minutes")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    requiresPolling = false,
                    error = "Please try again or contact support.",
                    shouldNavigateToDashboard = false
                )
            }
        }
    }

    /**
     * Check if mandate status is final (no more polling needed)
     */
    private fun isFinalStatus(status: MandateStatus): Boolean {
        return status == MandateStatus.APPROVED || 
               status == MandateStatus.REJECTED || 
               status == MandateStatus.CANCELLED
    }

    /**
     * Stop polling
     */
    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    /**
     * Stop polling and reset loading/polling state
     * Use this when switching away from a polling-active view (like QR code tab)
     */
    fun resetPollingState() {
        platformLog("MandateAuthModel: \uD83D\uDED1 Stopping polling and resetting state")
        stopPolling()
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            requiresPolling = false
        )
    }

    /**
     * Clear error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun startPlanPollingAfterApproval(userId: String, mandateRef: Long) {
        if (mandateRef <= 0L || _uiState.value.planPollingStarted) return
        _uiState.value = _uiState.value.copy(
            planPollingStarted = true,
            planPollingResolved = false,
            planPollingTimedOut = false,
            planSetupProgress = 20,
            isPlanReady = false
        )

        viewModelScope.launch {
            val startTime = com.pyllar.consumer.util.currentTimeMillis()
            val timeoutMillis = 3 * 60 * 1000L
            val progressSteps = listOf(20, 40, 60, 80, 100)
            var progressIndex = 1 // 20 already set

            while (true) {
                val elapsed = com.pyllar.consumer.util.currentTimeMillis() - startTime
                if (elapsed >= timeoutMillis) {
                    _uiState.value = _uiState.value.copy(
                        planPollingTimedOut = true,
                        planPollingResolved = true,
                        isPlanReady = false
                    )
                    break
                }

                if (progressIndex < progressSteps.size - 1) {
                    _uiState.value = _uiState.value.copy(planSetupProgress = progressSteps[progressIndex])
                    progressIndex++
                }

                val request = com.pyllar.consumer.data.remote.requests.PlanPollRequest(
                    userId = userId,
                    mandateRef = mandateRef,
                    mfppId = null
                )

                var shouldBreak = false
                repository.pollPurchasePlanStatus(request).collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            // In KMP, we don't have the same navigation action logic in the repository level yet
                            // but the Android version checks NavigationAction.STAY.
                            // For now, if result.data is true, we consider it ready.
                            if (result.data == true || result.navigation?.action == com.pyllar.consumer.data.remote.model.dto.NavigationAction.STAY) {
                                _uiState.value = _uiState.value.copy(
                                    planSetupProgress = 100,
                                    isPlanReady = result.data == true,
                                    planPollingResolved = true
                                )
                                shouldBreak = true
                            }
                        }
                        is Resource.Error -> {
                            // Keep polling on transient failures until timeout
                        }
                        is Resource.Loading -> Unit
                    }
                }

                if (shouldBreak) break
                delay(5000L)
            }
        }
    }

    /**
     * Reset UI state
     */
    fun resetState() {
        _uiState.value = MandateAuthUiState()
    }

    fun onCleared() {
        stopPolling()
    }
}
