package com.pyllar.consumer.data.repository

import com.pyllar.consumer.data.remote.model.dto.CreateDailySipRequestDto
import com.pyllar.consumer.data.remote.model.dto.FundDetailsResponseDto
import com.pyllar.consumer.data.remote.model.dto.MandateWrapper
import com.pyllar.consumer.data.remote.network.PyllarApiClient
import com.pyllar.consumer.domain.repository.FundDetailsRepository
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FundDetailsRepositoryImpl(
    private val apiClient: PyllarApiClient
) : FundDetailsRepository {

    override fun getFundDetails(isin: String): Flow<Resource<FundDetailsResponseDto>> = flow {
        emit(Resource.Loading())
        when (val result =
            apiClient.get<FundDetailsResponseDto>(
                path = "api/funds/$isin/details"
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

    override fun getFundDetailsByGoal(
        userId: String,
        goalType: String
    ): Flow<Resource<FundDetailsResponseDto>> = flow {
        emit(Resource.Loading())
        when (val result =
            apiClient.get<FundDetailsResponseDto>(
                path = "api/funds/details?userId=$userId&goalType=$goalType"
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

    override fun createDailySip(
        request: CreateDailySipRequestDto
    ): Flow<Resource<MandateWrapper>> = flow {
        emit(Resource.Loading())
        val result = apiClient.post<MandateWrapper, CreateDailySipRequestDto>(
            path = "api/mf-purchase-plans/create-daily-sip",
            body = request
        )
        emit(result)
    }

    override fun getInvestmentLimits(
        userInvestmentPurposeId: String
    ): Flow<Resource<com.pyllar.consumer.data.remote.model.dto.InvestmentLimitsResponseDto>> = flow {
        emit(Resource.Loading())
        val result = apiClient.get<com.pyllar.consumer.data.remote.model.dto.InvestmentLimitsResponseDto>(
            path = "api/goaltxn/limits/$userInvestmentPurposeId"
        )
        emit(result)
    }

    override fun syncMandate(
        request: com.pyllar.consumer.data.remote.requests.PollMandateRequest
    ): Flow<Resource<com.pyllar.consumer.data.remote.model.dto.MandateStatus>> = flow {
        emit(Resource.Loading())
        val result = apiClient.post<com.pyllar.consumer.data.remote.model.dto.MandateStatus, com.pyllar.consumer.data.remote.requests.PollMandateRequest>(
            path = "api/mandates/sync-mandate",
            body = request
        )
        emit(result)
    }

    override fun pollPurchasePlanStatus(
        userId: String,
        mandateRef: Long
    ): Flow<Resource<Boolean>> = flow {
        emit(Resource.Loading())
        val result = apiClient.post<Boolean, com.pyllar.consumer.data.remote.requests.PlanPollRequest>(
            path = "api/mf-purchase-plans/poll-purchase-plan-status",
            body = com.pyllar.consumer.data.remote.requests.PlanPollRequest(
                userId = userId,
                mandateRef = mandateRef,
                mfppId = null
            )
        )
        emit(result)
    }
}
