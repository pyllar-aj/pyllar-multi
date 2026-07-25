package com.pyllar.consumer.data.remote.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DoubtsSurveyRequestDto(
    @SerialName("screenName")
    val screenName: String,
    @SerialName("goalId")
    val goalId: String? = null,
    @SerialName("selectedOption")
    val selectedOption: String,
    @SerialName("freeText")
    val freeText: String? = null,
    @SerialName("requestCallback")
    val requestCallback: Boolean = false
)
