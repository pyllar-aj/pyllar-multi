package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class InvestorDashboardResponseV2Dto(
    @SerialName("userName")
    val userName: String?,
    @SerialName("portfolioSummary")
    val portfolioSummary: UserPortfolioSummaryDto?,
    @SerialName("kycDetails")
    val kycDetails: UserKycDetailsDto?,
    @SerialName("currentInvestments")
    val currentInvestments: List<CurrentInvestmentDto>?,
    @SerialName("recommendations")
    val recommendations: List<RecommendationDto>?,
    @SerialName("showAll")
    val showAll: Boolean? = false,
    @SerialName("referralEnabled")
    val referralEnabled: Boolean? = false,
    @SerialName("show_survey")
    val showSurvey: Boolean? = false
)

@Serializable
data class UserPortfolioSummaryDto(
    @SerialName("totalValue")
    val totalValue: Double?,
    @SerialName("profitAmount")
    val profitAmount: Double?,
    @SerialName("profitPercentage")
    val profitPercentage: Double?,
    @SerialName("currency")
    val currency: String?,
    @SerialName("totalInvestedAmount")
    val totalInvestedAmount: Double?
)

@Serializable
data class UserKycDetailsDto(
    @SerialName("kycStatus")
    val kycStatus: String?,
    @SerialName("kycMessage")
    val kycMessage: String?,
    @SerialName("kycUpdateRequired")
    val kycUpdateRequired: Boolean?,
    @SerialName("pendingDocuments")
    val pendingDocuments: List<String>?,
    @SerialName("kycProvider")
    val kycProvider: String?,
    @SerialName("bankAccountVerified")
    val bankAccountVerified: Boolean?,
    @SerialName("panVerified")
    val panVerified: Boolean?,
    @SerialName("addressVerified")
    val addressVerified: Boolean?
)

@Serializable
data class CurrentInvestmentDto(
    @SerialName("purpose")
    val purpose: String?,
    @SerialName("icon")
    val icon: String?,
    @SerialName("progressPercentage")
    val progressPercentage: Int?,
    @SerialName("investedAmount")
    val investedAmount: Double?,
    @SerialName("targetAmount")
    val targetAmount: Double?,
    @SerialName("currentValue")
    val currentValue: Double?,
    @SerialName("investmentInProgressValue")
    val investmentInProgressValue: Double?,
    @SerialName("currentValuePercentage")
    val currentValuePercentage: Double?,
    @SerialName("timeRemainingMonths")
    val timeRemainingMonths: Int?,
    @SerialName("folioDetails")
    val folioDetails: List<FolioDetailDto>?,
    @SerialName("createdAt")
    val createdAt: String?,
    @SerialName("canWithdraw")
    val canWithdraw: Boolean?,
    @SerialName("nextSipDate")
    val nextSipDate: String?,
    @SerialName("mandatePending")
    val mandatePending: Boolean?,
    @SerialName("amountUnderProcessing")
    val amountUnderProcessing: Double?,
    @SerialName("mandateStatus")
    val mandateStatus: String?,
    @SerialName("planSummaryDtos")
    val planSummaryDtos: List<PlanSummaryDto>?,
    @SerialName("totalSipSummary")
    val totalSipSummary: PlanSummaryDto?,
    @SerialName("unitsInGm")
    val unitsInGm: Double?,
    @SerialName("cummulativeValue")
    val cummulativeValue: Double?,
    @SerialName("profit")
    val profit: Double?,
    @SerialName("realizedProfit")
    val realizedProfit: Double?,
    @SerialName("unrealizedProfit")
    val unrealizedProfit: Double?,
    @SerialName("instantRedemptionValue")
    val instantRedemptionValue: Double?,
    @SerialName("redemptionInProgress")
    val redemptionInProgress: Double?,
    @SerialName("redeemableAmount")
    val redeemableAmount: Double?
)

@Serializable
data class FolioDetailDto(
    @SerialName("fundName")
    val fundName: String?,
    @SerialName("folioNumber")
    val folioNumber: String?,
    @SerialName("isin")
    val isin: String?,
    @SerialName("status")
    val status: String?,
    @SerialName("planDetails")
    val planDetails: String?,
    @SerialName("sipAmount")
    val sipAmount: Double?,
    @SerialName("investmentAmount")
    val investmentAmount: Double?,
    @SerialName("currentValue")
    val currentValue: Double?,
    @SerialName("profitAmount")
    val profitAmount: Double?
)

@Serializable
data class RecommendationDto(
    @SerialName("purpose")
    val purpose: String?,
    @SerialName("message")
    val message: String?,
    @SerialName("sipAmount")
    val sipAmount: Double?,
    @SerialName("totalInvestmentAmount")
    val totalInvestmentAmount: Double?
)

@Serializable
data class PlanSummaryDto(
    @SerialName("amount")
    val amount: Double?,
    @SerialName("nextSipDate")
    val nextSipDate: String?,
    @SerialName("status")
    val status: String?,
    @SerialName("frequency")
    val frequency: String?,
    @SerialName("planId")
    val planId: String?,
    @SerialName("mandateId")
    val mandateId: Long?,
    @SerialName("mandateApprovedDate")
    val mandateApprovedDate: String?,
    @SerialName("mandateCancelledDate")
    val mandateCancelledDate: String?,
    @SerialName("mandateCreatedDate")
    val mandateCreatedDate: String?,
    @SerialName("firstUnitAllocationDate")
    val firstUnitAllocationDate: String?
)
