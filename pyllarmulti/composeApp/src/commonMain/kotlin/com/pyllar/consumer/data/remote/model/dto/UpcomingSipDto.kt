package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpcomingSipDto(
    val sipId: String?,
    val fundId: String?,
    val fundName: String?,
    val amount: Double?,
    val sipDate: String?,
    val status: String?
)
