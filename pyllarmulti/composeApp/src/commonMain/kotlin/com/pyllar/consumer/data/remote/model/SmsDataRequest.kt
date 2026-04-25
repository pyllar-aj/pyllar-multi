package com.pyllar.consumer.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SmsDataRequest(
    @SerialName("userId")
    val userId: String,
    @SerialName("messages")
    val messages: List<SmsMessage>
)

@Serializable
data class SmsMessage(
    @SerialName("address")
    val address: String,
    @SerialName("date")
    val date: Long,
    @SerialName("body")
    val body: String
)

