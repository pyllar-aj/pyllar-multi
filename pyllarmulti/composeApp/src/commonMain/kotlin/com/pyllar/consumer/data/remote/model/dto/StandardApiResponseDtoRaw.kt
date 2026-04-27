package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Non-generic version of StandardApiResponseDto for situations where we need
 * to work with an untyped payload (e.g. logging, debugging, generic handling).
 *
 * Kotlin Multiplatform version using kotlinx.serialization – no Retrofit/Moshi.
 */
@Serializable
data class StandardApiResponseDtoRaw(
    @SerialName("status")
    val status: String = "",

    @SerialName("message")
    val message: String? = null,

    @SerialName("data")
    val data: JsonElement? = null,

    @SerialName("errors")
    val errors: List<FieldError>? = null,

    @SerialName("navigation")
    val navigation: NavigationInfo? = null,
    // metadata intentionally omitted in KMP build
) {
    val isSuccess: Boolean
        get() = status.equals("SUCCESS", ignoreCase = true) || status.equals("ACCEPTED", ignoreCase = true)

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
