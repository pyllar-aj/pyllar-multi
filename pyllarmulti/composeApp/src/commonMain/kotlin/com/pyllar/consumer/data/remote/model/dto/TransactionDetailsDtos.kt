package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class TransactionDetailsResponseDto(
    val purchasePlans: List<PurchasePlanWithTransactionsDto>?,
    val planSummaryDtos: List<PlanSummaryDto>?
)

@Serializable
data class PurchasePlanWithTransactionsDto(
    val purchasePlan: PurchasePlanDto?,
    val transactions: List<PurchaseTransactionDto>?,
    val folioList: List<String>?,
    val investedAmount: Double?,
    val unitsAllotted: Double?,
    val totalValue: Double?
)

@Serializable
data class PurchasePlanDto(
    val id: String?,
    val fintechPlanId: String?,
    val userId: String?,
    val userInvestmentPurposeId: String?,
    val folioNumber: String?,
    val scheme: String?,
    val amount: Double?,
    val frequency: String?,
    val numberOfInstallments: Int?,
    val remainingInstallments: Int?,
    val state: String?,
    val startDate: String?,
    val endDate: String?,
    val nextInstallmentDate: String?,
    val activatedAt: String?,
    val cancelledAt: String?,
    val completedAt: String?,
    val localCreatedAt: String?
)

@Serializable
data class PurchaseTransactionDto(
    val transactionId: String?,
    val planId: String?,
    val transactionType: String?,
    val amount: Double?,
    val scheduledOn: String?,
    val tradedOn: String?,
    val state: String?,
    val folioNumber: String?,
    val scheme: String?,
    val allottedUnits: Double?,
    val purchasedAmount: Double?,
    val purchasedPrice: Double?,
    val daysSincePurchase: Long?,
    val navAtPurchase: Double?,
    val units: Double?,
    val profitLoss: Double?
)
