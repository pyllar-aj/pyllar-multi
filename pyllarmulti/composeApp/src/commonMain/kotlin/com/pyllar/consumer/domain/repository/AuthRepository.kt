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
}

