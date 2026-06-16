package com.pyllar.consumer.data.repository

import com.pyllar.consumer.data.remote.model.dto.ReferralCodeDto
import com.pyllar.consumer.data.remote.model.dto.ReferralDashboardDto
import com.pyllar.consumer.data.remote.model.dto.ReferralStatsOnlyDto
import com.pyllar.consumer.data.remote.model.dto.CoinRedemptionResponseDto
import com.pyllar.consumer.data.remote.model.dto.CoinRedemptionHistoryDto
import com.pyllar.consumer.data.remote.model.dto.CoinRedemptionRequestBodyDto
import com.pyllar.consumer.data.remote.network.PyllarApiClient
import com.pyllar.consumer.domain.repository.ReferralRepository
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ReferralRepositoryImpl(
    private val apiClient: PyllarApiClient
) : ReferralRepository {

    override fun getMyCode(userId: String): Flow<Resource<ReferralCodeDto>> = flow {
        emit(Resource.Loading())
        when (val result = apiClient.get<ReferralCodeDto>("api/referral/my-code")) {
            is Resource.Success -> emit(
                Resource.Success(
                    data = result.data,
                    navigation = result.navigation,
                    fieldErrors = result.fieldErrors
                )
            )
            is Resource.Error -> emit(
                Resource.Error(
                    message = result.message ?: "Failed to fetch referral code",
                    navigation = result.navigation,
                    fieldErrors = result.fieldErrors,
                    errorType = result.errorType
                )
            )
            is Resource.Loading -> emit(Resource.Loading())
        }
    }

    override fun getMyStats(userId: String): Flow<Resource<ReferralStatsOnlyDto>> = flow {
        emit(Resource.Loading())
        when (val result = apiClient.get<ReferralStatsOnlyDto>("api/referral/my-stats")) {
            is Resource.Success -> emit(
                Resource.Success(
                    data = result.data,
                    navigation = result.navigation,
                    fieldErrors = result.fieldErrors
                )
            )
            is Resource.Error -> emit(
                Resource.Error(
                    message = result.message ?: "Failed to fetch referral stats",
                    navigation = result.navigation,
                    fieldErrors = result.fieldErrors,
                    errorType = result.errorType
                )
            )
            is Resource.Loading -> emit(Resource.Loading())
        }
    }

    override fun getMyDashboard(userId: String): Flow<Resource<ReferralDashboardDto>> = flow {
        emit(Resource.Loading())
        when (val result = apiClient.get<ReferralDashboardDto>("api/referral/my-dashboard")) {
            is Resource.Success -> emit(
                Resource.Success(
                    data = result.data,
                    navigation = result.navigation,
                    fieldErrors = result.fieldErrors
                )
            )
            is Resource.Error -> emit(
                Resource.Error(
                    message = result.message ?: "Failed to fetch referral dashboard",
                    navigation = result.navigation,
                    fieldErrors = result.fieldErrors,
                    errorType = result.errorType
                )
            )
            is Resource.Loading -> emit(Resource.Loading())
        }
    }

    override fun requestRedemption(userId: String, coins: Int): Flow<Resource<CoinRedemptionResponseDto>> = flow {
        emit(Resource.Loading())
        val body = CoinRedemptionRequestBodyDto(coins)
        when (val result = apiClient.post<CoinRedemptionResponseDto, CoinRedemptionRequestBodyDto>("api/referral/redeem-coins", body)) {
            is Resource.Success -> emit(
                Resource.Success(
                    data = result.data,
                    navigation = result.navigation,
                    fieldErrors = result.fieldErrors
                )
            )
            is Resource.Error -> emit(
                Resource.Error(
                    message = result.message ?: "Redemption failed",
                    navigation = result.navigation,
                    fieldErrors = result.fieldErrors,
                    errorType = result.errorType
                )
            )
            is Resource.Loading -> emit(Resource.Loading())
        }
    }

    override fun getRedemptionHistory(userId: String): Flow<Resource<CoinRedemptionHistoryDto>> = flow {
        emit(Resource.Loading())
        when (val result = apiClient.get<CoinRedemptionHistoryDto>("api/referral/redemption-history")) {
            is Resource.Success -> emit(
                Resource.Success(
                    data = result.data,
                    navigation = result.navigation,
                    fieldErrors = result.fieldErrors
                )
            )
            is Resource.Error -> emit(
                Resource.Error(
                    message = result.message ?: "Failed to fetch history",
                    navigation = result.navigation,
                    fieldErrors = result.fieldErrors,
                    errorType = result.errorType
                )
            )
            is Resource.Loading -> emit(Resource.Loading())
        }
    }
}

