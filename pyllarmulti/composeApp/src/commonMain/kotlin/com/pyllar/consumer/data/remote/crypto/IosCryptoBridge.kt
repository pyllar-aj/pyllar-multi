package com.pyllar.consumer.data.remote.crypto

interface IosCryptoBridge {
    fun generateEcdhKeyPair(): EcdhKeyResult
    fun deriveSharedSecret(privateKeyOpaque: Any, serverPublicKey: ByteArray): ByteArray
    fun encryptAesGcm(plaintext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray
    fun decryptAesGcm(ciphertext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray
    fun computeHmacSha256(data: ByteArray, key: ByteArray): ByteArray
    
    // Keychain support
    fun saveToKeychain(key: String, value: String): Boolean
    fun loadFromKeychain(key: String): String?
    fun deleteFromKeychain(key: String)
}

data class EcdhKeyResult(
    val publicKey: ByteArray,
    val privateKeyOpaque: Any
)

object SwiftCryptoScope {
    var bridge: IosCryptoBridge? = null
}
