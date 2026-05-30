package com.pyllar.consumer.data.repository

import com.pyllar.consumer.data.remote.model.dto.ReferralCodeDto
import com.pyllar.consumer.data.remote.model.dto.ReferralStatsOnlyDto
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
}
