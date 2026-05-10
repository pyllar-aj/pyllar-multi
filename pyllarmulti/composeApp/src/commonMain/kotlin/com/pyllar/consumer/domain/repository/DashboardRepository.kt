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

    fun pauseSip(userId: String, planId: String?, mandateId: Long): Flow<Resource<String>>
    fun resumeSip(userId: String, planId: String?, mandateId: Long): Flow<Resource<String>>
    fun cancelSip(userId: String, planId: String?, mandateId: Long, reason: String): Flow<Resource<String>>
    fun pollActionStatus(userId: String, actionId: String, action: String): Flow<Resource<Map<String, String>>>
}

