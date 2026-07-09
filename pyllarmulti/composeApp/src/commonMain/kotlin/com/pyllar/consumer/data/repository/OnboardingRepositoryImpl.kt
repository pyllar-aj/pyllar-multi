package com.pyllar.consumer.data.repository

import com.pyllar.consumer.data.remote.model.DigiLinkRequest
import com.pyllar.consumer.data.remote.model.MinimalKycRequest
import com.pyllar.consumer.data.remote.model.MinimalKycResponse
import com.pyllar.consumer.data.remote.requests.AccountDeletionRequestDto
import com.pyllar.consumer.data.remote.model.dto.AccountDeletionResponseDto
import com.pyllar.consumer.data.remote.model.dto.DigiLinkResponseDto
import com.pyllar.consumer.data.remote.model.dto.HelperCodeResponseDto
import com.pyllar.consumer.data.remote.model.dto.MinimalKycResponseDto
import com.pyllar.consumer.data.remote.network.PyllarApiClient
import com.pyllar.consumer.data.remote.requests.CreateNomineeRequest
import com.pyllar.consumer.data.remote.requests.CreateNomineeRequestV2
import com.pyllar.consumer.data.remote.requests.HelperCodeRequest
import com.pyllar.consumer.domain.repository.OnboardingRepository
import com.pyllar.consumer.util.Resource
import io.ktor.http.HttpMethod
import com.pyllar.consumer.domain.storage.SessionStore
import com.pyllar.consumer.util.platformLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.JsonObject
import com.pyllar.consumer.data.remote.model.dto.UpiVpaLookupRequest
import com.pyllar.consumer.data.remote.model.dto.UpiVpaLookupResponseDto
import com.pyllar.consumer.data.remote.model.dto.UpiVpaBankDetailsResponseDto

import io.ktor.client.request.header

class OnboardingRepositoryImpl(
    private val apiClient: PyllarApiClient,
    private val sessionStore: SessionStore
) : OnboardingRepository {

    override fun selectGoal(
        request: com.pyllar.consumer.data.remote.requests.GoalSelectionRequest,
        currentScreen: String
    ): Flow<Resource<com.pyllar.consumer.data.remote.model.dto.GoalSelectionResponseDto>> = flow {
        emit(Resource.Loading())
        val result = apiClient.post<com.pyllar.consumer.data.remote.model.dto.GoalSelectionResponseDto, com.pyllar.consumer.data.remote.requests.GoalSelectionRequest>(
            path = "api/kyc/onboarding/select-goal",
            body = request
        ) {
            header("X-Current-Screen", currentScreen)
        }
        emit(result)
    }


    override fun submitNomineeDetails(
        request: CreateNomineeRequest
    ): Flow<Resource<JsonObject>> = flow {
        emit(Resource.Loading())
        val result = apiClient.post<JsonObject, CreateNomineeRequest>(
            path = "api/kyc/onboarding/nominee-details",
            body = request
        )
        emit(result)
    }

    override fun submitNomineeDetailsV2(
        request: CreateNomineeRequestV2
    ): Flow<Resource<JsonObject>> = flow {
        emit(Resource.Loading())
        val result = apiClient.post<JsonObject, CreateNomineeRequestV2>(
            path = "api/kyc/onboarding/nominee-details-v2",
            body = request
        )
        emit(result)
    }

    override fun updateAdditionalKyc(
        kycAttemptId: String,
        request: com.pyllar.consumer.data.remote.model.AdditionalKycRequest
    ): Flow<Resource<com.pyllar.consumer.data.remote.model.dto.AdditionalKycResponseDto>> = flow {
        emit(Resource.Loading())
        val result = apiClient.put<com.pyllar.consumer.data.remote.model.dto.AdditionalKycResponseDto, com.pyllar.consumer.data.remote.model.AdditionalKycRequest>(
            path = "api/kyc/onboarding/request/$kycAttemptId",
            body = request
        )
        emit(result)
    }

    override fun checkPan(
        pan: String,
        userId: String
    ): Flow<Resource<com.pyllar.consumer.data.remote.model.dto.CheckPanResponseDto>> = flow {
        emit(Resource.Loading())
        
        @kotlinx.serialization.Serializable
        data class CheckPanReq(val pan: String, val userId: String)
        
        val result = apiClient.post<com.pyllar.consumer.data.remote.model.dto.CheckPanResponseDto, CheckPanReq>(
            path = "api/kyc/onboarding/checkPan",
            body = CheckPanReq(pan, userId)
        )
        emit(result)
    }

    override fun createMinimalKyc(
        request: MinimalKycRequest
    ): Flow<Resource<MinimalKycResponse>> = flow {
        emit(Resource.Loading())
        
        // Ensure userId is saved to sessionStore so PyllarApiClient picks it up in headers
        if (request.userId.isNotBlank() && request.userId != "anonymous") {
            platformLog("OnboardingRepository: Saving userId to sessionStore: ${request.userId}")
            sessionStore.saveUserId(request.userId)
        }
        when (val result =
            apiClient.post<MinimalKycResponseDto, MinimalKycRequest>(
                path = "api/kyc/onboarding/requestV2",
                body = request
            )
        ) {
            is Resource.Success -> {
                val dto = result.data
                if (dto != null) {
                    val mapped = MinimalKycResponse(
                        reUrl = dto.reUrl,
                        kycAttemptId = dto.kycAttemptId
                    )
                    emit(
                        Resource.Success(
                            data = mapped,
                            navigation = result.navigation,
                            fieldErrors = result.fieldErrors
                        )
                    )
                } else {
                    emit(
                        Resource.Error(
                            message = "Empty response data",
                            navigation = result.navigation,
                            fieldErrors = result.fieldErrors,
                            errorType = result.errorType
                        )
                    )
                }
            }

            is Resource.Error -> emit(
                Resource.Error(
                    message = result.message ?: "",
                    navigation = result.navigation,
                    fieldErrors = result.fieldErrors,
                    errorType = result.errorType
                )
            )

            is Resource.Loading -> emit(Resource.Loading())
        }
    }

    override fun createMinimalDetails(
        request: MinimalKycRequest
    ): Flow<Resource<MinimalKycResponse>> = flow {
        emit(Resource.Loading())

        // Ensure userId is saved to sessionStore so PyllarApiClient picks it up in headers
        if (request.userId.isNotBlank() && request.userId != "anonymous") {
            platformLog("OnboardingRepository: Saving userId to sessionStore: ${request.userId}")
            sessionStore.saveUserId(request.userId)
        }
        platformLog(
            "[API-REQ] createMinimalDetails: userId=${request.userId} name=${request.name} panNumber=${
                request.panNumber.takeLast(
                    4
                )
            } dob=${request.dateOfBirth} email=${request.emailAddress} mobile=${request.mobile.countryCode}-${request.mobile.number} preVerificationId=${request.preVerificationId}"
        )
        when (val result =
            apiClient.post<MinimalKycResponseDto, MinimalKycRequest>(
                path = "api/kyc/onboarding/minDetails",
                body = request
            )
        ) {
            is Resource.Success -> {
                val dto = result.data
                if (dto != null) {
                    val mapped = MinimalKycResponse(
                        reUrl = dto.reUrl,
                        kycAttemptId = dto.kycAttemptId
                    )
                    emit(
                        Resource.Success(
                            data = mapped,
                            navigation = result.navigation,
                            fieldErrors = result.fieldErrors
                        )
                    )
                } else {
                    emit(
                        Resource.Error(
                            message = "Empty response data",
                            navigation = result.navigation,
                            fieldErrors = result.fieldErrors,
                            errorType = result.errorType
                        )
                    )
                }
            }

            is Resource.Error -> emit(
                Resource.Error(
                    message = result.message ?: "",
                    navigation = result.navigation,
                    fieldErrors = result.fieldErrors,
                    errorType = result.errorType
                )
            )

            is Resource.Loading -> emit(Resource.Loading())
        }
    }

    override fun getDigiLink(
        request: DigiLinkRequest
    ): Flow<Resource<MinimalKycResponse>> = flow {
        emit(Resource.Loading())
        when (val result =
            apiClient.post<DigiLinkResponseDto, DigiLinkRequest>(
                path = "api/kyc/onboarding/digi-link",
                body = request
            )
        ) {
            is Resource.Success -> {
                val dto = result.data
                if (dto != null) {
                    val mapped = MinimalKycResponse(
                        reUrl = dto.reUrl,
                        kycAttemptId = dto.kycAttemptId
                    )
                    emit(
                        Resource.Success(
                            data = mapped,
                            navigation = result.navigation,
                            fieldErrors = result.fieldErrors
                        )
                    )
                } else {
                    emit(
                        Resource.Error(
                            message = "Empty response data",
                            navigation = result.navigation,
                            fieldErrors = result.fieldErrors,
                            errorType = result.errorType
                        )
                    )
                }
            }

            is Resource.Error -> emit(
                Resource.Error(
                    message = result.message ?: "",
                    navigation = result.navigation,
                    fieldErrors = result.fieldErrors,
                    errorType = result.errorType
                )
            )

            is Resource.Loading -> emit(Resource.Loading())
        }
    }

    override fun requestAccountDeletion(
        userId: String,
        notes: String?
    ): Flow<Resource<AccountDeletionResponseDto>> = flow {
        emit(Resource.Loading())
        when (val result = apiClient.post<AccountDeletionResponseDto, AccountDeletionRequestDto>(
            path = "api/profile/request-deletion",
            body = AccountDeletionRequestDto(userId = userId, notes = notes)
        )) {
            is Resource.Success -> emit(Resource.Success(data = result.data, navigation = result.navigation, fieldErrors = result.fieldErrors))
            is Resource.Error -> emit(Resource.Error(message = result.message ?: "", navigation = result.navigation, fieldErrors = result.fieldErrors, errorType = result.errorType))
            is Resource.Loading -> emit(Resource.Loading())
        }
    }

    override fun getHelperCode(
        userId: String
    ): Flow<Resource<HelperCodeResponseDto>> = flow {
        emit(Resource.Loading())
        when (val result = apiClient.get<HelperCodeResponseDto>("api/kyc/onboarding/helper-code?userId=$userId")) {
            is Resource.Success -> emit(Resource.Success(data = result.data, navigation = result.navigation, fieldErrors = result.fieldErrors))
            is Resource.Error -> emit(Resource.Error(message = result.message ?: "", navigation = result.navigation, fieldErrors = result.fieldErrors, errorType = result.errorType))
            is Resource.Loading -> emit(Resource.Loading())
        }
    }

    override fun submitHelperCode(
        request: HelperCodeRequest
    ): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        when (val result = apiClient.post<Map<String, String>, HelperCodeRequest>("api/kyc/onboarding/helper-code", request)) {
            is Resource.Success -> emit(Resource.Success(data = Unit, navigation = result.navigation))
            is Resource.Error -> emit(Resource.Error(message = result.message ?: "", errorType = result.errorType))
            is Resource.Loading -> emit(Resource.Loading())
        }
    }

    override fun submitBankDetails(
        userId: String,
        accountNumber: String,
        ifscCode: String,
        accountType: String
    ): Flow<Resource<com.pyllar.consumer.data.remote.model.dto.BankDetailsResponseDto>> = flow {
        emit(Resource.Loading())
        if (userId.isNotBlank() && userId != "anonymous") {
            sessionStore.saveUserId(userId)
        }
        val result = apiClient.post<com.pyllar.consumer.data.remote.model.dto.BankDetailsResponseDto, Unit?>(
            path = "api/kyc/onboarding/bank-account",
            body = null
        ) {
            url.parameters.append("userId", userId)
            url.parameters.append("accountNumber", accountNumber)
            url.parameters.append("ifscCode", ifscCode)
            url.parameters.append("accountType", accountType)
        }
        emit(result)
    }

    override fun initiateBankVerification(
        userId: String,
        name: String
    ): Flow<Resource<com.pyllar.consumer.data.remote.model.dto.VerificationInitiateResponseDto>> = flow {
        emit(Resource.Loading())
        if (userId.isNotBlank() && userId != "anonymous") {
            sessionStore.saveUserId(userId)
        }
        @kotlinx.serialization.Serializable
        data class InitReq(val userId: String, val name: String)
        val result = apiClient.post<com.pyllar.consumer.data.remote.model.dto.VerificationInitiateResponseDto, InitReq>(
            path = "api/bank-verification/initiate",
            body = InitReq(userId, name)
        )
        emit(result)
    }

    override fun getVerificationStatus(
        verificationId: String,
        userId: String
    ): Flow<Resource<com.pyllar.consumer.data.remote.model.dto.VerificationStatusResponseDto>> = flow {
        emit(Resource.Loading())
        if (userId.isNotBlank() && userId != "anonymous") {
            sessionStore.saveUserId(userId)
        }
        val result = apiClient.get<com.pyllar.consumer.data.remote.model.dto.VerificationStatusResponseDto>(
            path = "api/bank-verification/status/$verificationId"
        )
        emit(result)
    }

    override fun getProfileDetails(
        userId: String
    ): Flow<Resource<com.pyllar.consumer.data.remote.model.dto.ProfileResponseDto>> = flow {
        emit(Resource.Loading())
        val result = apiClient.post<com.pyllar.consumer.data.remote.model.dto.ProfileResponseDto, com.pyllar.consumer.data.remote.model.dto.ProfileRequestDto>(
            path = "api/profile/details",
            body = com.pyllar.consumer.data.remote.model.dto.ProfileRequestDto(userId = userId)
        )
        emit(result)
    }

    override fun fetchUserDetails(
        userId: String,
        request: com.pyllar.consumer.data.remote.requests.UserDetailsFetchRequest
    ): Flow<Resource<com.pyllar.consumer.data.remote.model.dto.UserDetailsFetchResponseDto>> = flow {
        emit(Resource.Loading())
        if (userId.isNotBlank() && userId != "anonymous") {
            sessionStore.saveUserId(userId)
        }
        val result = apiClient.post<com.pyllar.consumer.data.remote.model.dto.UserDetailsFetchResponseDto, com.pyllar.consumer.data.remote.requests.UserDetailsFetchRequest>(
            path = "api/device/user-details-fetch",
            body = request
        )
        emit(result)
    }

    override fun lookupUpiVpaBankDetails(
        userId: String,
        upiVpa: String
    ): Flow<Resource<UpiVpaBankDetailsResponseDto>> = flow {
        emit(Resource.Loading())
        val result = apiClient.post<UpiVpaBankDetailsResponseDto, UpiVpaLookupRequest>(
            path = "api/device/upi-vpa-bank-details",
            body = UpiVpaLookupRequest(upiVpa = upiVpa, userId = userId)
        )
        emit(result)
    }

    override fun lookupUpiVpa(
        userId: String,
        upiVpa: String
    ): Flow<Resource<UpiVpaLookupResponseDto>> = flow {
        emit(Resource.Loading())
        val result = apiClient.post<UpiVpaLookupResponseDto, UpiVpaLookupRequest>(
            path = "api/device/upi-vpa-lookup",
            body = UpiVpaLookupRequest(upiVpa = upiVpa, userId = userId)
        )
        emit(result)
    }

    override fun pollUpiVpaLookupStatus(
        userId: String,
        upiVpa: String
    ): Flow<Resource<UpiVpaLookupResponseDto>> = flow {
        emit(Resource.Loading())
        val result = apiClient.get<UpiVpaLookupResponseDto>(
            path = "api/device/upi-vpa-lookup/status"
        ) {
            url.parameters.append("upiVpa", upiVpa)
            url.parameters.append("userId", userId)
        }
        emit(result)
    }
}
