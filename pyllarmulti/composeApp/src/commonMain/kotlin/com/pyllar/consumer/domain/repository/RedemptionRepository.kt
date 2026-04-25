package com.pyllar.consumer.domain.repository

import com.pyllar.consumer.data.remote.model.dto.RedemptionRequest
import com.pyllar.consumer.data.remote.model.dto.RedemptionResponse
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.flow.Flow

interface RedemptionRepository {
    fun createRedemption(request: RedemptionRequest): Flow<Resource<RedemptionResponse>>
    
    fun generateRedemptionOtp(userId: String): Flow<Resource<com.pyllar.consumer.data.remote.model.dto.RedemptionOtpResponseDto>>
    
    fun verifyRedemptionOtp(request: com.pyllar.consumer.data.remote.model.dto.RedemptionOtpVerifyRequestDto): Flow<Resource<String>>
}

