package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class CreditBureauLookupRequest(
    @SerialName("mobile")
    val mobile: String,
    @SerialName("name")
    val name: String
)
