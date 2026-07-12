package com.pyllar.consumer.util

fun String.toUserFriendlyErrorMessage(): String {
    val trimmed = this.trim()
    if (trimmed.isEmpty()) {
        return "An unknown error occurred. Please try again."
    }

    val isNetwork = trimmed.contains("connect", ignoreCase = true) ||
            trimmed.contains("internet", ignoreCase = true) ||
            trimmed.contains("network", ignoreCase = true) ||
            trimmed.contains("timeout", ignoreCase = true) ||
            trimmed.contains("offline", ignoreCase = true) ||
            trimmed.contains("UnknownHostException", ignoreCase = true) ||
            trimmed.contains("SocketTimeoutException", ignoreCase = true) ||
            trimmed.contains("ConnectException", ignoreCase = true) ||
            trimmed.contains("IOException", ignoreCase = true)

    if (isNetwork) {
        return "Check your Internet connection and try again"
    }

    val isRawHttpOrHtml = trimmed.startsWith("<!DOCTYPE", ignoreCase = true) ||
            trimmed.contains("<html", ignoreCase = true) ||
            trimmed.contains("http error", ignoreCase = true) ||
            trimmed.contains("status code", ignoreCase = true) ||
            trimmed.startsWith("{") || // JSON structure raw
            trimmed.contains("502 Bad Gateway", ignoreCase = true) ||
            trimmed.contains("504 Gateway Timeout", ignoreCase = true) ||
            trimmed.contains("500 Internal Server Error", ignoreCase = true)

    if (isRawHttpOrHtml) {
        return "We are facing issues connecting to our servers. Please try again later."
    }

    return trimmed
}
