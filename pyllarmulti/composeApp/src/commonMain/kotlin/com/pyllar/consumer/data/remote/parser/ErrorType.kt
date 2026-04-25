package com.pyllar.consumer.data.remote.parser

/**
 * Types of errors that can occur during API communication.
 * Shared between platforms.
 */
enum class ErrorType {
    NETWORK_ERROR,
    PARSING_ERROR,
    VALIDATION_ERROR,
    AUTHENTICATION_ERROR,
    AUTHORIZATION_ERROR,
    NOT_FOUND_ERROR,
    SERVER_ERROR,
    RATE_LIMIT_ERROR,
    TIMEOUT_ERROR,
    FILE_NOT_FOUND,
    UNKNOWN_ERROR
}

