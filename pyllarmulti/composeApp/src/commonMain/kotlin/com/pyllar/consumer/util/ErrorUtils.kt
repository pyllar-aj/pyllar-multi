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

    val isSystemOrTechnicalError = trimmed.contains("unrecognized field", ignoreCase = true) ||
            trimmed.contains("jackson", ignoreCase = true) ||
            trimmed.contains("deserialization", ignoreCase = true) ||
            trimmed.contains("class com.pyllar", ignoreCase = true) ||
            trimmed.contains("streamreadfeature", ignoreCase = true) ||
            trimmed.contains("preverificationresponse", ignoreCase = true) ||
            (trimmed.contains("Failed to check investor readiness", ignoreCase = true) && trimmed.contains("field", ignoreCase = true))

    if (isSystemOrTechnicalError) {
        return "Failed to check readiness. Please try again later."
    }

    val lowerMsg = trimmed.lowercase()
    return when {
        lowerMsg.contains("incorrect otp") -> "Incorrect OTP. Please try again."
        lowerMsg.contains("maximum attempts") -> "Maximum attempts reached. Please try again later."
        lowerMsg.contains("no units available") -> "You have no units available to withdraw."
        lowerMsg.contains("instant redemption range") || lowerMsg.contains("min_instant_redemption_amount") -> "The withdrawal amount is outside the allowed instant redemption range."
        lowerMsg.contains("greater than the min withdrawal amount") -> "Amount should be greater than the minimum withdrawal amount."
        lowerMsg.contains("less than the max redeemable amount") -> "Amount should be less than the maximum redeemable amount."
        else -> trimmed
    }
}
