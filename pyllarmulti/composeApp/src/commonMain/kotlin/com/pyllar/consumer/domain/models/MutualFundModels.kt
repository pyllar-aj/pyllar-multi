package com.pyllar.consumer.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Request models
@Serializable
data class InvestorOnboardingRequest(
    @SerialName("firstName") val firstName: String,
    @SerialName("lastName") val lastName: String,
    @SerialName("middleName") val middleName: String? = null,
    @SerialName("gender") val gender: String,
    @SerialName("panNumber") val panNumber: String,
    @SerialName("dateOfBirth") val dateOfBirth: String,
    @SerialName("addressLine1") val addressLine1: String,
    @SerialName("city") val city: String,
    @SerialName("state") val state: String,
    @SerialName("pincode") val pincode: String,
    @SerialName("occupation") val occupation: String,
    @SerialName("incomeRange") val incomeRange: String,
    @SerialName("bankAccount") val bankAccount: BankAccountRequest
)

@Serializable
data class BankAccountRequest(
    @SerialName("accountNumber") val accountNumber: String,
    @SerialName("ifscCode") val ifscCode: String,
    @SerialName("accountHolderName") val accountHolderName: String,
    @SerialName("accountType") val accountType: String,
    @SerialName("bankName") val bankName: String
)

@Serializable
data class SipCreationRequest(
    @SerialName("investmentAccountId") val investmentAccountId: Int,
    @SerialName("isin") val isin: String,
    @SerialName("amount") val amount: Double,
    @SerialName("frequency") val frequency: String,
    @SerialName("startDate") val startDate: String,
    @SerialName("installments") val installments: Int? = null,
    @SerialName("fundSchemeName") val fundSchemeName: String? = null
)

@Serializable
data class LumpsumPurchaseRequest(
    @SerialName("investmentAccountId") val investmentAccountId: Int,
    @SerialName("isin") val isin: String,
    @SerialName("amount") val amount: Double,
    @SerialName("investmentDate") val investmentDate: String,
    @SerialName("fundSchemeName") val fundSchemeName: String
)

// Response models
@Serializable
data class MutualFundResponse<T>(
    @SerialName("status") val status: String,
    @SerialName("message") val message: String,
    @SerialName("data") val data: T? = null
)

@Serializable
data class OnboardingResponse(
    @SerialName("investorProfileId") val investorProfileId: Int,
    @SerialName("investmentAccountId") val investmentAccountId: Int,
    @SerialName("message") val message: String
)

@Serializable
data class SipResponse(
    @SerialName("sipId") val sipId: String,
    @SerialName("amount") val amount: Double,
    @SerialName("frequency") val frequency: String,
    @SerialName("startDate") val startDate: String,
    @SerialName("status") val status: String,
    @SerialName("sourceRefId") val sourceRefId: String
)

@Serializable
data class PortfolioResponse(
    @SerialName("totalSips") val totalSips: Int,
    @SerialName("activeSips") val activeSips: Int,
    @SerialName("totalInvestmentAccounts") val totalInvestmentAccounts: Int,
    @SerialName("totalBankAccounts") val totalBankAccounts: Int,
    @SerialName("totalPurchases") val totalPurchases: Int,
    @SerialName("sipOrders") val sipOrders: List<SipOrder>? = null,
    @SerialName("purchaseOrders") val purchaseOrders: List<PurchaseOrder>? = null
)

@Serializable
data class SipOrder(
    @SerialName("id") val id: String,
    @SerialName("amount") val amount: Double,
    @SerialName("frequency") val frequency: String,
    @SerialName("startDate") val startDate: String,
    @SerialName("active") val active: Boolean,
    @SerialName("fundSchemeName") val fundSchemeName: String? = null,
    @SerialName("sourceRefId") val sourceRefId: String? = null
)

@Serializable
data class PurchaseOrder(
    @SerialName("id") val id: String,
    @SerialName("amount") val amount: Double,
    @SerialName("investmentDate") val investmentDate: String,
    @SerialName("status") val status: String,
    @SerialName("fundSchemeName") val fundSchemeName: String? = null
)

// UI Models
data class InvestorFormData(
    val firstName: String = "",
    val lastName: String = "",
    val middleName: String = "",
    val gender: String = "",
    val panNumber: String = "",
    val dateOfBirth: String = "",
    val addressLine1: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    val occupation: String = "",
    val incomeRange: String = "",
    val bankAccountNumber: String = "",
    val ifscCode: String = "",
    val accountHolderName: String = "",
    val accountType: String = "",
    val bankName: String = ""
)

data class SipFormData(
    val investmentAccountId: Int = 0,
    val isin: String = "INF846K018E9",
    val amount: String = "",
    val frequency: String = "MONTHLY",
    val startDate: String = "",
    val installments: String = "",
    val fundSchemeName: String = "HDFC Equity Fund"
)

object MutualFundConstants {
    val GENDER_OPTIONS = listOf("MALE", "FEMALE", "OTHER")
    val INCOME_RANGES = listOf(
        "BELOW_1L", "1L_TO_5L", "5L_TO_10L", "10L_TO_25L", "25L_TO_50L", "ABOVE_50L"
    )
    val ACCOUNT_TYPES = listOf("SAVINGS", "CURRENT")
    val SIP_FREQUENCIES = listOf("DAILY", "WEEKLY", "MONTHLY", "QUARTERLY")
    val OCCUPATIONS = listOf(
        "Software Engineer", "Business", "Service", "Professional", "Self Employed", "Student", "Other"
    )
    val STATES = listOf(
        "Maharashtra", "Delhi", "Karnataka", "Tamil Nadu", "West Bengal", "Gujarat", "Rajasthan", "Uttar Pradesh", "Other"
    )
}

