package com.pyllar.consumer.data.remote.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoanApplicationRequestDto(
    @SerialName("clientRequestId") val clientRequestId: String,
    /** Ids of the consent texts the user ticked, as fetched from the server. */
    @SerialName("consentTextIds") val consentTextIds: List<String>,
    @SerialName("applicant") val applicant: LoanApplicantDto
)

/** One piece of consent wording from the server; [requiresAcceptance] false means a notice shown without a checkbox. */
@Serializable
data class ConsentTextDto(
    @SerialName("id") val id: String,
    @SerialName("consentType") val consentType: String,
    @SerialName("version") val version: Int = 1,
    @SerialName("body") val body: String,
    @SerialName("requiresAcceptance") val requiresAcceptance: Boolean = true
)

@Serializable
data class LoanApplicantDto(
    @SerialName("contactNumber") val contactNumber: String,
    @SerialName("pan") val pan: String,
    @SerialName("panName") val panName: String,
    @SerialName("dob") val dob: String,
    @SerialName("gender") val gender: String,
    @SerialName("email") val email: String,
    @SerialName("pincode") val pincode: String,
    @SerialName("city") val city: String,
    @SerialName("state") val state: String,
    @SerialName("addressL1") val addressL1: String,
    @SerialName("addressL2") val addressL2: String,
    @SerialName("employmentType") val employmentType: String,
    @SerialName("income") val income: Long,
    @SerialName("incomeType") val incomeType: String,
    @SerialName("companyName") val companyName: String? = null,
    @SerialName("officialEmail") val officialEmail: String? = null,
    @SerialName("endUse") val endUse: String,
    @SerialName("bureauConsent") val bureauConsent: Boolean
)

@Serializable
data class LoanApplicationResponseDto(
    @SerialName("applicationId") val applicationId: String,
    @SerialName("status") val status: String? = null
)

@Serializable
data class LoanOfferDto(
    @SerialName("offerId") val offerId: String,
    @SerialName("lenderCode") val lenderCode: String? = null,
    @SerialName("lenderName") val lenderName: String,
    @SerialName("logoUrl") val logoUrl: String? = null,
    @SerialName("kfsUrl") val kfsUrl: String? = null,
    @SerialName("digital") val digital: Boolean = true,
    @SerialName("principal") val principal: Double? = null,
    @SerialName("interestRate") val interestRate: Double? = null,
    @SerialName("apr") val apr: Double? = null,
    @SerialName("installment") val installment: Double? = null,
    @SerialName("term") val term: String? = null,
    @SerialName("processingFee") val processingFee: String? = null,
    @SerialName("lenderMinAmount") val lenderMinAmount: Double? = null,
    @SerialName("lenderMaxAmount") val lenderMaxAmount: Double? = null,
    @SerialName("totalPayable") val totalPayable: Double? = null,
    @SerialName("netDisbursed") val netDisbursed: Double? = null,
    @SerialName("totalInterest") val totalInterest: Double? = null,
    @SerialName("installments") val installments: Int? = null,
    @SerialName("tncUrl") val tncUrl: String? = null
)

@Serializable
data class LoanApplicationViewDto(
    @SerialName("applicationId") val applicationId: String,
    @SerialName("status") val status: String,
    @SerialName("offers") val offers: List<LoanOfferDto> = emptyList(),
    @SerialName("pendingLenders") val pendingLenders: Int = 0,
    @SerialName("accountAggregatorUrl") val accountAggregatorUrl: String? = null,
    @SerialName("pollAfterMillis") val pollAfterMillis: Long = 0
)

@Serializable
data class LoanDetailsDto(
    @SerialName("applicationId") val applicationId: String,
    @SerialName("status") val status: String? = null,
    /** yyyy-MM-dd. */
    @SerialName("appliedOn") val appliedOn: String? = null,
    @SerialName("offer") val offer: LoanOfferDto
)

@Serializable
data class LoanContinueDto(
    @SerialName("mode") val mode: String,
    @SerialName("url") val url: String? = null
)

@Serializable
data class LoanEntryDto(
    @SerialName("status") val status: String? = null,
    @SerialName("applicationId") val applicationId: String? = null,
    @SerialName("cooldownActive") val cooldownActive: Boolean = false,
    /** yyyy-MM-dd; set only while cooldownActive. */
    @SerialName("appliedOn") val appliedOn: String? = null,
    @SerialName("retryOn") val retryOn: String? = null,
    @SerialName("daysRemaining") val daysRemaining: Int = 0
)

