package com.pyllar.consumer.util

fun String.toUserFriendlyErrorMessage(): String {
    val trimmed = this.trim()
    if (trimmed.isEmpty()) {
        return "An unknown error occurred. Please try again."
    }

    val isRawHttpOrHtml = trimmed.startsWith("<!DOCTYPE", ignoreCase = true) ||
            trimmed.contains("<html", ignoreCase = true) ||
            trimmed.contains("http error", ignoreCase = true) ||
            trimmed.contains("status code", ignoreCase = true) ||
            trimmed.startsWith("{") || // JSON structure raw
            (trimmed.startsWith("[") && trimmed.endsWith("]")) ||
            trimmed.contains("502 Bad Gateway", ignoreCase = true) ||
            trimmed.contains("503 Service Unavailable", ignoreCase = true) ||
            trimmed.contains("504 Gateway Timeout", ignoreCase = true) ||
            trimmed.contains("500 Internal Server Error", ignoreCase = true) ||
            trimmed.contains("bad gateway", ignoreCase = true) ||
            trimmed.contains("gateway timeout", ignoreCase = true) ||
            trimmed.contains("internal server error", ignoreCase = true) ||
            trimmed.contains("service unavailable", ignoreCase = true) ||
            trimmed.contains("handshake failed", ignoreCase = true) ||
            trimmed.contains("handshake response", ignoreCase = true) ||
            trimmed.contains("secure channel", ignoreCase = true) ||
            trimmed.contains("SSLHandshakeException", ignoreCase = true) ||
            trimmed.contains("CertificateException", ignoreCase = true)

    if (isRawHttpOrHtml) {
        return "We are facing issues connecting to our servers. Please try again later."
    }

    val isNetwork = trimmed.contains("connect", ignoreCase = true) ||
            trimmed.contains("internet", ignoreCase = true) ||
            trimmed.contains("network", ignoreCase = true) ||
            trimmed.contains("timeout", ignoreCase = true) ||
            trimmed.contains("timed out", ignoreCase = true) ||
            trimmed.contains("offline", ignoreCase = true) ||
            trimmed.contains("unable to resolve host", ignoreCase = true) ||
            trimmed.contains("hostname could not be found", ignoreCase = true) ||
            trimmed.contains("broken pipe", ignoreCase = true) ||
            trimmed.contains("unexpected end of stream", ignoreCase = true) ||
            trimmed.contains("eofexception", ignoreCase = true) ||
            trimmed.contains("UnknownHostException", ignoreCase = true) ||
            trimmed.contains("SocketTimeoutException", ignoreCase = true) ||
            trimmed.contains("ConnectException", ignoreCase = true) ||
            trimmed.contains("IOException", ignoreCase = true)

    if (isNetwork) {
        return "Check your Internet connection and try again"
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

    val isGenericTechnicalError = trimmed.equals("Unknown error", ignoreCase = true) ||
            trimmed.equals("Empty response data", ignoreCase = true) ||
            trimmed.equals("Unexpected operation result", ignoreCase = true) ||
            trimmed.equals("Unexpected operation result.", ignoreCase = true) ||
            trimmed.contains("[url=", ignoreCase = true) ||
            trimmed.contains("kotlinx.serialization", ignoreCase = true) ||
            trimmed.contains("SerializationException", ignoreCase = true) ||
            trimmed.contains("JsonDecodingException", ignoreCase = true) ||
            trimmed.contains("for type with serial name", ignoreCase = true) ||
            trimmed.contains("Unexpected JSON token", ignoreCase = true) ||
            trimmed.contains("Unexpected end of input", ignoreCase = true) ||
            trimmed.contains("Expected start of the", ignoreCase = true) ||
            trimmed.contains("Encountered an unknown key", ignoreCase = true) ||
            trimmed.contains("Invalid AES key", ignoreCase = true) ||
            trimmed.contains("InvalidKeyException", ignoreCase = true) ||
            trimmed.contains("AEADBadTagException", ignoreCase = true) ||
            trimmed.contains("HMAC validation failed", ignoreCase = true) ||
            trimmed.contains("NullPointerException", ignoreCase = true) ||
            trimmed.contains("IllegalStateException", ignoreCase = true) ||
            trimmed.contains("IllegalArgumentException", ignoreCase = true) ||
            trimmed.contains("ClassCastException", ignoreCase = true) ||
            trimmed.contains("java.lang.", ignoreCase = true) ||
            trimmed.contains("java.net.", ignoreCase = true) ||
            trimmed.contains("javax.net.", ignoreCase = true) ||
            trimmed.contains("io.ktor.", ignoreCase = true)

    if (isGenericTechnicalError) {
        return "Something went wrong. Please try again."
    }

    val lowerMsg = trimmed.lowercase()
    return when {
        lowerMsg.contains("incorrect otp") -> "Incorrect OTP. Please try again."
        lowerMsg.contains("maximum attempts") -> "Maximum attempts reached. Please try again later."
        lowerMsg.contains("secure session expired") || lowerMsg.contains("session expired (401)") -> "Your session expired. Please try again."
        lowerMsg.contains("no units available") -> "You have no units available to withdraw."
        lowerMsg.contains("instant redemption range") || lowerMsg.contains("min_instant_redemption_amount") -> "The withdrawal amount is outside the allowed instant redemption range."
        lowerMsg.contains("greater than the min withdrawal amount") -> "Amount should be greater than the minimum withdrawal amount."
        lowerMsg.contains("less than the max redeemable amount") -> "Amount should be less than the maximum redeemable amount."
        lowerMsg.contains("requested isin has not been configured") || lowerMsg.contains("isin has not been configured") || lowerMsg.contains("failed to create mf redemption") || lowerMsg.contains("fintech api") || lowerMsg.contains("failed to create redemption") -> "Failed to create redemption"
        else -> trimmed
    }
}
