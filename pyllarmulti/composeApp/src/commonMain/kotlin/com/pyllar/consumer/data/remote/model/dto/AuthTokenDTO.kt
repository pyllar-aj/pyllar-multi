package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuthTokenDTO(
    val status: String? = null,
    val message: String? = null,
    val data: AuthTokenData? = null
) {
    val success: Boolean
        get() = status == "SUCCESS"
}

@Serializable
data class AuthTokenData(
    @SerialName("registration_token")
    var t: String? = null,
    @SerialName("reference_id")
    var referenceId: String? = null,
    var token: String? = null,
    @SerialName("auth_token")
    var authToken: String? = null,
    @SerialName("user_id")
    var userId: String? = null,
    @SerialName("phone_number")
    var phoneNumber: String? = null
)
