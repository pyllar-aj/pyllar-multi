package com.pyllar.consumer.data.repository

import com.pyllar.consumer.data.remote.model.dto.DashboardResponseDto
import com.pyllar.consumer.data.remote.model.dto.InvestorDashboardResponseV2Dto
import com.pyllar.consumer.data.remote.network.PyllarApiClient
import com.pyllar.consumer.domain.repository.DashboardRepository
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class DashboardRepositoryImpl(
    private val apiClient: PyllarApiClient
) : DashboardRepository {

    override fun getDashboard(userId: String): Flow<Resource<DashboardResponseDto>> = flow {
        emit(Resource.Loading())
        when (val result =
            apiClient.get<DashboardResponseDto>(
                path = "api/dashboard/$userId?includeGrowthHistory=true&growthDays=90"
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

    override fun getDashboardV2(userId: String): Flow<Resource<InvestorDashboardResponseV2Dto>> = flow {
        emit(Resource.Loading())
        when (val result =
            apiClient.get<InvestorDashboardResponseV2Dto>(
                path = "api/dashboardv2/$userId"
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

    override fun getTransactions(request: com.pyllar.consumer.data.remote.requests.TransactionDetailsRequest): Flow<Resource<com.pyllar.consumer.data.remote.model.dto.TransactionDetailsResponseDto>> = flow {
        emit(Resource.Loading())
        when (val result = apiClient.post<com.pyllar.consumer.data.remote.model.dto.TransactionDetailsResponseDto, com.pyllar.consumer.data.remote.requests.TransactionDetailsRequest>(
            path = "api/invDashboard/transactions",
            body = request
        )) {
            is Resource.Success -> emit(Resource.Success(
                data = result.data,
                navigation = result.navigation,
                fieldErrors = result.fieldErrors
            ))
            is Resource.Error -> emit(Resource.Error(
                message = result.message ?: "",
                navigation = result.navigation,
                fieldErrors = result.fieldErrors,
                errorType = result.errorType
            ))
            is Resource.Loading -> emit(Resource.Loading())
        }
    }

    override fun initGoalTxn(request: com.pyllar.consumer.data.remote.requests.GoalSelectionRequest): Flow<Resource<com.pyllar.consumer.data.remote.model.dto.GoalSelectionResponseDto>> = flow {
        emit(Resource.Loading())
        when (val result = apiClient.post<com.pyllar.consumer.data.remote.model.dto.GoalSelectionResponseDto, com.pyllar.consumer.data.remote.requests.GoalSelectionRequest>(
            path = "api/goaltxn/init",
            body = request
        )) {
            is Resource.Success -> emit(Resource.Success(
                data = result.data,
                navigation = result.navigation,
                fieldErrors = result.fieldErrors
            ))
            is Resource.Error -> emit(Resource.Error(
                message = result.message ?: "",
                navigation = result.navigation,
                fieldErrors = result.fieldErrors,
                errorType = result.errorType
            ))
            is Resource.Loading -> emit(Resource.Loading())
        }
    }
}

