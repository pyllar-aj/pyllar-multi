package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ========== AUTH RESPONSE DTOS ==========

@Serializable
data class OtpSendResponseDto(
    @SerialName("newUser")
    val isNewUser: Boolean,
    @SerialName("actualUserId")
    val actualUserId: String?,
    @SerialName("phoneNumber")
    val phoneNumber: String,
    @SerialName("ref")
    val ref: String
)

@Serializable
data class PhoneVerificationResponseDto(
    @SerialName("phoneNumber")
    val phoneNumber: String,
    @SerialName("registered")
    val registered: Boolean,
    @SerialName("message")
    val message: String
)

@Serializable
data class AuthUserResponseDto(
    @SerialName("userId")
    val userId: String,
    @SerialName("phoneNumber")
    val phoneNumber: String,
    @SerialName("role")
    val role: String,
    @SerialName("authToken")
    val authToken: String
)

// ========== KYC RESPONSE DTOS ==========

@Serializable
data class CheckPanResponseDto(
    @SerialName("panNumber")
    val panNumber: String,
    @SerialName("valid")
    val valid: Boolean,
    @SerialName("message")
    val message: String,
    @SerialName("panHolderName")
    val panHolderName: String?
)

@Serializable
data class UpdateEmailResponseDto(
    @SerialName("email")
    val email: String,
    @SerialName("message")
    val message: String? = null,
    @SerialName("updated")
    val updated: Boolean,
    @SerialName("newUser")
    val newUser: Boolean? = null,
    @SerialName("mismatch")
    val mismatch: Boolean? = null,
    @SerialName("error")
    val error: Boolean? = null
)

@Serializable
data class AdditionalKycResponseDto(
    @SerialName("kycAttemptId")
    val kycAttemptId: String,
    @SerialName("status")
    val status: String,
    @SerialName("message")
    val message: String
)

@Serializable
data class MinimalKycResponseDto(
    @SerialName("reUrl")
    val reUrl: String? = null,
    @SerialName("kycAttemptId")
    val kycAttemptId: String? = null
)

@Serializable
data class DigiLinkResponseDto(
    @SerialName("reUrl")
    val reUrl: String? = null,
    @SerialName("kycAttemptId")
    val kycAttemptId: String? = null
)

@Serializable
data class BankDetailsResponseDto(
    @SerialName("investorId")
    val investorId: String? = null
)

@Serializable
data class GoalSelectionResponseDto(
    @SerialName("userPurposeId")
    val userPurposeId: String,
    @SerialName("investmentPurpose")
    val investmentPurpose: String,
    @SerialName("userId")
    val userId: String,
    @SerialName("message")
    val message: String? = null,
    @SerialName("success")
    val success: Boolean
)

@Serializable
data class InvestmentLimitsResponseDto(
    @SerialName("userInvestmentPurposeId")
    val userInvestmentPurposeId: String,
    @SerialName("min")
    val min: Long? = null,
    @SerialName("max")
    val max: Long? = null,
    @SerialName("defaultAmount")
    val defaultAmount: Long? = null
)

// ========== MUTUAL FUND RESPONSE DTOS ==========

@Serializable
data class OnboardingResponseDto(
    @SerialName("investorId")
    val investorId: String,
    @SerialName("userId")
    val userId: String,
    @SerialName("status")
    val status: String,
    @SerialName("message")
    val message: String
)

@Serializable
data class SipResponseDto(
    @SerialName("sipId")
    val sipId: String,
    @SerialName("userId")
    val userId: String,
    @SerialName("fundId")
    val fundId: String,
    @SerialName("amount")
    val amount: Double,
    @SerialName("status")
    val status: String,
    @SerialName("message")
    val message: String
)

@Serializable
data class PortfolioResponseDto(
    @SerialName("userId")
    val userId: String,
    @SerialName("totalValue")
    val totalValue: Double,
    @SerialName("totalInvestment")
    val totalInvestment: Double,
    @SerialName("gainLoss")
    val gainLoss: Double,
    @SerialName("holdings")
    val holdings: List<HoldingInfoDto>,
    @SerialName("message")
    val message: String
)

@Serializable
data class HoldingInfoDto(
    @SerialName("fundId")
    val fundId: String,
    @SerialName("fundName")
    val fundName: String,
    @SerialName("currentValue")
    val currentValue: Double,
    @SerialName("investedAmount")
    val investedAmount: Double
)

@Serializable
data class DailySipResponseDto(
    @SerialName("dailySipId")
    val dailySipId: String,
    @SerialName("userId")
    val userId: String,
    @SerialName("kycAttemptId")
    val kycAttemptId: String,
    @SerialName("investorId")
    val investorId: String,
    @SerialName("amount")
    val amount: Double,
    @SerialName("status")
    val status: String,
    @SerialName("message")
    val message: String
)


@Serializable
data class ConsentOtpVerificationResponseDto(
    @SerialName("message")
    val message: String? = null,
    @SerialName("success")
    val success: Boolean = false
)
