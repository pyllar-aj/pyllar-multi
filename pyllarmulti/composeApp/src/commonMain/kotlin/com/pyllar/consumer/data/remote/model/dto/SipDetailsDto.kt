package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class SipDetailsDto(
    val sipId: String?,
    val amount: Double?,
    val frequency: String?,
    val startDate: String?,
    val endDate: String?,
    val status: String?,
    val nextSipDate: String?,
    val totalInstallments: Int?,
    val completedInstallments: Int?
)
