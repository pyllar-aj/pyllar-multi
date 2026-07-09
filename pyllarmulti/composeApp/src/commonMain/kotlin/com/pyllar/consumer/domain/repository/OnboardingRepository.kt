package com.pyllar.consumer.domain.repository

import com.pyllar.consumer.data.remote.model.DigiLinkRequest
import com.pyllar.consumer.data.remote.model.MinimalKycRequest
import com.pyllar.consumer.data.remote.model.MinimalKycResponse
import com.pyllar.consumer.data.remote.model.dto.AccountDeletionResponseDto
import com.pyllar.consumer.data.remote.model.dto.HelperCodeResponseDto
import com.pyllar.consumer.data.remote.requests.CreateNomineeRequest
import com.pyllar.consumer.data.remote.requests.CreateNomineeRequestV2
import com.pyllar.consumer.data.remote.requests.HelperCodeRequest
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonObject

/**
 * Server-facing onboarding/KYC operations.
 * This excludes any local Room persistence concerns.
 */
interface OnboardingRepository {
    fun submitNomineeDetails(
        request: CreateNomineeRequest
    ): Flow<Resource<JsonObject>>

    fun submitNomineeDetailsV2(
        request: CreateNomineeRequestV2
    ): Flow<Resource<JsonObject>>

    fun updateAdditionalKyc(
        kycAttemptId: String,
        request: com.pyllar.consumer.data.remote.model.AdditionalKycRequest
    ): Flow<Resource<com.pyllar.consumer.data.remote.model.dto.AdditionalKycResponseDto>>

    fun checkPan(
        pan: String,
        userId: String
    ): Flow<Resource<com.pyllar.consumer.data.remote.model.dto.CheckPanResponseDto>>

    fun createMinimalKyc(
        request: MinimalKycRequest
    ): Flow<Resource<MinimalKycResponse>>

    fun createMinimalDetails(
        request: MinimalKycRequest
    ): Flow<Resource<MinimalKycResponse>>

    fun getDigiLink(
        request: DigiLinkRequest
    ): Flow<Resource<MinimalKycResponse>>

    fun requestAccountDeletion(
        userId: String,
        notes: String? = null
    ): Flow<Resource<AccountDeletionResponseDto>>

    fun getHelperCode(
        userId: String
    ): Flow<Resource<HelperCodeResponseDto>>

    fun submitHelperCode(
        request: HelperCodeRequest
    ): Flow<Resource<Unit>>

    fun selectGoal(
        request: com.pyllar.consumer.data.remote.requests.GoalSelectionRequest,
        currentScreen: String = com.pyllar.consumer.navigation.ScreenNames.ONBOARDING_GOALS_V3
    ): Flow<Resource<com.pyllar.consumer.data.remote.model.dto.GoalSelectionResponseDto>>

    fun submitBankDetails(
        userId: String,
        accountNumber: String,
        ifscCode: String,
        accountType: String
    ): Flow<Resource<com.pyllar.consumer.data.remote.model.dto.BankDetailsResponseDto>>

    fun initiateBankVerification(
        userId: String,
        name: String
    ): Flow<Resource<com.pyllar.consumer.data.remote.model.dto.VerificationInitiateResponseDto>>

    fun getVerificationStatus(
        verificationId: String,
        userId: String
    ): Flow<Resource<com.pyllar.consumer.data.remote.model.dto.VerificationStatusResponseDto>>

    fun getProfileDetails(
        userId: String
    ): Flow<Resource<com.pyllar.consumer.data.remote.model.dto.ProfileResponseDto>>

    fun fetchUserDetails(
        userId: String,
        request: com.pyllar.consumer.data.remote.requests.UserDetailsFetchRequest
    ): Flow<Resource<com.pyllar.consumer.data.remote.model.dto.UserDetailsFetchResponseDto>>

    fun lookupUpiVpaBankDetails(
        userId: String,
        upiVpa: String
    ): Flow<Resource<com.pyllar.consumer.data.remote.model.dto.UpiVpaBankDetailsResponseDto>>

    fun lookupUpiVpa(
        userId: String,
        upiVpa: String
    ): Flow<Resource<com.pyllar.consumer.data.remote.model.dto.UpiVpaLookupResponseDto>>

    fun pollUpiVpaLookupStatus(
        userId: String,
        upiVpa: String
    ): Flow<Resource<com.pyllar.consumer.data.remote.model.dto.UpiVpaLookupResponseDto>>
}

