package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class ProfileRequestDto(
    val userId: String
)

@Serializable
data class ProfileResponseDto(
    val name: String?,
    val email: String?,
    val phoneNumber: String?,
    val dob: String?,
    val gender: String?,
    val deletionRequested: Boolean? = null,
    val deletionRequestId: String? = null,
    val deletionStatus: String? = null,
    val deletionRequestedAt: String? = null,
    val deletionMessage: String? = null,
    val referralEnabled: Boolean? = false,
    val referredByCode: String? = null
)

