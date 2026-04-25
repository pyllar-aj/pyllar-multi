package com.pyllar.consumer.domain.models

data class UpdateEmailResponse(
    val email: String,
    val message: String?,
    val isUpdated: Boolean,
    val isNewUser: Boolean,
    val isMismatch: Boolean,
    val isError: Boolean,
    val preverify: Boolean = false,
    val screen: String? = null
)
