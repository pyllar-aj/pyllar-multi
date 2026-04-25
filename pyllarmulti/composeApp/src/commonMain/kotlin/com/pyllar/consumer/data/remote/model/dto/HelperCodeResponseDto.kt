package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HelperCodeResponseDto(
    @SerialName("helperCode")
    val helperCode: String? = null,
    @SerialName("helperCodeCreatedAt")
    val helperCodeCreatedAt: String? = null,
    @SerialName("exists")
    val exists: Boolean? = null
)

@Serializable
data class AccountDeletionRequestDto(
    @SerialName("userId")
    val userId: String
)
