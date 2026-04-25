package com.pyllar.consumer.domain.models

data class AuthUserDTO(
    val token: String,
    val userId: String,
    val phoneNumber: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val newUser: Boolean
)

