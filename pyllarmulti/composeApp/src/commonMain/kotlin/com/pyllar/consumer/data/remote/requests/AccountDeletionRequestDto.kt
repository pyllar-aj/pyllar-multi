package com.pyllar.consumer.data.remote.requests

data class AccountDeletionRequestDto(
    val userId: String,
    val notes: String? = null
)

