package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MandateWrapper(
    @SerialName("finMandateId")
    val finMandateId: Long?,
    @SerialName("mandateId")
    val mandateId: Long?,
    @SerialName("uri")
    val uri: String?
)

@Serializable
data class LumpsumPurchaseResponseData(
    val id: String?,
    val old_id: Long?,
    val payment_id: Long?,
    val token_url: String?
)

@Serializable
data class MandateStatusResponseDto(
    @SerialName("status")
    val status: String,
    @SerialName("message")
    val message: String?,
    @SerialName("mandateId")
    val mandateId: Long?,
    @SerialName("mandateRef")
    val mandateRef: Long?,
    @SerialName("requiresPolling")
    val requiresPolling: Boolean = false
)

@Serializable
data class MandateSyncResponseDto(
    @SerialName("mandateStatus")
    val mandateStatus: String,
    @SerialName("message")
    val message: String?,
    @SerialName("mandateId")
    val mandateId: Long?,
    @SerialName("mandateRef")
    val mandateRef: Long?,
    @SerialName("requiresPolling")
    val requiresPolling: Boolean = false,
    @SerialName("nextPollInSeconds")
    val nextPollInSeconds: Int? = null
)

@Serializable
enum class MandateStatus {
    CREATED,
    RECEIVED,
    APPROVED,
    SUBMITTED,
    REJECTED,
    CANCELLED
}
