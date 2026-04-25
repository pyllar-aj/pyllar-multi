package com.pyllar.consumer.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    @SerialName("id")
    val id: Long? = null,
    @SerialName("userId")
    val userId: String? = null,
    @SerialName("phoneNumber")
    val phoneNumber: String? = null,
    @SerialName("firstName")
    val firstName: String? = null,
    @SerialName("lastName")
    val lastName: String? = null,
    @SerialName("fullName")
    val fullName: String? = null,
    @SerialName("email")
    val email: String? = null,
    @SerialName("token")
    val token: String? = null,
    @SerialName("auth_token")
    val authToken: String? = null,
    @SerialName("newUser")
    val newUser: Boolean? = null
)

