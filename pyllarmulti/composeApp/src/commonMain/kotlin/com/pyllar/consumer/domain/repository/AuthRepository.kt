package com.pyllar.consumer.domain.repository

import com.pyllar.consumer.data.remote.requests.OtpRegistrationRequest
import com.pyllar.consumer.data.remote.requests.OtpVerificationRequest
import com.pyllar.consumer.domain.models.AuthToken
import com.pyllar.consumer.domain.models.AuthUserDTO
import com.pyllar.consumer.domain.models.UpdateEmailResponse
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun checkPreviousAuthUser(): Flow<Resource<AuthToken>>
    fun sendOtp(request: OtpRegistrationRequest): Flow<Resource<AuthToken>>
    fun verifyOtp(request: OtpVerificationRequest): Flow<Resource<AuthUserDTO>>
    fun updateEmail(email: String, userId: String): Flow<Resource<UpdateEmailResponse>>
    fun uploadSignatureFile(bytes: ByteArray, kycAttemptId: String): Flow<Resource<com.pyllar.consumer.data.remote.model.dto.EsignCreateResponseDto>>
    fun getDigiLink(
        userId: String,
        name: String,
        emailAddress: String,
        dateOfBirth: String,
        mobileCountryCode: String,
        mobileNumber: String,
        preVerificationId: String? = null,
        docId: String? = null,
        kycRequestId: String? = null
    ): Flow<Resource<com.pyllar.consumer.data.remote.model.MinimalKycResponse>>
}

