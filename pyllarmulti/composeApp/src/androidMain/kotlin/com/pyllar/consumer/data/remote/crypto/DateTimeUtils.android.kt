package com.pyllar.consumer.data.remote.crypto

import java.time.Instant

actual object DateTimeUtils {
    actual fun getCurrentIso8601(): String {
        return Instant.now().toString()
    }

    actual fun isExpired(iso8601DateString: String): Boolean {
        return try {
            val expiresAt = Instant.parse(iso8601DateString)
            Instant.now().isAfter(expiresAt)
        } catch (e: Exception) {
            true // If unparseable, treat as expired to force re-handshake
        }
    }
}
