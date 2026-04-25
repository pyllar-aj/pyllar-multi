package com.pyllar.consumer.data.remote.crypto

import com.pyllar.consumer.data.remote.model.crypto.SecurePayloadEnvelopeDto

expect class HandshakeContext

expect class SecurePayloadCrypto() {
    fun encrypt(plaintext: ByteArray, session: SecureSessionData): SecurePayloadEnvelopeDto
    fun decrypt(envelope: SecurePayloadEnvelopeDto, session: SecureSessionData): ByteArray
    fun createFreshHeaders(session: SecureSessionData): Pair<String, String>
    
    fun generateHandshakeRequest(clientSessionId: String, platform: String): Pair<com.pyllar.consumer.data.remote.model.crypto.SecureHandshakeRequestDto, HandshakeContext>
    fun deriveKeysAndSession(response: com.pyllar.consumer.data.remote.model.crypto.SecureHandshakeResponseDto, context: HandshakeContext, clientSessionId: String): SecureSessionData
}

class SecureChannelException(message: String, cause: Throwable? = null) : Exception(message, cause)
