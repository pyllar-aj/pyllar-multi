package com.pyllar.consumer.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class ResponseMetadata(
    val timestamp: Long,
    val requestId: String? = null,
    val version: String? = null
)

