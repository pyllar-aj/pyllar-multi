package com.pyllar.consumer.data.remote.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GoalSelectionRequest(
    @SerialName("userId")
    val userId: String,
    @SerialName("goal")
    val goal: String
)

