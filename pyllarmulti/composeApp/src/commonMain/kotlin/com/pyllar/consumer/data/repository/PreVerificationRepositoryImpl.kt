package com.pyllar.consumer.data.repository

import com.pyllar.consumer.data.remote.network.PyllarApiClient
import com.pyllar.consumer.data.remote.dto.PanFetchRequestDto
import com.pyllar.consumer.data.remote.dto.PanFetchDataDto
import com.pyllar.consumer.data.remote.dto.PanVerifyOtpRequestDto
import com.pyllar.consumer.data.remote.dto.PanVerifyOtpDataDto
import com.pyllar.consumer.data.remote.dto.PreVerificationRequestDto
import com.pyllar.consumer.data.remote.dto.PreVerificationRequestHelper
import com.pyllar.consumer.data.remote.dto.PreVerificationResponseDto
import com.pyllar.consumer.domain.repository.PreVerificationRepository
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class PreVerificationRepositoryImpl(
    private val apiClient: PyllarApiClient
) : PreVerificationRepository {

    override fun checkInvestorReadiness(
        panNumber: String
    ): Flow<Resource<PreVerificationResponseDto>> = flow {
        emit(Resource.Loading())
        val result = apiClient.post<PreVerificationResponseDto, Unit>(
            path = "api/pre-verification/readiness-check?panNumber=$panNumber",
            body = Unit
        )
        emit(result)
    }

    override fun startAutomaticVerification(
        panNumber: String,
        name: String,
        accountNumber: String,
        ifscCode: String,
        accountType: String
    ): Flow<Resource<PreVerificationResponseDto>> = flow {
        emit(Resource.Loading())
        val request = PreVerificationRequestHelper.createAutoVerificationRequest(
            panNumber = panNumber,
            name = name,
            accountNumber = accountNumber,
            ifscCode = ifscCode,
            accountType = accountType
        )
        val result = apiClient.post<PreVerificationResponseDto, PreVerificationRequestDto>(
            path = "api/pre-verification/bank-accounts/verify",
            body = request
        )
        emit(result)
    }

    override fun fetchVerificationStatus(
        preVerificationId: String
    ): Flow<Resource<PreVerificationResponseDto>> = flow {
        emit(Resource.Loading())
        val result = apiClient.get<PreVerificationResponseDto>(
            path = "api/pre-verification/$preVerificationId"
        )
        emit(result)
    }

    override fun pollVerificationStatus(
        preVerificationId: String,
        maxAttempts: Int,
        intervalSeconds: Long
    ): Flow<Resource<PreVerificationResponseDto>> = flow {
        emit(Resource.Loading())
        var attempts = 0
        while (attempts < maxAttempts) {
            try {
                val result = apiClient.get<PreVerificationResponseDto>(
                    path = "api/pre-verification/$preVerificationId"
                )
                emit(result)
                
                if (result is Resource.Success) {
                    val isCompleted = result.data?.data?.isCompleted() == true
                    if (isCompleted) {
                        break
                    }
                }
                
                delay(intervalSeconds * 1000)
                attempts++
            } catch (e: Exception) {
                emit(Resource.Error(e.message ?: "Polling failed"))
                break
            }
        }
        
        if (attempts >= maxAttempts) {
            emit(Resource.Error("Verification timeout - please try again"))
        }
    }

    override fun performManualVerification(
        panNumber: String,
        name: String,
        accountNumber: String,
        ifscCode: String,
        bankAccountProof: String,
        accountType: String
    ): Flow<Resource<PreVerificationResponseDto>> = flow {
        emit(Resource.Loading())
        val request = PreVerificationRequestHelper.createManualVerificationRequest(
            panNumber = panNumber,
            name = name,
            accountNumber = accountNumber,
            ifscCode = ifscCode,
            accountType = accountType,
            bankAccountProof = bankAccountProof
        )
        val result = apiClient.post<PreVerificationResponseDto, PreVerificationRequestDto>(
            path = "api/pre-verification/bank-accounts/verify/manual",
            body = request
        )
        emit(result)
    }

    override fun initiatePanFetch(
        mobileNumber: String
    ): Flow<Resource<PanFetchDataDto>> = flow {
        emit(Resource.Loading())
        val result = apiClient.post<PanFetchDataDto, PanFetchRequestDto>(
            path = "api/userprefill/consent/initiate",
            body = PanFetchRequestDto(mobileNumber)
        )
        emit(result)
    }

    override fun verifyOtpAndFetchPan(
        mobileNumber: String,
        prefillId: Long,
        otp: String
    ): Flow<Resource<PanVerifyOtpDataDto>> = flow {
        emit(Resource.Loading())
        val result = apiClient.post<PanVerifyOtpDataDto, PanVerifyOtpRequestDto>(
            path = "api/userprefill/consent/verify",
            body = PanVerifyOtpRequestDto(
                mobileNumber = mobileNumber,
                prefillId = prefillId,
                otp = otp
            )
        )
        emit(result)
    }
}
