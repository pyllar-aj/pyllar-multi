package com.pyllar.consumer.data.remote.requests

import kotlinx.serialization.Serializable

@Serializable
data class SipActionRequest(
    val userId: String,
    val planId: String?,
    val mandateId: Long,
    val action: String,
    val reason: String
)

@Serializable
data class ActionPollRequest(
    val userId: String,
    val userIp: String = "0.0.0.0",
    val actionId: String,
    val action: String
)
