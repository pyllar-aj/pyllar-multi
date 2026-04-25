package com.pyllar.consumer.data.remote.model.crypto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SecurePayloadEnvelopeDto(
    @SerialName("handshake_id")
    val handshakeId: String,
    val iv: String,
    val ciphertext: String,
    val hmac: String,
    @SerialName("timestamp_utc")
    val timestampUtc: String
)
