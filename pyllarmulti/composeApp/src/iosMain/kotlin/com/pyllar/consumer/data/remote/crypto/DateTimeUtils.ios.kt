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
            var expiresAt = formatter.dateFromString(iso8601DateString)
            if (expiresAt == null) {
                // Try parsing with fractional seconds
                formatter.formatOptions = platform.Foundation.NSISO8601DateFormatWithInternetDateTime or platform.Foundation.NSISO8601DateFormatWithFractionalSeconds
                expiresAt = formatter.dateFromString(iso8601DateString)
            }
            if (expiresAt == null) {
                return true
            }
            val now = NSDate()
            now.compare(expiresAt) == platform.Foundation.NSOrderedDescending
        } catch (e: Exception) {
            true
        }
    }
}
