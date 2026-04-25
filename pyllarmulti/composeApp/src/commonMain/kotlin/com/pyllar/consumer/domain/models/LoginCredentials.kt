package com.pyllar.consumer.domain.models

data class LoginCredentials(
    var phoneNumber: String?,
    var password: String?
)

data class RegistrationCredentials(
    var phoneNumber: String?,
    var password: String?
)

