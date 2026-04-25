package com.pyllar.consumer.data.remote.parser

import com.pyllar.consumer.data.remote.model.dto.FieldError
import com.pyllar.consumer.data.remote.model.dto.NavigationInfo

/**
 * Represents the result of parsing an API response in a platform-agnostic way.
 * Kept for compatibility with existing logic; new code can use Resource directly.
 */
sealed class ParsedResponse<T> {
    data class Success<T>(
        val data: T?,
        val navigation: NavigationInfo? = null,
        val fieldErrors: List<FieldError>? = null
    ) : ParsedResponse<T>()

    data class Error<T>(
        val message: String,
        val navigation: NavigationInfo? = null,
        val fieldErrors: List<FieldError>? = null,
        val errorType: ErrorType = ErrorType.UNKNOWN_ERROR
    ) : ParsedResponse<T>()
}

