package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class UpiVpaBankDetailsResponseDto(
    @SerialName("accountNumber")
    val accountNumber: String? = null,
    @SerialName("ifscCode")
    val ifscCode: String? = null
)
