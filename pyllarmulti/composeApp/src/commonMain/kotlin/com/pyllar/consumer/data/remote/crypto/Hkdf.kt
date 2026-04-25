package com.pyllar.consumer.data.remote.crypto

expect object Hkdf {
    fun deriveKey(ikm: ByteArray, salt: ByteArray, info: ByteArray, outLengthBytes: Int): ByteArray
}
