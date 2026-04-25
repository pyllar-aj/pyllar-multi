package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpiVpaVerifyResponseDto(
    val vpa: String,
    val accountNumber: String? = null,
    val bankName: String? = null,
    val ifscCode: String? = null,
    val accountHolderName: String? = null,
    val isVerified: Boolean = false
)
