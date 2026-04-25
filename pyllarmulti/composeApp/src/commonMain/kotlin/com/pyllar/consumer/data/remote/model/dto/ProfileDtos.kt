package com.pyllar.consumer.data.remote.model.dto

data class ProfileRequestDto(
    val userId: String
)

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
    val deletionMessage: String? = null
)

