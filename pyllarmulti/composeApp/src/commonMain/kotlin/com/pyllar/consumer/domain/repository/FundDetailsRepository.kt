package com.pyllar.consumer.domain.repository

import com.pyllar.consumer.data.remote.model.dto.FundDetailsResponseDto
import com.pyllar.consumer.data.remote.model.dto.CreateDailySipRequestDto
import com.pyllar.consumer.data.remote.model.dto.MandateWrapper
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.flow.Flow

interface FundDetailsRepository {
    fun getFundDetails(isin: String): Flow<Resource<FundDetailsResponseDto>>

    fun getFundDetailsByGoal(
        userId: String,
        goalType: String
    ): Flow<Resource<FundDetailsResponseDto>>

    fun createDailySip(
        request: CreateDailySipRequestDto
    ): Flow<Resource<MandateWrapper>>

    fun getInvestmentLimits(
        userInvestmentPurposeId: String
    ): Flow<Resource<com.pyllar.consumer.data.remote.model.dto.InvestmentLimitsResponseDto>>

    fun syncMandate(
        request: com.pyllar.consumer.data.remote.requests.PollMandateRequest
    ): Flow<Resource<com.pyllar.consumer.data.remote.model.dto.MandateStatus>>
}

