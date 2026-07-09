package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class CreditBureauLookupResponseDto(
    @SerialName("fullName")
    val fullName: String? = null,
    @SerialName("dob")
    val dob: String? = null,
    @SerialName("panNumber")
    val panNumber: String? = null,
    @SerialName("gender")
    val gender: String? = null,
    @SerialName("occupation")
    val occupation: String? = null,
    @SerialName("verified")
    val verified: Boolean = false
)
