package com.pyllar.consumer.data.remote.requests

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class UserDetailsFetchRequest(
    @SerialName("phoneNumber")
    val phoneNumber: String,
    @SerialName("name")
    val name: String
)
