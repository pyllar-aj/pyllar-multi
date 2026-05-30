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
    @SerialName("pendingRewardPaise") val pendingRewardPaise: Long = 0,
    @SerialName("creditedRewardPaise") val creditedRewardPaise: Long = 0
)

@Serializable
data class ReferralStatsDto(
    @SerialName("referralCode") val referralCode: String? = null,
    @SerialName("campaigns") val campaigns: List<CampaignShareInfoDto>? = null,
    @SerialName("totalReferrals") val totalReferrals: Long = 0,
    @SerialName("convertedReferrals") val convertedReferrals: Long = 0,
    @SerialName("pendingRewardPaise") val pendingRewardPaise: Long = 0,
    @SerialName("creditedRewardPaise") val creditedRewardPaise: Long = 0
)

@Serializable
data class CampaignShareInfoDto(
    @SerialName("campaignCode") val campaignCode: String? = null,
    @SerialName("campaignName") val campaignName: String? = null,
    @SerialName("shareUrl") val shareUrl: String? = null,
    @SerialName("referrerRewardPaise") val referrerRewardPaise: Long = 0,
    @SerialName("refereeRewardPaise") val refereeRewardPaise: Long = 0,
    @SerialName("minInvestmentPaise") val minInvestmentPaise: Long = 0,
    @SerialName("validUntil") val validUntil: String? = null
)
