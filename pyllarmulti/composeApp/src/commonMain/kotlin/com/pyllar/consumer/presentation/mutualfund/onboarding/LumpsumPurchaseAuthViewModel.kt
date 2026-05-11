package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyllar.consumer.domain.repository.FundDetailsRepository
import com.pyllar.consumer.data.remote.model.dto.LumpsumPaymentStatusRequest
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.util.platformLog
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LumpsumPurchaseAuthViewModel(
    private val repository: FundDetailsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LumpsumPurchaseUiState())
    val uiState: StateFlow<LumpsumPurchaseUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    fun startPaymentSync(userId: String, paymentId: Long) {
        platformLog("LumpsumPurchaseAuthViewModel: Starting sync for userId: $userId, paymentId: $paymentId")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            startPolling(userId, paymentId)
        }
    }

    private fun startPolling(userId: String, paymentId: Long) {
        pollingJob?.cancel()
        pollingJob = viewModelScope.launch {
            val startTime = currentTimeMillis()
            val timeoutMillis = 3 * 60 * 1000L // 3 minutes

            while (true) {
                try {
                    val elapsedTime = currentTimeMillis() - startTime
                    if (elapsedTime >= timeoutMillis) {
                        platformLog("LumpsumPurchaseAuthViewModel: Polling timeout reached")
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            requiresPolling = false,
                            error = "Please try again or contact support.",
                            status = PurchaseStatus.FAILED
                        )
                        break
                    }

                    val request = LumpsumPaymentStatusRequest(
                        userId = userId,
                        paymentId = paymentId
                    )

                    repository.syncLumpsumPayment(request).collect { result ->
                        when (result) {
                            is Resource.Success -> {
                                val responseData = result.data
                                val statusStr = (responseData?.get("status") ?: responseData?.get("paymentStatus") ?: "").uppercase()

                                platformLog("LumpsumPurchaseAuthViewModel: Received status: $statusStr")

                                val parsedStatus = when {
                                    statusStr.contains("SUCCESS") || statusStr == "APPROVED" || statusStr == "PAID" -> PurchaseStatus.SUCCESS
                                    statusStr.contains("FAIL") || statusStr == "REJECTED" -> PurchaseStatus.FAILED
                                    statusStr.contains("CANCEL") -> PurchaseStatus.CANCELLED
                                    else -> PurchaseStatus.PENDING
                                }

                                if (parsedStatus != PurchaseStatus.PENDING) {
                                    _uiState.value = _uiState.value.copy(
                                        isLoading = false,
                                        status = parsedStatus,
                                        requiresPolling = false,
                                    )
                                    pollingJob?.cancel()
                                } else {
                                    _uiState.value = _uiState.value.copy(
                                        isLoading = true,
                                        status = PurchaseStatus.PENDING,
                                        requiresPolling = true
                                    )
                                }
                            }
                            is Resource.Error -> {
                                platformLog("LumpsumPurchaseAuthViewModel: API call failed: ${result.message}")
                                val errorMsg = result.message ?: ""
                                if (errorMsg.contains("connect", true) || errorMsg.contains("Network", true)) {
                                    _uiState.value = _uiState.value.copy(
                                        errorMessage = "Check your Internet connection and try again"
                                    )
                                }
                            }
                            else -> {}
                        }
                    }
                    delay(5000L)
                } catch (e: Exception) {
                    platformLog("LumpsumPurchaseAuthViewModel: Exception during polling: ${e.message}")
                    delay(5000L)
                }
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    fun clearErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun resetPollingState() {
        stopPolling()
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            requiresPolling = false
        )
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }
    
    private fun currentTimeMillis(): Long {
        return kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
    }
}

data class LumpsumPurchaseUiState(
    val isLoading: Boolean = false,
    val status: PurchaseStatus = PurchaseStatus.PENDING,
    val message: String? = null,
    val requiresPolling: Boolean = false,
    val error: String? = null,
    val errorMessage: String? = null
)

enum class PurchaseStatus {
    PENDING,
    SUCCESS,
    FAILED,
    CANCELLED
}
