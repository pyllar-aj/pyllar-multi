package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class SipPerformanceDto(
    val totalSipReturns: Double?,
    val sipReturnsPercentage: Double?,
    val averageSipXirr: Double?,
    val bestPerformingSip: String?,
    val worstPerformingSip: String?,
    val consistencyScore: Double?
)
