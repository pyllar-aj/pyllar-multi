package com.pyllar.consumer.data.remote.crypto

import com.pyllar.consumer.data.remote.model.crypto.SecurePayloadEnvelopeDto
import com.pyllar.consumer.data.remote.model.crypto.SecureHandshakeRequestDto
import com.pyllar.consumer.data.remote.model.crypto.SecureHandshakeResponseDto

import platform.Foundation.NSUUID
import platform.posix.arc4random
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import com.pyllar.consumer.util.platformLog

actual class HandshakeContext(
    val privateKeyOpaque: Any,
    val clientNonce: ByteArray
)

@OptIn(ExperimentalEncodingApi::class)
actual class SecurePayloadCrypto actual constructor() {
    
    private val bridge: IosCryptoBridge
        get() = SwiftCryptoScope.bridge ?: throw IllegalStateException("IosCryptoBridge not initialized by iOS app!")

    actual fun encrypt(plaintext: ByteArray, session: SecureSessionData): SecurePayloadEnvelopeDto {
        // Generate random 12-byte IV for AES-GCM
        val iv = ByteArray(12) { arc4random().toByte() }
        
        val ciphertext = bridge.encryptAesGcm(plaintext, session.encryptionKey, iv)
        val timestamp = DateTimeUtils.getCurrentIso8601()
        val hmac = computeHmac(session, timestamp, iv, ciphertext)
        
        return SecurePayloadEnvelopeDto(
            handshakeId = session.handshakeId,
            iv = Base64.encode(iv),
            ciphertext = Base64.encode(ciphertext),
            hmac = Base64.encode(hmac),
            timestampUtc = timestamp
        )
    }

    actual fun decrypt(envelope: SecurePayloadEnvelopeDto, session: SecureSessionData): ByteArray {
        val iv = Base64.decode(envelope.iv)
        val ciphertext = Base64.decode(envelope.ciphertext)
        val expectedHmac = Base64.decode(envelope.hmac)

        val computed = computeHmac(session, envelope.timestampUtc, iv, ciphertext)
        platformLog("HTTPSecure(Pyllar) HMAC Debug: handshakeId=${session.handshakeId}, timestamp=${envelope.timestampUtc}")
        platformLog("HTTPSecure(Pyllar) Expected HMAC: ${envelope.hmac}")
        platformLog("HTTPSecure(Pyllar) Computed HMAC: ${Base64.encode(computed)}")

        if (!computed.contentEquals(expectedHmac)) {
            throw SecureChannelException("HMAC validation failed")
        }

        return bridge.decryptAesGcm(ciphertext, session.encryptionKey, iv)
    }

    actual fun createFreshHeaders(session: SecureSessionData): Pair<String, String> {
        return session.handshakeId to DateTimeUtils.getCurrentIso8601()
    }

    private fun computeHmac(session: SecureSessionData, timestamp: String, iv: ByteArray, ciphertext: ByteArray): ByteArray {
        // Build the payload that needs to be HMAC'd
        val payloadToHash = mutableListOf<Byte>()
        payloadToHash.addAll(session.handshakeId.encodeToByteArray().toList())
        payloadToHash.addAll(iv.toList())
        payloadToHash.addAll(timestamp.encodeToByteArray().toList())
        payloadToHash.addAll(ciphertext.toList())
        
        return bridge.computeHmacSha256(payloadToHash.toByteArray(), session.hmacKey)
    }

    actual fun generateHandshakeRequest(clientSessionId: String, platform: String): Pair<SecureHandshakeRequestDto, HandshakeContext> {
        val ecdhResult = bridge.generateEcdhKeyPair()
        
        val clientNonce = ByteArray(16) { arc4random().toByte() }

        val request = SecureHandshakeRequestDto(
            clientPublicKey = Base64.encode(ecdhResult.publicKey),
            clientNonce = Base64.encode(clientNonce),
            clientSessionId = clientSessionId,
            platform = platform
        )
        return request to HandshakeContext(ecdhResult.privateKeyOpaque, clientNonce)
    }

    actual fun deriveKeysAndSession(response: SecureHandshakeResponseDto, context: HandshakeContext, clientSessionId: String): SecureSessionData {
        val serverPublicKeyBytes = Base64.decode(response.serverPublicKey)
        val serverNonce = Base64.decode(response.serverNonce)

        val sharedSecret = bridge.deriveSharedSecret(context.privateKeyOpaque, serverPublicKeyBytes)

        val salt = context.clientNonce + serverNonce
        val derivedKey = Hkdf.deriveKey(sharedSecret, salt, "pyllar-secure-payload".encodeToByteArray(), 64)

        return SecureSessionData(
            handshakeId = response.handshakeId,
            encryptionKey = derivedKey.copyOfRange(0, 32),
            hmacKey = derivedKey.copyOfRange(32, 64),
            expiresAt = response.expiresAt,
            clientSessionId = clientSessionId
        )
    }
}

@OptIn(ExperimentalEncodingApi::class)
actual object Hkdf {
    actual fun deriveKey(ikm: ByteArray, salt: ByteArray, info: ByteArray, outLengthBytes: Int): ByteArray {
        val bridge = SwiftCryptoScope.bridge ?: throw IllegalStateException("IosCryptoBridge not initialized by iOS app!")
        
        val actualSalt = if (salt.isEmpty()) ByteArray(32) else salt
        val prk = bridge.computeHmacSha256(ikm, actualSalt)
        
        var t = ByteArray(0)
        val okm = mutableListOf<Byte>()
        var i = 1
        while (okm.size < outLengthBytes) {
            val blockInput = t + info + byteArrayOf(i.toByte())
            t = bridge.computeHmacSha256(blockInput, prk)
            okm.addAll(t.toList())
            i++
        }
        return okm.toByteArray().copyOf(outLengthBytes)
    }
}

@OptIn(ExperimentalEncodingApi::class)
class IosSecureSessionStore : SecureSessionStore {
    
    private val bridge get() = SwiftCryptoScope.bridge

    override fun saveSession(session: SecureSessionData) {
        bridge?.saveToKeychain("secure_session_handshakeId", session.handshakeId)
        bridge?.saveToKeychain("secure_session_encryptionKey", Base64.encode(session.encryptionKey))
        bridge?.saveToKeychain("secure_session_hmacKey", Base64.encode(session.hmacKey))
        bridge?.saveToKeychain("secure_session_expiresAt", session.expiresAt)
        bridge?.saveToKeychain("secure_session_clientSessionId", session.clientSessionId)
    }

    override fun getSession(): SecureSessionData? {
        val handshakeId = bridge?.loadFromKeychain("secure_session_handshakeId") ?: return null
        val encKeyStr = bridge?.loadFromKeychain("secure_session_encryptionKey") ?: return null
        val hmacKeyStr = bridge?.loadFromKeychain("secure_session_hmacKey") ?: return null
        val expiresAtStr = bridge?.loadFromKeychain("secure_session_expiresAt") ?: return null
        val clientSessionId = bridge?.loadFromKeychain("secure_session_clientSessionId") ?: return null
        
        return SecureSessionData(
            handshakeId = handshakeId,
            encryptionKey = Base64.decode(encKeyStr),
            hmacKey = Base64.decode(hmacKeyStr),
            expiresAt = expiresAtStr,
            clientSessionId = clientSessionId
        )
    }

    override fun clear() {
        bridge?.deleteFromKeychain("secure_session_handshakeId")
        bridge?.deleteFromKeychain("secure_session_encryptionKey")
        bridge?.deleteFromKeychain("secure_session_hmacKey")
        bridge?.deleteFromKeychain("secure_session_expiresAt")
        // Don't remove clientSessionId, it should persist across handshakes based on Android logic
    }

    override fun getClientSessionId(): String {
        return bridge?.loadFromKeychain("secure_session_clientSessionId") ?: run {
            val newId = NSUUID.UUID().UUIDString
            bridge?.saveToKeychain("secure_session_clientSessionId", newId)
            newId
        }
    }
}

actual fun createSecureSessionStore(): SecureSessionStore = IosSecureSessionStore()
