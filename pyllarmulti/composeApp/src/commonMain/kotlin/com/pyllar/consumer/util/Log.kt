package com.pyllar.consumer.util

object Log {
    fun d(tag: String, message: String) {
        platformLog("$tag: [DEBUG] $message")
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        platformLog("$tag: [ERROR] $message ${throwable?.message ?: ""}")
    }

    fun w(tag: String, message: String) {
        platformLog("$tag: [WARN] $message")
    }
}

