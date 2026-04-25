package com.pyllar.consumer.util

import com.pyllar.consumer.data.remote.model.dto.FieldError
import com.pyllar.consumer.data.remote.model.dto.NavigationInfo
import com.pyllar.consumer.data.remote.parser.ErrorType

enum class Status {
    SUCCESS,
    ERROR,
    LOADING
}

sealed class Resource<T>(
    val data: T? = null,
    val message: String? = null,
    val navigation: NavigationInfo? = null,
    val fieldErrors: List<FieldError>? = null,
    val errorType: ErrorType? = null
) {
    class Success<T>(
        data: T?,
        navigation: NavigationInfo? = null,
        fieldErrors: List<FieldError>? = null
    ) : Resource<T>(data = data, navigation = navigation, fieldErrors = fieldErrors)

    class Error<T>(
        message: String,
        navigation: NavigationInfo? = null,
        fieldErrors: List<FieldError>? = null,
        errorType: ErrorType? = null
    ) : Resource<T>(
        message = message,
        navigation = navigation,
        fieldErrors = fieldErrors,
        errorType = errorType
    ) {
        fun copy(
            message: String? = this.message,
            navigation: NavigationInfo? = this.navigation,
            fieldErrors: List<FieldError>? = this.fieldErrors,
            errorType: ErrorType? = this.errorType
        ): Error<T> {
            return Error(message ?: "", navigation, fieldErrors, errorType)
        }
    }

    class Loading<T> : Resource<T>()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
    val isLoading: Boolean get() = this is Loading

    val status: Status get() = when (this) {
        is Success -> Status.SUCCESS
        is Error -> Status.ERROR
        is Loading -> Status.LOADING
    }

    val hasNavigation: Boolean get() = navigation != null
    val hasFieldErrors: Boolean get() = !fieldErrors.isNullOrEmpty()

    val isForceUpdate: Boolean
        get() = navigation?.action == com.pyllar.consumer.data.remote.model.dto.NavigationAction.FORCE_UPDATE

    val isNetworkError: Boolean get() = errorType == ErrorType.NETWORK_ERROR
    val isServerError: Boolean get() = errorType == ErrorType.SERVER_ERROR
    val isValidationError: Boolean get() = errorType == ErrorType.VALIDATION_ERROR
    val isAuthenticationError: Boolean get() = errorType == ErrorType.AUTHENTICATION_ERROR
}

