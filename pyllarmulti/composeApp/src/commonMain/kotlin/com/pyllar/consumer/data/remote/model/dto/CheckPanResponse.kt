package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class CheckPanResponse(
    var status: String? = null,
    var message: String? = null,
    var data: Boolean? = null
) {
    val success: Boolean
        get() = status == "SUCCESS"
}
