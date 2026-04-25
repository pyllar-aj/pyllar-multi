package com.pyllar.consumer.data.remote.model.crypto

import kotlinx.serialization.Serializable

@Serializable
data class SecureHandshakeResponseDto(
    val handshakeId: String,
    val serverPublicKey: String,
    val serverNonce: String,
    val expiresAt: String
)
