package com.pyllar.consumer.data.remote.crypto

expect object DateTimeUtils {
    fun getCurrentIso8601(): String
    fun isExpired(iso8601DateString: String): Boolean
}
