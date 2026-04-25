package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Field-level validation error information from API responses.
 */
@Serializable
data class FieldError(
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

