package com.pyllar.consumer.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class PanFetchRequestDto(
    val mobileNumber: String
)

@Serializable
data class PanFetchResponseDto(
    val success: Boolean,
    val status: String,
    val message: String?,
    val data: PanFetchDataDto?
)

@Serializable
data class PanFetchDataDto(
    val prefillId: Long,
    val status: String, // OTP_GENERATED, ALREADY_VERIFIED
    val message: String?,
    val providerId: String? = null,
    val fullName: String? = null,
    val panNumber: String? = null
)

@Serializable
data class PanVerifyOtpRequestDto(
    val mobileNumber: String,
    val prefillId: Long,
    val otp: String // Change variable name to match JSON payload "otp" not "otpCode"
)

@Serializable
data class PanVerifyOtpResponseDto(
    val success: Boolean,
    val status: String,
    val message: String?,
    val data: PanVerifyOtpDataDto?
)

@Serializable
data class PanVerifyOtpDataDto(
    val status: String,
    val providerId: String?,
    val personalDetails: PersonalDetailsDto?,
    val panDetails: PanDetailsDto?
)

@Serializable
data class PersonalDetailsDto(
    val fullName: String?,
    val gender: String?,
    val dateOfBirth: String?
)

@Serializable
data class PanDetailsDto(
    val panNumber: String?,
    val status: String?
)
