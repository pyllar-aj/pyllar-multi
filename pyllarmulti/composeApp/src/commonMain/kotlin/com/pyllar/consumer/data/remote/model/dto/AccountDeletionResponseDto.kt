package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class AccountDeletionResponseDto(
    val requestId: String? = null,
    val userId: String? = null,
    val status: String? = null,
    val requestedAt: String? = null,
    val processedAt: String? = null,
    val message: String? = null
)

