package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class LumpsumPaymentStatusRequest(
    val userId: String,
    val paymentId: Long
)
