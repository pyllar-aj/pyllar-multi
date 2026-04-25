package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Serializable representation of a field-level validation error.
 * Mirrors FieldError but kept as a DTO type for backwards compatibility.
 */
@Serializable
data class FieldErrorDto(
    @SerialName("field")
    val field: String,
    @SerialName("message")
    val message: String,
    @SerialName("code")
    val code: String? = null
) {
    val hasCode: Boolean
        get() = !code.isNullOrBlank()

    fun getDisplayMessage(): String {
        return if (hasCode) {
            "$message (Code: $code)"
        } else {
            message
        }
    }
}