package com.pyllar.consumer.util

object Log {
    fun d(tag: String, message: String) {
        // No-op in common; platform-specific logging can wrap this if needed
    }

    fun e(tag: String, message: String) {
        // No-op in common
    }

    fun w(tag: String, message: String) {
        // No-op in common
    }
}

