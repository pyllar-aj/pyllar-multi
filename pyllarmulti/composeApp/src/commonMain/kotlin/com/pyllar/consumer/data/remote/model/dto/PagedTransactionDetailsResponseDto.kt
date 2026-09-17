package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class PagedTransactionDetailsResponseDto(
    val transactions: List<PurchaseTransactionDto>? = null,
    val folioList: List<String>? = null,
    val investedAmount: Double? = null,
    val unitsAllotted: Double? = null,
    val totalValue: Double? = null,
    val withdrawableAmount: Double? = null,
    val inProgress: Double? = null,
    val nextBeforeDate: String? = null,
    val hasMore: Boolean = false
)
