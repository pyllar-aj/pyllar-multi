package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response of GET /api/kyc/onboarding/options: the KYC providers offered on the "Start KYC" sheet. */
@Serializable
data class KycOptionsResponseDto(
    @SerialName("options")
    val options: List<KycOptionDto> = emptyList()
)

@Serializable
data class KycOptionDto(
    /** DIGILOCKER or CAMS_AB. */
    @SerialName("provider")
    val provider: String? = null,
    @SerialName("label")
    val label: String? = null,
    /** NATIVE (existing in-app flow) or WEBVIEW (open [url]). */
    @SerialName("type")
    val type: String? = null,
    @SerialName("enabled")
    val enabled: Boolean = false,
    @SerialName("url")
    val url: String? = null
)

/** Response of POST /api/kyc/onboarding/cams/started. */
@Serializable
data class CamsStartedResponseDto(
    @SerialName("kycAttemptId")
    val kycAttemptId: String? = null
)

