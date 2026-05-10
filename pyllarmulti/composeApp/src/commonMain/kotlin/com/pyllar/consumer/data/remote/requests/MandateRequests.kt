package com.pyllar.consumer.data.remote.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request DTOs for mandate operations
 */

@Serializable
data class PollMandateRequest(
    @SerialName("userId")
    val userId: String,

    @SerialName("mandateRef")
    val mandateRef: Long,

    @SerialName("mandateId")
    val mandateId: Long
)

@Serializable
data class MandateSyncRequest(
    @SerialName("userId")
    val userId: String,

    @SerialName("mandateRef")
    val mandateRef: Long,

    @SerialName("mandateId")
    val mandateId: Long
)

@Serializable
data class PlanPollRequest(
    @SerialName("userId")
    val userId: String,

    @SerialName("mandateRef")
    val mandateRef: Long,

    @SerialName("mfppId")
    val mfppId: Long? = null
)
