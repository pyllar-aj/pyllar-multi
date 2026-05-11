package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class MandateWrapper(
    val finMandateId: Long?,
    val mandateId: Long?,
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
    val status: String,
    val message: String?,
    val mandateId: Long?,
    val mandateRef: Long?,
    val requiresPolling: Boolean = false
)

@Serializable
data class MandateSyncResponseDto(
    val mandateStatus: String,
    val message: String?,
    val mandateId: Long?,
    val mandateRef: Long?,
    val requiresPolling: Boolean = false,
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
