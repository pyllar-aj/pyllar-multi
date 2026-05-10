package com.pyllar.consumer.data.remote.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SipActionRequest(
    @SerialName("userId")
    val userId: String,
    @SerialName("planId")
    val planId: String?,
    @SerialName("mandateId")
    val mandateId: Long?,
    @SerialName("action")
    val action: String, // PAUSE, RESUME, CANCEL
    @SerialName("reason")
    val reason: String? = null
)

@Serializable
data class ActionPollRequest(
    @SerialName("userId")
    val userId: String,
    @SerialName("actionId")
    val actionId: String,
    @SerialName("action")
    val action: String
)
