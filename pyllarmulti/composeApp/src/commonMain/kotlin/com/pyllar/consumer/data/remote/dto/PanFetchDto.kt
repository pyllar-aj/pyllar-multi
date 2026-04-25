package com.pyllar.consumer.data.remote.dto

data class PanFetchRequestDto(
    val mobileNumber: String
)

data class PanFetchResponseDto(
    val success: Boolean,
    val status: String,
    val message: String?,
    val data: PanFetchDataDto?
)

data class PanFetchDataDto(
    val prefillId: Long,
    val status: String, // OTP_GENERATED, ALREADY_VERIFIED
    val message: String?,
    val providerId: String? = null,
    val fullName: String? = null,
    val panNumber: String? = null
)

data class PanVerifyOtpRequestDto(
    val mobileNumber: String,
    val prefillId: Long,
    val otp: String // Change variable name to match JSON payload "otp" not "otpCode"
)

data class PanVerifyOtpResponseDto(
    val success: Boolean,
    val status: String,
    val message: String?,
    val data: PanVerifyOtpDataDto?
)

data class PanVerifyOtpDataDto(
    val status: String,
    val providerId: String?,
    val personalDetails: PersonalDetailsDto?,
    val panDetails: PanDetailsDto?
)

data class PersonalDetailsDto(
    val fullName: String?,
    val gender: String?,
    val dateOfBirth: String?
)

data class PanDetailsDto(
    val panNumber: String?,
    val status: String?
)
