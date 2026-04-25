package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ScreenDataResponseDto(
    val screenName: String,
    val data: Map<String, JsonElement> = emptyMap()
)
