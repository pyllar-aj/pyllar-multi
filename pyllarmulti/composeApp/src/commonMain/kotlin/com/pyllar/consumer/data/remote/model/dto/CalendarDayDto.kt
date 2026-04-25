package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class CalendarDayDto(
    val day: Int?,
    val isInvested: Boolean?,
    val isToday: Boolean?,
    val amount: Double?,
    val status: String?
)
