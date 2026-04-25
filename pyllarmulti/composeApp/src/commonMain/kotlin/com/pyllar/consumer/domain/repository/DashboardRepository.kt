package com.pyllar.consumer.domain.repository

import com.pyllar.consumer.data.remote.model.dto.DashboardResponseDto
import com.pyllar.consumer.data.remote.model.dto.InvestorDashboardResponseV2Dto
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.flow.Flow

import com.pyllar.consumer.data.remote.requests.TransactionDetailsRequest
import com.pyllar.consumer.data.remote.model.dto.TransactionDetailsResponseDto

interface DashboardRepository {
    fun getDashboard(userId: String): Flow<Resource<DashboardResponseDto>>

    fun getDashboardV2(userId: String): Flow<Resource<InvestorDashboardResponseV2Dto>>
    
    fun getTransactions(request: TransactionDetailsRequest): Flow<Resource<TransactionDetailsResponseDto>>

    fun initGoalTxn(request: com.pyllar.consumer.data.remote.requests.GoalSelectionRequest): Flow<Resource<com.pyllar.consumer.data.remote.model.dto.GoalSelectionResponseDto>>
}

