package com.pyllar.consumer.data.remote.model.crypto

import kotlinx.serialization.Serializable

@Serializable
data class SecureHandshakeRequestDto(
    val clientPublicKey: String,
    val clientNonce: String,
    val clientSessionId: String,
    val platform: String = "android"
)
