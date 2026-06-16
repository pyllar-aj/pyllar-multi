package com.pyllar.consumer.presentation.referral

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pyllar.consumer.domain.storage.SessionStore
import com.pyllar.consumer.domain.repository.ReferralRepository
import com.pyllar.consumer.data.remote.model.dto.ReferredUserEntryDto
import com.pyllar.consumer.data.remote.model.dto.CoinRedemptionHistoryEntryDto
import com.pyllar.consumer.data.remote.model.dto.CoinRedemptionHistoryDto
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

class ReferralViewModel(
    private val referralRepository: ReferralRepository,
    private val sessionStore: SessionStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReferralUiState())
    val uiState: StateFlow<ReferralUiState> = _uiState.asStateFlow()

    init {
        loadAll()
    }

    fun loadAll() {
        viewModelScope.launch {
            val userId = sessionStore.getCurrentUserId()
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

            val codeDeferred = async {
                var codeRes: Resource<com.pyllar.consumer.data.remote.model.dto.ReferralCodeDto> = Resource.Loading()
                referralRepository.getMyCode(userId).collect { result ->
                    if (result !is Resource.Loading) {
                        codeRes = result
                    }
                }
                codeRes
            }

            val dashboardDeferred = async {
                var dashboardRes: Resource<com.pyllar.consumer.data.remote.model.dto.ReferralDashboardDto> = Resource.Loading()
                referralRepository.getMyDashboard(userId).collect { result ->
                    if (result !is Resource.Loading) {
                        dashboardRes = result
                    }
                }
                dashboardRes
            }

            val statsDeferred = async {
                var statsRes: Resource<com.pyllar.consumer.data.remote.model.dto.ReferralStatsOnlyDto> = Resource.Loading()
                referralRepository.getMyStats(userId).collect { result ->
                    if (result !is Resource.Loading) {
                        statsRes = result
                    }
                }
                statsRes
            }

            val historyDeferred = async {
                var historyRes: Resource<com.pyllar.consumer.data.remote.model.dto.CoinRedemptionHistoryDto> = Resource.Loading()
                referralRepository.getRedemptionHistory(userId).collect { result ->
                    if (result !is Resource.Loading) {
                        historyRes = result
                    }
                }
                historyRes
            }

            val codeResult = codeDeferred.await()
            val dashResult = dashboardDeferred.await()
            val statsResult = statsDeferred.await()
            val historyResult = historyDeferred.await()

            // Process codeResult
            when (codeResult) {
                is Resource.Success -> {
                    val data = codeResult.data
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
                        errorMessage = codeResult.message ?: "Failed to fetch referral details"
                    )
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isCodeLoading = false)
                }
            }

            // Process dashResult
            when (dashResult) {
                is Resource.Success -> {
                    val data = dashResult.data
                    _uiState.value = _uiState.value.copy(
                        isStatsLoading = false,
                        balanceCoins = data?.coinsAvailable?.toInt() ?: 0,
                        lifetimeEarnedCoins = ((data?.coinsPending ?: 0) + (data?.coinsCredited ?: 0) + (data?.totalCashedOut ?: 0)).toInt(),
                        withdrawnCoins = data?.totalCashedOut?.toInt() ?: 0,
                        invitedCount = data?.totalReferrals?.toInt() ?: 0,
                        earnedCount = data?.convertedReferrals?.toInt() ?: 0,
                        referredUsers = data?.referredUsers?.map { mapToReferredUser(it) } ?: emptyList()
                    )
                }
                is Resource.Error -> {
                    _uiState.value = _uiState.value.copy(
                        isStatsLoading = false,
                        errorMessage = dashResult.message ?: "Failed to fetch referral statistics"
                    )
                }
                else -> {
                    _uiState.value = _uiState.value.copy(isStatsLoading = false)
                }
            }

            // Process statsResult
            if (statsResult is Resource.Success) {
                _uiState.value = _uiState.value.copy(
                    minimumCashoutAmount = statsResult.data?.minimumCashoutAmount?.toInt() ?: 1000,
                    qualifyingDays = statsResult.data?.qualifyingDays?.toInt() ?: 7
                )
            }

            // Process historyResult
            if (historyResult is Resource.Success) {
                _uiState.value = _uiState.value.copy(
                    withdrawalHistory = historyResult.data?.requests?.map { mapToWithdrawalHistory(it) } ?: emptyList()
                )
            }
        }
    }

    private fun mapToReferredUser(entry: ReferredUserEntryDto): ReferredUser {
        val name = entry.referredDisplayName ?: "Friend"
        val phone = entry.referredDisplayPhone?.let { "...${it}" } ?: ""
        val displayName = if (phone.isNotBlank()) "$name $phone" else name

        val joinDetail = entry.referredAt?.let { dateStr ->
            runCatching {
                val datePart = dateStr.substring(0, 10)
                val localDate = LocalDate.parse(datePart)
                val monthName = when (localDate.monthNumber) {
                    1 -> "Jan"; 2 -> "Feb"; 3 -> "Mar"; 4 -> "Apr"; 5 -> "May"; 6 -> "Jun"
                    7 -> "Jul"; 8 -> "Aug"; 9 -> "Sep"; 10 -> "Oct"; 11 -> "Nov"; 12 -> "Dec"
                    else -> "Jan"
                }
                "Joined ${localDate.dayOfMonth} $monthName"
            }.getOrElse { "Joined" }
        } ?: ""

        val (statusText, statusType) = when (entry.milestoneStatus) {
            "STREAK_DONE"    -> "Completed 7 SIPs" to ReferredUserStatus.EARNED
            "FIRST_INVESTED" -> "Made first investment" to ReferredUserStatus.IN_PROGRESS
            else             -> "Signed up" to ReferredUserStatus.INVITED
        }

        val rewardText = if (entry.coinsEarned > 0) "+${entry.coinsEarned} coins" else ""

        return ReferredUser(
            name = displayName,
            phone = phone,
            joinDetail = joinDetail,
            statusText = statusText,
            rewardText = rewardText,
            statusType = statusType
        )
    }

    private fun mapToWithdrawalHistory(entry: CoinRedemptionHistoryEntryDto): WithdrawalHistory {
        val displayDate = entry.disbursementTxnDate ?: entry.requestedAt?.substring(0, 10) ?: ""
        val statusLabel = when (entry.status) {
            "DISBURSED"  -> "Done"
            "FAILED"     -> "Failed"
            "PROCESSING" -> "Processing"
            else         -> "Pending"
        }
        return WithdrawalHistory(
            amount = entry.coinsRequested.toInt(),
            date = displayDate,
            bankDetails = entry.disbursementTxnId ?: "",
            status = statusLabel
        )
    }

    fun requestRedemption(amount: Int) {
        viewModelScope.launch {
            val userId = sessionStore.getCurrentUserId()
            _uiState.value = _uiState.value.copy(isWithdrawLoading = true, errorMessage = null)
            referralRepository.requestRedemption(userId, amount).collect { result ->
                when (result) {
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isWithdrawLoading = false,
                            successMessage = "Withdrawal request placed! ₹$amount will be credited to your account shortly."
                        )
                        loadAll()
                    }
                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(
                            isWithdrawLoading = false,
                            errorMessage = result.message ?: "Withdrawal failed. Please try again."
                        )
                    }
                    is Resource.Loading -> Unit
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

