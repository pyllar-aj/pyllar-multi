package com.pyllar.consumer.data.remote.crypto

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.pyllar.consumer.data.remote.model.crypto.SecurePayloadEnvelopeDto

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.security.GeneralSecurityException
import java.time.Duration
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.xor
import java.security.SecureRandom
import java.security.KeyPairGenerator
import java.security.spec.ECGenParameterSpec
import java.security.KeyFactory
import java.security.spec.X509EncodedKeySpec
import javax.crypto.KeyAgreement
import com.pyllar.consumer.data.remote.model.crypto.SecureHandshakeRequestDto
import com.pyllar.consumer.data.remote.model.crypto.SecureHandshakeResponseDto
import android.util.Base64

actual class HandshakeContext(
    val keyPair: java.security.KeyPair,
    val clientNonce: ByteArray
)

actual class SecurePayloadCrypto actual constructor() {
    private val secureRandom = SecureRandom()

    actual fun encrypt(plaintext: ByteArray, session: SecureSessionData): SecurePayloadEnvelopeDto {
        val iv = ByteArray(12)
        secureRandom.nextBytes(iv)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(session.encryptionKey, "AES"), GCMParameterSpec(128, iv))
        val ciphertext = cipher.doFinal(plaintext)
        val timestamp = java.time.Instant.now().toString()
        val hmac = computeHmac(session, timestamp, iv, ciphertext)
        return SecurePayloadEnvelopeDto(
            handshakeId = session.handshakeId,
            iv = Base64.encodeToString(iv, Base64.NO_WRAP),
            ciphertext = Base64.encodeToString(ciphertext, Base64.NO_WRAP),
            hmac = Base64.encodeToString(hmac, Base64.NO_WRAP),
            timestampUtc = timestamp
        )
    }

    actual fun decrypt(envelope: SecurePayloadEnvelopeDto, session: SecureSessionData): ByteArray {
        val iv = Base64.decode(envelope.iv, Base64.NO_WRAP)
        val ciphertext = Base64.decode(envelope.ciphertext, Base64.NO_WRAP)
        val expectedHmac = Base64.decode(envelope.hmac, Base64.NO_WRAP)

        val computed = computeHmac(session, envelope.timestampUtc, iv, ciphertext)
        if (!computed.contentEquals(expectedHmac)) {
            throw SecureChannelException("HMAC validation failed")
        }

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(session.encryptionKey, "AES"), GCMParameterSpec(128, iv))
        return cipher.doFinal(ciphertext)
    }

    actual fun createFreshHeaders(session: SecureSessionData): Pair<String, String> {
        return session.handshakeId to java.time.Instant.now().toString()
    }

    private fun computeHmac(session: SecureSessionData, timestamp: String, iv: ByteArray, ciphertext: ByteArray): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(session.hmacKey, "HmacSHA256"))
        mac.update(session.handshakeId.toByteArray(Charsets.UTF_8))
        mac.update(iv)
        mac.update(timestamp.toByteArray(Charsets.UTF_8))
        mac.update(ciphertext)
        return mac.doFinal()
    }

    actual fun generateHandshakeRequest(clientSessionId: String, platform: String): Pair<SecureHandshakeRequestDto, HandshakeContext> {
        val generator = KeyPairGenerator.getInstance("EC")
        generator.initialize(ECGenParameterSpec("secp256r1"))
        val keyPair = generator.generateKeyPair()
        
        val clientNonce = ByteArray(16)
        secureRandom.nextBytes(clientNonce)

        val request = SecureHandshakeRequestDto(
            clientPublicKey = Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP),
            clientNonce = Base64.encodeToString(clientNonce, Base64.NO_WRAP),
            clientSessionId = clientSessionId,
            platform = platform
        )
        return request to HandshakeContext(keyPair, clientNonce)
    }

    actual fun deriveKeysAndSession(response: SecureHandshakeResponseDto, context: HandshakeContext, clientSessionId: String): SecureSessionData {
        val serverPublicKeyBytes = Base64.decode(response.serverPublicKey, Base64.NO_WRAP)
        val serverNonce = Base64.decode(response.serverNonce, Base64.NO_WRAP)

        val keyFactory = KeyFactory.getInstance("EC")
        val serverSpec = X509EncodedKeySpec(serverPublicKeyBytes)
        val serverKey = keyFactory.generatePublic(serverSpec)
        
        val agreement = KeyAgreement.getInstance("ECDH")
        agreement.init(context.keyPair.private)
        agreement.doPhase(serverKey, true)
        val sharedSecret = agreement.generateSecret()

        val salt = context.clientNonce + serverNonce
        val derivedKey = Hkdf.deriveKey(sharedSecret, salt, "pyllar-secure-payload".toByteArray(), 64)

        return SecureSessionData(
            handshakeId = response.handshakeId,
            encryptionKey = derivedKey.copyOfRange(0, 32),
            hmacKey = derivedKey.copyOfRange(32, 64),
            expiresAt = response.expiresAt,
            clientSessionId = clientSessionId
        )
    }
}

actual object Hkdf {
    // Basic HKDF-SHA256 implementation using javax.crypto.Mac
    actual fun deriveKey(ikm: ByteArray, salt: ByteArray, info: ByteArray, outLengthBytes: Int): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        
        // Extract
        val actualSalt = if (salt.isEmpty()) ByteArray(32) else salt
        mac.init(SecretKeySpec(actualSalt, "HmacSHA256"))
        val prk = mac.doFinal(ikm)
        
        // Expand
        mac.init(SecretKeySpec(prk, "HmacSHA256"))
        var t = ByteArray(0)
        val okm = java.io.ByteArrayOutputStream()
        var i = 1
        while (okm.size() < outLengthBytes) {
            mac.update(t)
            mac.update(info)
            mac.update(i.toByte())
            t = mac.doFinal()
            okm.write(t)
            i++
        }
        return okm.toByteArray().copyOf(outLengthBytes)
    }
}

class AndroidSecureSessionStore : SecureSessionStore, KoinComponent {
    private val context: Context by inject()

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "secure_session_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    override fun getClientSessionId(): String {
        return prefs.getString("clientSessionId", null) ?: run {
            val newId = UUID.randomUUID().toString()
            prefs.edit().putString("clientSessionId", newId).apply()
            newId
        }
    }

    override fun saveSession(session: SecureSessionData) {
        prefs.edit()
            .putString("handshakeId", session.handshakeId)
            .putString("encryptionKey", Base64.encodeToString(session.encryptionKey, Base64.NO_WRAP))
            .putString("hmacKey", Base64.encodeToString(session.hmacKey, Base64.NO_WRAP))
            .putString("expiresAt", session.expiresAt)
            .putString("clientSessionId", session.clientSessionId)
            .apply()
    }

    override fun getSession(): SecureSessionData? {
        val handshakeId = prefs.getString("handshakeId", null) ?: return null
        val encKeyStr = prefs.getString("encryptionKey", null) ?: return null
        val hmacKeyStr = prefs.getString("hmacKey", null) ?: return null
        val expiresAtStr = prefs.getString("expiresAt", null) ?: return null
        val clientSessionId = prefs.getString("clientSessionId", null) ?: return null
        
        return SecureSessionData(
            handshakeId = handshakeId,
            encryptionKey = Base64.decode(encKeyStr, Base64.NO_WRAP),
            hmacKey = Base64.decode(hmacKeyStr, Base64.NO_WRAP),
            expiresAt = expiresAtStr,
            clientSessionId = clientSessionId
        )
    }

    override fun clear() {
        prefs.edit().clear().apply()
    }
}

actual fun createSecureSessionStore(): SecureSessionStore = AndroidSecureSessionStore()
