package com.pyllar.consumer.presentation.referral

data class ReferralUiState(
    val referralCode: String = "",
    val shareUrl: String = "",
    val shareMessage: String = "",
    val referralEnabled: Boolean = false,
    val balanceCoins: Int = 0,
    val lifetimeEarnedCoins: Int = 0,
    val withdrawnCoins: Int = 0,
    val invitedCount: Int = 0,
    val earnedCount: Int = 0,
    val referredUsers: List<ReferredUser> = emptyList(),
    val withdrawalHistory: List<WithdrawalHistory> = emptyList(),
    val isCodeLoading: Boolean = true,
    val isStatsLoading: Boolean = true,
    val successMessage: String? = null,
    val errorMessage: String? = null
)

data class ReferredUser(
    val name: String,
    val joinDetail: String,
    val statusText: String,
    val rewardText: String,
    val statusType: ReferredUserStatus
)

enum class ReferredUserStatus {
    INVITED,
    IN_PROGRESS,
    EARNED
}

data class WithdrawalHistory(
    val amount: Int,
    val date: String,
    val bankDetails: String,
    val status: String = "Done"
)
