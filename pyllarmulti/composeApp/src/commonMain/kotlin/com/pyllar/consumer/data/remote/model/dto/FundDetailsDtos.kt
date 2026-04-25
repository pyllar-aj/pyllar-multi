package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FundDetailsResponseDto(
    @SerialName("isin") val isin: String,
    @SerialName("fundName") val fundName: String,
    @SerialName("fundType") val fundType: String?,
    @SerialName("category") val category: String?,
    @SerialName("currentNav") val currentNav: Double,
    @SerialName("navDate") val navDate: String, // String for date to handle parsing later
    @SerialName("dayChange") val dayChange: Double?,
    @SerialName("dayChangePercent") val dayChangePercent: Double?,
    @SerialName("expenseRatio") val expenseRatio: Double?,
    @SerialName("aum") val aum: Double?,
    @SerialName("minInvestment") val minInvestment: Double?,
    @SerialName("exitLoad") val exitLoad: Double?,
    @SerialName("exitLoadPeriodDays") val exitLoadPeriodDays: Int?,
    @SerialName("returns") val returns: FundReturnsDto?,
    @SerialName("chartData") val chartData: Map<String, List<NavChartDataDto>>?,
    @SerialName("bankDetails") val bankDetails: BankDetailsDto?,
    @SerialName("schemeDocumentUrl") val schemeDocumentUrl: String?,
    @SerialName("riskLevel") val riskLevel: String?,
    @SerialName("companyAllocation") val companyAllocation: List<CompanyAllocationDto>?
)

@Serializable
data class FundReturnsDto(
    @SerialName("oneYear") val oneYear: Double?,
    @SerialName("threeYear") val threeYear: Double?,
    @SerialName("fiveYear") val fiveYear: Double?
)

@Serializable
data class NavChartDataDto(
    @SerialName("date") val date: String,
    @SerialName("nav") val nav: Double
)

@Serializable
data class BankDetailsDto(
    @SerialName("accountNumber") val accountNumber: String?,
    @SerialName("ifscCode") val ifscCode: String?,
    @SerialName("bankName") val bankName: String?,
    @SerialName("accountHolderName") val accountHolderName: String?
)

/**
 * Company allocation entry for a fund (company name -> allocation %).
 * Backend may send companyAllocation as a list of these in fund details JSON.
 */
@Serializable
data class CompanyAllocationDto(
    @SerialName("company") val company: String?,
    @SerialName("allocation") val allocation: Double?
)
