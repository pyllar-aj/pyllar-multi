package com.pyllar.consumer.data.remote.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GoalSelectionRequest(
    @SerialName("userId")
    val userId: String,
    @SerialName("goal")
    val goal: String,
    @SerialName("attributionProviderName")
    val attributionProviderName: String? = null,
    @SerialName("attributionMediaSource")
    val attributionMediaSource: String? = null,
    @SerialName("attributionCampaign")
    val attributionCampaign: String? = null,
    @SerialName("attributionCampaignId")
    val attributionCampaignId: String? = null,
    @SerialName("attributionAdSet")
    val attributionAdSet: String? = null
)

