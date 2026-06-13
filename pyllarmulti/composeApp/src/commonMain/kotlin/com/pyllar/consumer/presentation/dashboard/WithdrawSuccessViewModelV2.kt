package com.pyllar.consumer.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyllar.consumer.domain.repository.RedemptionRepository
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.util.platformLog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RedemptionSyncResponse(
    @SerialName("status") val status: String? = null,
    @SerialName("redemptionId") val redemptionId: String? = null,
    @SerialName("redemptionGroupId") val redemptionGroupId: String? = null,
    @SerialName("errorMessage") val errorMessage: String? = null
)

enum class RedemptionPollStatus {
    PENDING, CONFIRMED, SUBMITTED, SUCCEEDED, FAILED, CANCELLED, TIMED_OUT
}

data class WithdrawSuccessV2UiState(
    val isPolling: Boolean = false,
    val status: RedemptionPollStatus = RedemptionPollStatus.PENDING,
    val errorMessage: String? = null,
    val hasTimedOut: Boolean = false
)

class WithdrawSuccessViewModelV2(
    private val redemptionRepository: RedemptionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WithdrawSuccessV2UiState())
    val uiState: StateFlow<WithdrawSuccessV2UiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    fun startPolling(userId: String, redemptionId: String, redemptionGroupId: String? = null) {
        if (pollingJob?.isActive == true) return
        val useGroup = !redemptionGroupId.isNullOrBlank()
        if (!useGroup && redemptionId.isBlank()) {
            platformLog("redemptionId and redemptionGroupId are both blank — cannot poll")
            return
        }

        val logKey = if (useGroup) "redemptionGroupId=$redemptionGroupId" else "redemptionId=$redemptionId"
        platformLog("🔄 Starting redemption polling for $logKey")
        _uiState.value = _uiState.value.copy(isPolling = true)

        pollingJob = viewModelScope.launch {
            val timeoutMillis = 1 * 60 * 1000L // 1 minute
            val request = buildMap<String, String> {
                put("userId", userId)
                if (useGroup) put("redemptionGroupId", redemptionGroupId!!)
                else put("redemptionId", redemptionId)
            }

            val completed = withTimeoutOrNull(timeoutMillis) {
                var iteration = 0
                while (true) {
                    try {
                        platformLog("📡 sync-status call #${++iteration} [$logKey]")

                        redemptionRepository.syncRedemptionStatus(request).collectLatest { result ->
                            when (result) {
                                is Resource.Success -> {
                                    val resp = result.data
                                    if (resp != null) {
                                        val newStatus = mapStatus(resp.status)
                                        platformLog("✅ status=$newStatus (raw=${resp.status})")

                                        _uiState.value = _uiState.value.copy(
                                            status = newStatus,
                                            errorMessage = resp.errorMessage,
                                            isPolling = !isFinal(newStatus)
                                        )
                                    }
                                }
                                is Resource.Error -> {
                                    platformLog("❌ API error: ${result.message}, retrying in 5s [$logKey]")
                                }
                                is Resource.Loading -> Unit
                            }
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        platformLog("💥 Exception during polling: ${e.message}")
                    }
                    
                    if (isFinal(_uiState.value.status)) {
                        return@withTimeoutOrNull true
                    }
                    
                    delay(5000L)
                }
                @Suppress("UNREACHABLE_CODE")
                true
            }

            if (completed == null) {
                platformLog("⏱️ Polling timed out after ${timeoutMillis / 1000}s")
                _uiState.value = _uiState.value.copy(
                    isPolling = false,
                    hasTimedOut = true,
                    status = RedemptionPollStatus.TIMED_OUT
                )
            }
        }
    }

    fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopPolling()
    }

    private fun mapStatus(raw: String?): RedemptionPollStatus = when (raw?.uppercase()) {
        "CONFIRMED" -> RedemptionPollStatus.CONFIRMED
        "SUBMITTED" -> RedemptionPollStatus.SUBMITTED
        "SUCCEEDED" -> RedemptionPollStatus.SUCCEEDED
        "FAILED"    -> RedemptionPollStatus.FAILED
        "CANCELLED" -> RedemptionPollStatus.CANCELLED
        else        -> RedemptionPollStatus.PENDING
    }

    private fun isFinal(s: RedemptionPollStatus) =
        s == RedemptionPollStatus.SUBMITTED ||
        s == RedemptionPollStatus.SUCCEEDED ||
        s == RedemptionPollStatus.FAILED    ||
        s == RedemptionPollStatus.CANCELLED
}
