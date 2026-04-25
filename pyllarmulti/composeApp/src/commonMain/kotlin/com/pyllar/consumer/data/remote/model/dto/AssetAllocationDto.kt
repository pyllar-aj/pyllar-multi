package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class AssetAllocationDto(
    val equity: Double?,
    val debt: Double?,
    val hybrid: Double?,
    val liquid: Double?,
    val others: Double?
)
