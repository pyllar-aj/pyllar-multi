package com.pyllar.consumer.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PanFetchRequestDto(
    @SerialName("mobileNumber")
    val mobileNumber: String,
    @SerialName("force")
    val force: Boolean = false
)

@Serializable
data class PanFetchResponseDto(
    @SerialName("success")
    val success: Boolean,
    @SerialName("status")
    val status: String,
    @SerialName("message")
    val message: String?,
    @SerialName("data")
    val data: PanFetchDataDto?
)

@Serializable
data class PanFetchDataDto(
    @SerialName("prefillId")
    val prefillId: Long,
    @SerialName("status")
    val status: String, // OTP_GENERATED, ALREADY_VERIFIED
    @SerialName("message")
    val message: String?,
    @SerialName("providerId")
    val providerId: String? = null,
    @SerialName("fullName")
    val fullName: String? = null,
    @SerialName("panNumber")
    val panNumber: String? = null
)

@Serializable
data class PanVerifyOtpRequestDto(
    @SerialName("mobileNumber")
    val mobileNumber: String,
    @SerialName("prefillId")
    val prefillId: Long,
    @SerialName("otp")
    val otp: String
)

@Serializable
data class PanVerifyOtpResponseDto(
    @SerialName("success")
    val success: Boolean,
    @SerialName("status")
    val status: String,
    @SerialName("message")
    val message: String? = null,
    @SerialName("data")
    val data: PanVerifyOtpDataDto? = null
)

@Serializable
data class PanVerifyOtpDataDto(
    @SerialName("status")
    val status: String,
    @SerialName("providerId")
    val providerId: String? = null,
    @SerialName("personalDetails")
    val personalDetails: PersonalDetailsDto? = null,
    @SerialName("panDetails")
    val panDetails: PanDetailsDto? = null
)

@Serializable
data class PersonalDetailsDto(
    @SerialName("fullName")
    val fullName: String? = null,
    @SerialName("gender")
    val gender: String? = null,
    @SerialName("dateOfBirth")
    val dateOfBirth: String? = null
)

@Serializable
data class PanDetailsDto(
    @SerialName("panNumber")
    val panNumber: String? = null,
    @SerialName("status")
    val status: String? = null
)

