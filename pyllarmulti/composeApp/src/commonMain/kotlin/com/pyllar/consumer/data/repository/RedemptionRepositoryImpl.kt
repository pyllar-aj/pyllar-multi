package com.pyllar.consumer.data.repository

import com.pyllar.consumer.data.remote.model.dto.RedemptionRequest
import com.pyllar.consumer.data.remote.model.dto.RedemptionResponse
import com.pyllar.consumer.data.remote.network.PyllarApiClient
import com.pyllar.consumer.domain.repository.RedemptionRepository
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class RedemptionRepositoryImpl(
    private val apiClient: PyllarApiClient
) : RedemptionRepository {

    override fun createRedemption(request: RedemptionRequest): Flow<Resource<RedemptionResponse>> = flow {
        emit(Resource.Loading())
        when (val result =
            apiClient.post<RedemptionResponse, RedemptionRequest>(
                path = "api/mf-redemptions",
                body = request
            )
        ) {
            is Resource.Success -> emit(
                Resource.Success(
                    data = result.data,
                    navigation = result.navigation,
                    fieldErrors = result.fieldErrors
                )
            )

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

    override fun generateRedemptionOtp(userId: String): Flow<Resource<com.pyllar.consumer.data.remote.model.dto.RedemptionOtpResponseDto>> = flow {
        emit(Resource.Loading())
        val result = apiClient.post<com.pyllar.consumer.data.remote.model.dto.RedemptionOtpResponseDto, Unit>(
            path = "api/mf-redemptions/generate-redemption-otp?userId=$userId",
            body = Unit
        )
        emit(result)
    }

    override fun verifyRedemptionOtp(request: com.pyllar.consumer.data.remote.model.dto.RedemptionOtpVerifyRequestDto): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        val result = apiClient.post<String, com.pyllar.consumer.data.remote.model.dto.RedemptionOtpVerifyRequestDto>(
            path = "api/mf-redemptions/redemption-otp-verify",
            body = request
        )
        emit(result)
    }
}

