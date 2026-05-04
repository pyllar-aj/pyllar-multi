package com.pyllar.consumer.data.remote.datasource

import com.pyllar.consumer.data.remote.model.dto.AuthUserResponseDto
import com.pyllar.consumer.data.remote.model.dto.OtpSendResponseDto
import com.pyllar.consumer.data.remote.model.dto.UpdateEmailResponseDto
import com.pyllar.consumer.data.remote.model.dto.EsignCreateResponseDto
import com.pyllar.consumer.data.remote.model.UpdateEmailRequest
import com.pyllar.consumer.data.remote.network.PyllarApiClient
import com.pyllar.consumer.data.remote.requests.OtpRegistrationRequest
import com.pyllar.consumer.data.remote.requests.OtpVerificationRequest
import com.pyllar.consumer.domain.models.PhoneVerificationRequest
import com.pyllar.consumer.data.remote.model.dto.PhoneVerificationResponseDto
import com.pyllar.consumer.util.Resource

interface AuthRemoteDataSource {
    suspend fun sendOtp(request: OtpRegistrationRequest): Resource<OtpSendResponseDto>
    suspend fun verifyPhone(request: PhoneVerificationRequest): Resource<PhoneVerificationResponseDto>
    suspend fun verifyOtp(request: OtpVerificationRequest): Resource<AuthUserResponseDto>
    suspend fun updateEmail(request: UpdateEmailRequest): Resource<UpdateEmailResponseDto>
    suspend fun uploadSignatureFile(bytes: ByteArray, kycAttemptId: String): Resource<EsignCreateResponseDto>
}

class AuthRemoteDataSourceImpl(
    private val apiClient: PyllarApiClient
) : AuthRemoteDataSource {

    override suspend fun sendOtp(request: OtpRegistrationRequest): Resource<OtpSendResponseDto> {
        return apiClient.post("api/auth/otp/send", request)
    }

    override suspend fun verifyPhone(request: PhoneVerificationRequest): Resource<PhoneVerificationResponseDto> {
        return apiClient.post("api/auth/verify-phone", request)
    }

    override suspend fun verifyOtp(request: OtpVerificationRequest): Resource<AuthUserResponseDto> {
        return apiClient.post("api/auth/verify-otp", request)
    }

    override suspend fun updateEmail(request: UpdateEmailRequest): Resource<UpdateEmailResponseDto> {
        return apiClient.post("api/kyc/onboarding/updateEm", request)
    }

    override suspend fun uploadSignatureFile(bytes: ByteArray, kycAttemptId: String): Resource<EsignCreateResponseDto> {
        return apiClient.postMultipart<EsignCreateResponseDto>(
            path = "api/kyc/onboarding/upload-file",
            formData = {
                append("kycAttemptId", kycAttemptId)
                append("file", bytes, io.ktor.http.Headers.build {
                    append(io.ktor.http.HttpHeaders.ContentType, "image/png")
                    append(io.ktor.http.HttpHeaders.ContentDisposition, "filename=\"signature.png\"")
                })
            }
        )
    }
}

