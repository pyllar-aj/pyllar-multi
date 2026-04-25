package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Standard API Response DTO that wraps all API responses with consistent structure.
 * Kotlin Multiplatform version using kotlinx.serialization.
 */
@Serializable
data class StandardApiResponseDto<T>(
    @SerialName("status")
    val status: String = "",
    @SerialName("message")
    val message: String? = null,
    @SerialName("data")
    val data: T? = null,
    @SerialName("errors")
    val errors: List<FieldError>? = null,
    @SerialName("navigation")
    val navigation: NavigationInfo? = null,
    // metadata intentionally omitted in KMP build; use StandardApiResponseDtoRaw if needed
) {
    val isSuccess: Boolean
        get() = status.equals("SUCCESS", ignoreCase = true)

    val hasValidationErrors: Boolean
        get() = status.equals("VALIDATION_ERROR", ignoreCase = true) && !errors.isNullOrEmpty()

    val isAuthenticationError: Boolean
        get() = status.equals("UNAUTHORIZED", ignoreCase = true)

    val isServerError: Boolean
        get() = status.equals("SERVER_ERROR", ignoreCase = true)

    val isNetworkError: Boolean
        get() = status.equals("NETWORK_ERROR", ignoreCase = true)

    val isNotFound: Boolean
        get() = status.equals("NOT_FOUND", ignoreCase = true)

    val responseStatus: ResponseStatus
        get() = ResponseStatus.fromString(status)
}

enum class ResponseStatus {
    SUCCESS,
    ERROR,
    VALIDATION_ERROR,
    UNAUTHORIZED,
    NOT_FOUND,
    SERVER_ERROR,
    NETWORK_ERROR;

    companion object {
        fun fromString(status: String): ResponseStatus {
            return when (status.uppercase()) {
                "SUCCESS" -> SUCCESS
                "ERROR" -> ERROR
                "VALIDATION_ERROR" -> VALIDATION_ERROR
                "UNAUTHORIZED" -> UNAUTHORIZED
                "NOT_FOUND" -> NOT_FOUND
                "SERVER_ERROR" -> SERVER_ERROR
                "NETWORK_ERROR" -> NETWORK_ERROR
                else -> ERROR
            }
        }
    }
}

