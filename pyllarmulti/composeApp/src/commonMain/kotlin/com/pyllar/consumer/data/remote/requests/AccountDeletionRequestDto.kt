package com.pyllar.consumer.data.remote.requests

import kotlinx.serialization.Serializable

@Serializable
data class AccountDeletionRequestDto(
    val userId: String,
    val notes: String? = null
)

