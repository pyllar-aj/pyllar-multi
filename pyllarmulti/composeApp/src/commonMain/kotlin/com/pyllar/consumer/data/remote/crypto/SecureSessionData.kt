package com.pyllar.consumer.data.remote.crypto



data class SecureSessionData(
    val handshakeId: String,
    val encryptionKey: ByteArray,
    val hmacKey: ByteArray,
    val expiresAt: String,
    val clientSessionId: String
) {
    fun isExpired(): Boolean {
        return DateTimeUtils.isExpired(expiresAt)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as SecureSessionData

        if (handshakeId != other.handshakeId) return false
        if (!encryptionKey.contentEquals(other.encryptionKey)) return false
        if (!hmacKey.contentEquals(other.hmacKey)) return false
        if (expiresAt != other.expiresAt) return false
        if (clientSessionId != other.clientSessionId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = handshakeId.hashCode()
        result = 31 * result + encryptionKey.contentHashCode()
        result = 31 * result + hmacKey.contentHashCode()
        result = 31 * result + expiresAt.hashCode()
        result = 31 * result + clientSessionId.hashCode()
        return result
    }
}
