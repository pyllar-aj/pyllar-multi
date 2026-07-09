package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class UpiVpaLookupRequest(
    @SerialName("upiVpa")
    val upiVpa: String,
    @SerialName("userId")
    val userId: String
)
