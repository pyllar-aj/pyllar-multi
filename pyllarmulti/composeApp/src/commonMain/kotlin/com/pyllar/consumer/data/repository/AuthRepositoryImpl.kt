package com.pyllar.consumer.data.repository

import com.pyllar.consumer.data.remote.datasource.AuthRemoteDataSource
import com.pyllar.consumer.data.remote.model.UpdateEmailRequest
import com.pyllar.consumer.data.remote.requests.OtpRegistrationRequest
import com.pyllar.consumer.data.remote.requests.OtpVerificationRequest
import com.pyllar.consumer.domain.models.AuthToken
import com.pyllar.consumer.domain.models.AuthUserDTO
import com.pyllar.consumer.domain.models.UpdateEmailResponse
import com.pyllar.consumer.domain.repository.AuthRepository
import com.pyllar.consumer.domain.storage.SessionStore
import com.pyllar.consumer.util.Resource
import com.pyllar.consumer.util.platformLog
import com.pyllar.consumer.data.remote.parser.ErrorType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

import com.pyllar.consumer.platform.AttributionProvider

class AuthRepositoryImpl(
    private val remote: AuthRemoteDataSource,
    private val sessionStore: SessionStore,
    private val attributionProvider: AttributionProvider
) : AuthRepository {

    override fun checkPreviousAuthUser(): Flow<Resource<AuthToken>> = flow {
        val token = sessionStore.getAuthToken()
        if (token != null) {
            emit(Resource.Success(token))
        } else {
            emit(Resource.Error("No previous authentication found"))
        }
    }

    override fun sendOtp(request: OtpRegistrationRequest): Flow<Resource<AuthToken>> = flow {
        emit(Resource.Loading())
        val enrichedRequest = request.copy(
            utmSource = request.utmSource ?: sessionStore.getValue("utm_source") ?: attributionProvider.getMediaSource(),
            utmMedium = request.utmMedium ?: sessionStore.getValue("utm_medium") ?: attributionProvider.getChannel(),
            utmCampaign = request.utmCampaign ?: sessionStore.getValue("utm_campaign") ?: attributionProvider.getCampaign(),
            utmTerm = request.utmTerm ?: sessionStore.getValue("utm_term"),
            utmContent = request.utmContent ?: sessionStore.getValue("utm_content") ?: attributionProvider.getAdSet(),
            utmCampaignId = request.utmCampaignId ?: sessionStore.getValue("utm_campaign_id") ?: attributionProvider.getCampaignId(),
            gclid = request.gclid ?: sessionStore.getValue("gclid") ?: attributionProvider.getGclid(),
            gbraid = request.gbraid ?: sessionStore.getValue("gbraid") ?: attributionProvider.getGbraid(),
            wbraid = request.wbraid ?: sessionStore.getValue("wbraid") ?: attributionProvider.getWbraid(),
            afMediaSource = request.afMediaSource ?: attributionProvider.getMediaSource(),
            afCampaign = request.afCampaign ?: attributionProvider.getCampaign(),
            afCampaignId = request.afCampaignId ?: attributionProvider.getCampaignId(),
            afAdSet = request.afAdSet ?: attributionProvider.getAdSet(),
            afStatus = request.afStatus ?: attributionProvider.getAfStatus(),
            afChannel = request.afChannel ?: attributionProvider.getChannel()
        )
        when (val result = remote.sendOtp(enrichedRequest)) {
            is Resource.Success -> {
                val data = result.data
                if (data != null) {
                    val actualUserId = data.actualUserId
                    if (!actualUserId.isNullOrBlank()) {
                        com.pyllar.consumer.util.Log.d("AuthRepository", "💾 [sendOtp] Saving actualUserId: $actualUserId")
                        sessionStore.saveUserId(actualUserId)
                    }

                    val authToken = AuthToken(
                        token = "",
                        userId = actualUserId ?: "",
                        auth_token = "",
                        otpRef = data.ref,
                        phoneNumber = data.phoneNumber
                    )
                    emit(Resource.Success(authToken, result.navigation, result.fieldErrors))
                } else {
                    emit(
                        Resource.Error(
                            "Empty response data",
                            result.navigation,
                            result.fieldErrors,
                            ErrorType.UNKNOWN_ERROR
                        )
                    )
                }
            }

            is Resource.Error -> {
                emit(
                    Resource.Error(
                        result.message ?: "Unknown error",
                        result.navigation,
                        result.fieldErrors,
                        result.errorType
                    )
                )
            }

            is Resource.Loading -> emit(Resource.Loading())
        }
    }

    override fun verifyOtp(request: OtpVerificationRequest): Flow<Resource<AuthUserDTO>> = flow {
        emit(Resource.Loading())
        when (val result = remote.verifyOtp(request)) {
            is Resource.Success -> {
                val data = result.data
                if (data != null) {
                    val storedUserId = sessionStore.getCurrentUserId()
                    val bestUserId = if (data.userId == "anonymous" || data.userId.isBlank()) {
                        com.pyllar.consumer.util.Log.d("AuthRepository", "⚠️ [verifyOtp] Server returned anonymous/blank userId, falling back to stored: $storedUserId")
                        storedUserId
                    } else {
                        data.userId
                    }

                    com.pyllar.consumer.util.Log.d("AuthRepository", "🔐 [verifyOtp] Final userId for session: $bestUserId")

                    val domainUser = AuthUserDTO(
                        token = data.authToken,
                        userId = bestUserId,
                        phoneNumber = data.phoneNumber,
                        email = "",
                        firstName = "",
                        lastName = "",
                        newUser = true
                    )
                    // Persist token via SessionStore
                    val finalPhone = if (data.phoneNumber.isNotBlank()) data.phoneNumber else request.phoneNumber
                    platformLog("AuthRepository: Saving AuthToken with finalPhone: $finalPhone (from response: ${data.phoneNumber}, from request: ${request.phoneNumber})")
                    sessionStore.saveAuthToken(
                        AuthToken(
                            auth_token = data.authToken,
                            token = data.authToken,
                            userId = bestUserId,
                            phoneNumber = finalPhone
                        )
                    )
                    emit(Resource.Success(domainUser, result.navigation, result.fieldErrors))
                } else {
                    emit(
                        Resource.Error(
                            "Empty response data",
                            result.navigation,
                            result.fieldErrors,
                            ErrorType.UNKNOWN_ERROR
                        )
                    )
                }
            }

            is Resource.Error -> {
                emit(
                    Resource.Error(
                        result.message ?: "Unknown error",
                        result.navigation,
                        result.fieldErrors,
                        result.errorType
                    )
                )
            }

            is Resource.Loading -> emit(Resource.Loading())
        }
    }

    override fun updateEmail(email: String, userId: String): Flow<Resource<UpdateEmailResponse>> = flow {
        emit(Resource.Loading())
        val request = UpdateEmailRequest(
            email = email,
            userId = userId,
            utmSource = sessionStore.getValue("utm_source") ?: attributionProvider.getMediaSource(),
            utmMedium = sessionStore.getValue("utm_medium") ?: attributionProvider.getChannel(),
            utmCampaign = sessionStore.getValue("utm_campaign") ?: attributionProvider.getCampaign(),
            utmTerm = sessionStore.getValue("utm_term"),
            utmContent = sessionStore.getValue("utm_content") ?: attributionProvider.getAdSet(),
            utmCampaignId = sessionStore.getValue("utm_campaign_id") ?: attributionProvider.getCampaignId(),
            gclid = sessionStore.getValue("gclid") ?: attributionProvider.getGclid(),
            gbraid = sessionStore.getValue("gbraid") ?: attributionProvider.getGbraid(),
            wbraid = sessionStore.getValue("wbraid") ?: attributionProvider.getWbraid()
        )
        when (val result = remote.updateEmail(request)) {
            is Resource.Success -> {
                val data = result.data
                if (data != null) {
                    val domain = UpdateEmailResponse(
                        email = data.email,
                        message = data.message,
                        isUpdated = data.updated,
                        isNewUser = data.newUser ?: false,
                        isMismatch = data.mismatch ?: false,
                        isError = data.error ?: false
                    )
                    val existingToken = sessionStore.getCurrentToken()
                    val existingPhone = sessionStore.getCurrentPhone()
                    sessionStore.saveUserSession(userId = userId, email = email, phone = existingPhone, authToken = existingToken)
                    emit(Resource.Success(domain, result.navigation, result.fieldErrors))
                } else {
                    emit(
                        Resource.Error(
                            "Empty response data",
                            result.navigation,
                            result.fieldErrors,
                            ErrorType.UNKNOWN_ERROR
                        )
                    )
                }
            }
            is Resource.Error -> {
                emit(
                    Resource.Error(
                        result.message ?: "Unknown error",
                        result.navigation,
                        result.fieldErrors,
                        result.errorType
                    )
                )
            }
            is Resource.Loading -> emit(Resource.Loading())
        }
    }

    override fun uploadSignatureFile(bytes: ByteArray, kycAttemptId: String): Flow<Resource<com.pyllar.consumer.data.remote.model.dto.EsignCreateResponseDto>> = flow {
        emit(Resource.Loading())
        val result = remote.uploadSignatureFile(bytes, kycAttemptId)
        emit(result)
    }

    override fun getDigiLink(
        userId: String,
        name: String,
        emailAddress: String,
        dateOfBirth: String,
        mobileCountryCode: String,
        mobileNumber: String,
        preVerificationId: String?,
        docId: String?,
        kycRequestId: String?
    ): Flow<Resource<com.pyllar.consumer.data.remote.model.MinimalKycResponse>> = flow {
        emit(Resource.Loading())
        val request = com.pyllar.consumer.data.remote.model.DigiLinkRequest(
            userId = userId,
            name = name,
            mobile = com.pyllar.consumer.data.remote.model.Mobile(
                countryCode = mobileCountryCode,
                number = mobileNumber
            ),
            emailAddress = emailAddress,
            dateOfBirth = dateOfBirth,
            preVerificationId = preVerificationId,
            docId = docId,
            kycRequestId = kycRequestId
        )
        val result = remote.getDigiLink(request)
        emit(result)
    }
}

