package com.pyllar.consumer.data.remote.model.dto

import com.pyllar.consumer.data.remote.model.User
import kotlinx.serialization.Serializable

@Serializable
data class AuthUserDTO(
    var status: String? = null,
    var message: String? = null,
    var data: User? = null
) {
    val success: Boolean
        get() = status == "SUCCESS"
}
