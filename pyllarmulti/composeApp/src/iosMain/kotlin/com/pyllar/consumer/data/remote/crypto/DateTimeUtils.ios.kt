package com.pyllar.consumer.data.remote.crypto

import platform.Foundation.NSDate
import platform.Foundation.NSISO8601DateFormatter
import platform.Foundation.compare
import platform.Foundation.timeIntervalSince1970

actual object DateTimeUtils {
    actual fun getCurrentIso8601(): String {
        val formatter = NSISO8601DateFormatter()
        return formatter.stringFromDate(NSDate())
    }

    actual fun isExpired(iso8601DateString: String): Boolean {
        return try {
            val formatter = NSISO8601DateFormatter()
            val expiresAt = formatter.dateFromString(iso8601DateString) ?: return true
            val now = NSDate()
            now.compare(expiresAt) == platform.Foundation.NSOrderedDescending
        } catch (e: Exception) {
            true
        }
    }
}
