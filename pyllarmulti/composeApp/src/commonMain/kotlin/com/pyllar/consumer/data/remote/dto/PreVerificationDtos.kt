package com.pyllar.consumer.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PreVerificationRequestDto(
    val panNumber: String,
    val name: String,
    val bankAccounts: List<BankAccountRequestDto>
)

@Serializable
data class BankAccountRequestDto(
    val value: BankAccountValueDto,
    @SerialName("verify_manually_if_required")
    val verifyManuallyIfRequired: Boolean = false
)

@Serializable
data class BankAccountValueDto(
    @SerialName("account_number")
    val accountNumber: String,
    @SerialName("ifsc_code")
    val ifscCode: String,
    @SerialName("account_type")
    val accountType: String,
    @SerialName("bank_account_proof")
    val bankAccountProof: String? = null
)

object PreVerificationRequestHelper {
    
    fun createAutoVerificationRequest(
        panNumber: String,
        name: String,
        accountNumber: String,
        ifscCode: String,
        accountType: String = "savings"
    ): PreVerificationRequestDto {
        return PreVerificationRequestDto(
            panNumber = panNumber,
            name = name,
            bankAccounts = listOf(
                BankAccountRequestDto(
                    value = BankAccountValueDto(
                        accountNumber = accountNumber,
                        ifscCode = ifscCode,
                        accountType = accountType
                    ),
                    verifyManuallyIfRequired = false
                )
            )
        )
    }

    fun createManualVerificationRequest(
        panNumber: String,
        name: String,
        accountNumber: String,
        ifscCode: String,
        accountType: String = "savings",
        bankAccountProof: String
    ): PreVerificationRequestDto {
        return PreVerificationRequestDto(
            panNumber = panNumber,
            name = name,
            bankAccounts = listOf(
                BankAccountRequestDto(
                    value = BankAccountValueDto(
                        accountNumber = accountNumber,
                        ifscCode = ifscCode,
                        accountType = accountType,
                        bankAccountProof = bankAccountProof
                    ),
                    verifyManuallyIfRequired = true
                )
            )
        )
    }
}

@Serializable
data class PreVerificationResponseDto(
    val status: String,
    val message: String? = null,
    val data: PreVerificationDataDto? = null,
    val errors: List<String>? = null,
    val navigation: com.pyllar.consumer.data.remote.model.dto.NavigationInfo? = null,
    
    // Flattened fields for responses where 'data' is at the top level
    val id: String? = null,
    @SerialName("object")
    val objectType: String? = null,
    @SerialName("investor_identifier")
    val investorIdentifier: String? = null,
    val readiness: ReadinessDto? = null
)

@Serializable
data class PreVerificationDataDto(
    @SerialName("object")
    val objectType: String,
    val id: String,
    val status: String,
    @SerialName("investor_identifier")
    val investorIdentifier: String,
    val readiness: ReadinessDto? = null,
    val name: NameDto? = null,
    val pan: PanDto? = null,
    @SerialName("bank_accounts")
    val bankAccounts: List<BankAccountDto>? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("completed_at")
    val completedAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
) {
    fun isCompleted(): Boolean = status == "completed"
    fun isInProgress(): Boolean = status == "accepted"
    fun isInvestorReadyToInvest(): Boolean = readiness?.status == "verified"
    fun getVerifiedBankAccounts(): List<BankAccountDto> = bankAccounts?.filter { it.status == "verified" } ?: emptyList()
    fun getFailedBankAccounts(): List<BankAccountDto> = bankAccounts?.filter { it.status == "failed" } ?: emptyList()
    fun getBankAccountsRequiringManualVerification(): List<BankAccountDto> = bankAccounts?.filter { 
        it.status == "failed" && it.code == "awaiting_approval_for_manual_verification_with_proof" 
    } ?: emptyList()
}

@Serializable
data class ReadinessDto(
    val status: String? = null,
    val code: String? = null,
    val reason: String? = null
)

@Serializable
data class NameDto(
    val status: String? = null,
    val code: String? = null,
    val reason: String? = null,
    val value: String? = null
)

@Serializable
data class PanDto(
    val status: String? = null,
    val code: String? = null,
    val reason: String? = null,
    val value: String? = null
)

@Serializable
data class BankAccountDto(
    val status: String? = null,
    val code: String? = null,
    val reason: String? = null,
    val value: BankAccountValueResponseDto? = null
)

@Serializable
data class BankAccountValueResponseDto(
    @SerialName("account_number")
    val accountNumber: String,
    @SerialName("ifsc_code")
    val ifscCode: String,
    @SerialName("account_type")
    val accountType: String,
    @SerialName("bank_account_proof")
    val bankAccountProof: String? = null
)

