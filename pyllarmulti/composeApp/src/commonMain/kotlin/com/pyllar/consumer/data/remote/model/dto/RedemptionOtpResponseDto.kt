package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class RedemptionOtpResponseDto(
    val tokenTrackerId: String?
)
