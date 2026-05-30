package com.pyllar.consumer.presentation.referral

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyllar.consumer.data.local.LocalOnboardingStore
import com.pyllar.consumer.domain.repository.ReferralRepository
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ReferralViewModel(
    private val referralRepository: ReferralRepository,
    private val localStore: LocalOnboardingStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReferralUiState())
    val uiState: StateFlow<ReferralUiState> = _uiState.asStateFlow()

    init {
        loadAll()
    }

    fun loadAll() {
        viewModelScope.launch {
            val userId = localStore.getCurrentUserId()
            if (userId.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    isCodeLoading = false,
                    isStatsLoading = false,
                    errorMessage = "User session expired. Please log in again."
                )
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                isCodeLoading = true,
                isStatsLoading = true,
                errorMessage = null
            )

            // Concurrently fetch referral code
            launch {
                referralRepository.getMyCode(userId).collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            val data = result.data
                            _uiState.value = _uiState.value.copy(
                                isCodeLoading = false,
                                referralCode = data?.referralCode ?: "",
                                shareUrl = data?.shareUrl ?: "",
                                shareMessage = data?.shareMessage ?: "",
                                referralEnabled = data?.referralEnabled ?: false
                            )
                        }
                        is Resource.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isCodeLoading = false,
                                errorMessage = result.message ?: "Failed to fetch referral details"
                            )
                        }
                        is Resource.Loading -> {
                            _uiState.value = _uiState.value.copy(isCodeLoading = true)
                        }
                    }
                }
            }

            // Concurrently fetch referral stats
            launch {
                referralRepository.getMyStats(userId).collect { result ->
                    when (result) {
                        is Resource.Success -> {
                            val data = result.data
                            _uiState.value = _uiState.value.copy(
                                isStatsLoading = false,
                                balanceCoins = data?.pendingRewardPaise?.toInt() ?: 0,
                                lifetimeEarnedCoins = ((data?.pendingRewardPaise ?: 0) + (data?.creditedRewardPaise ?: 0)).toInt(),
                                withdrawnCoins = data?.creditedRewardPaise?.toInt() ?: 0,
                                invitedCount = data?.totalReferrals?.toInt() ?: 0,
                                earnedCount = data?.convertedReferrals?.toInt() ?: 0
                            )
                        }
                        is Resource.Error -> {
                            _uiState.value = _uiState.value.copy(
                                isStatsLoading = false,
                                errorMessage = result.message ?: "Failed to fetch referral statistics"
                            )
                        }
                        is Resource.Loading -> {
                            _uiState.value = _uiState.value.copy(isStatsLoading = true)
                        }
                    }
                }
            }
        }
    }

    fun dismissSuccessMessage() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    fun dismissErrorMessage() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
