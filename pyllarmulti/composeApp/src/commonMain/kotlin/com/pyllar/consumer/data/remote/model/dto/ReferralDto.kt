package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReferralCodeDto(
    @SerialName("referralCode") val referralCode: String? = null,
    @SerialName("shareUrl") val shareUrl: String? = null,
    @SerialName("shareMessage") val shareMessage: String? = null,
    @SerialName("referralEnabled") val referralEnabled: Boolean = false
)

@Serializable
data class ReferralStatsOnlyDto(
    @SerialName("totalReferrals") val totalReferrals: Long = 0,
    @SerialName("convertedReferrals") val convertedReferrals: Long = 0,
    @SerialName("coinsPending") val coinsPending: Long = 0,
    @SerialName("coinsCredited") val coinsCredited: Long = 0,
    @SerialName("coinsAvailable") val coinsAvailable: Long = 0,
    @SerialName("totalCashedOut") val totalCashedOut: Long = 0,
    @SerialName("minimumCashoutAmount") val minimumCashoutAmount: Long = 1000,
    @SerialName("qualifyingDays") val qualifyingDays: Long = 7
)

@Serializable
data class ReferralStatsDto(
    @SerialName("referralCode") val referralCode: String? = null,
    @SerialName("campaigns") val campaigns: List<CampaignShareInfoDto>? = null,
    @SerialName("totalReferrals") val totalReferrals: Long = 0,
    @SerialName("convertedReferrals") val convertedReferrals: Long = 0,
    @SerialName("coinsPending") val coinsPending: Long = 0,
    @SerialName("coinsCredited") val coinsCredited: Long = 0,
    @SerialName("coinsAvailable") val coinsAvailable: Long = 0,
    @SerialName("totalCashedOut") val totalCashedOut: Long = 0
)

@Serializable
data class ReferralDashboardDto(
    @SerialName("totalCoinsEarned") val totalCoinsEarned: Long = 0,
    @SerialName("coinsPending") val coinsPending: Long = 0,
    @SerialName("coinsCredited") val coinsCredited: Long = 0,
    @SerialName("totalCashedOut") val totalCashedOut: Long = 0,
    @SerialName("coinsAvailable") val coinsAvailable: Long = 0,
    @SerialName("totalReferrals") val totalReferrals: Long = 0,
    @SerialName("convertedReferrals") val convertedReferrals: Long = 0,
    @SerialName("referredUsers") val referredUsers: List<ReferredUserEntryDto> = emptyList()
)

@Serializable
data class ReferredUserEntryDto(
    @SerialName("referredDisplayName") val referredDisplayName: String? = null,
    @SerialName("referredDisplayPhone") val referredDisplayPhone: String? = null,
    @SerialName("referredAt") val referredAt: String? = null,
    @SerialName("milestoneStatus") val milestoneStatus: String = "SIGNED_UP",
    @SerialName("coinsEarned") val coinsEarned: Long = 0
)

@Serializable
data class CampaignShareInfoDto(
    @SerialName("campaignCode") val campaignCode: String? = null,
    @SerialName("campaignName") val campaignName: String? = null,
    @SerialName("shareUrl") val shareUrl: String? = null,
    @SerialName("referrerRewardCoins") val referrerRewardCoins: Long = 0,
    @SerialName("validUntil") val validUntil: String? = null
)
