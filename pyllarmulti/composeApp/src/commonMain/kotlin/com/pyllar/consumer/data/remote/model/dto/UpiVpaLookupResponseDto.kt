package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class UpiVpaLookupResponseDto(
    @SerialName("upiVpa")
    val upiVpa: String? = null,
    @SerialName("nameAsPerBank")
    val nameAsPerBank: String? = null,
    @SerialName("verified")
    val verified: Boolean = false,
    @SerialName("dob")
    val dob: String? = null,
    @SerialName("panNumber")
    val panNumber: String? = null
)
