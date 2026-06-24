package com.pyllar.consumer.presentation.mutualfund.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyllar.consumer.data.remote.model.dto.UserDetailsFetchResponseDto
import com.pyllar.consumer.data.remote.model.dto.UserDetailsFetchState
import com.pyllar.consumer.data.remote.requests.UserDetailsFetchRequest
import com.pyllar.consumer.domain.repository.OnboardingRepository
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.util.platformLog
import com.pyllar.consumer.util.currentTimeMillis
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

private const val DEFAULT_POLL_DELAY_MS = 4000L

class PennyDropLoadingModel(
    private val onboardingRepository: OnboardingRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PennyDropUiState())
    val uiState: StateFlow<PennyDropUiState> = _uiState.asStateFlow()

    private var pollingJob: Job? = null

    fun start(userId: String, phoneNumber: String, name: String) {
        pollingJob?.cancel()
        _uiState.value = PennyDropUiState(isLoading = true)
        pollingJob = viewModelScope.launch {
            val startTime = currentTimeMillis()
            val timeoutMillis = 3 * 60 * 1000L

            while (true) {
                val elapsed = currentTimeMillis() - startTime
                if (elapsed >= timeoutMillis) {
                    platformLog("PennyDropLoadingModel: ⏱️ Polling timeout reached after 3 minutes")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Please try again or contact support."
                    )
                    break
                }

                var nextDelayMs = DEFAULT_POLL_DELAY_MS

                onboardingRepository.fetchUserDetails(userId, UserDetailsFetchRequest(phoneNumber, name))
                    .collect { result ->
                        when (result) {
                            is Resource.Success -> {
                                val dto = result.data ?: UserDetailsFetchResponseDto()
                                val navigation = result.navigation
                                val nextScreen = navigation?.nextScreen?.takeIf { navigation.action == com.pyllar.consumer.data.remote.model.dto.NavigationAction.NAVIGATE }

                                _uiState.value = _uiState.value.copy(
                                    isLoading = dto.overallStatus != UserDetailsFetchState.SUCCESS && dto.overallStatus != UserDetailsFetchState.FAILED,
                                    overallStatus = dto.overallStatus,
                                    mobileAccountStatus = dto.mobileAccountStatus,
                                    creditBureauStatus = dto.creditBureauStatus,
                                    error = if (dto.overallStatus == UserDetailsFetchState.FAILED) dto.errorMessage else null,
                                    nextScreen = nextScreen
                                )

                                navigation?.params?.get("delay_seconds")?.jsonPrimitive?.longOrNull?.let { seconds ->
                                    nextDelayMs = seconds * 1000L
                                }
                            }
                            is Resource.Error -> {
                                platformLog("PennyDropLoadingModel: ❌ Poll failed: ${result.message}, retrying in ${nextDelayMs}ms...")
                            }
                            is Resource.Loading -> Unit
                        }
                    }

                val current = _uiState.value
                if (current.overallStatus == UserDetailsFetchState.SUCCESS || current.overallStatus == UserDetailsFetchState.FAILED) {
                    break
                }

                delay(nextDelayMs)
            }
        }
    }

    fun stop() {
        pollingJob?.cancel()
        pollingJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stop()
    }
}

data class PennyDropUiState(
    val isLoading: Boolean = false,
    val overallStatus: UserDetailsFetchState = UserDetailsFetchState.PENDING,
    val mobileAccountStatus: UserDetailsFetchState = UserDetailsFetchState.PENDING,
    val creditBureauStatus: UserDetailsFetchState = UserDetailsFetchState.PENDING,
    val error: String? = null,
    val nextScreen: String? = null
)
