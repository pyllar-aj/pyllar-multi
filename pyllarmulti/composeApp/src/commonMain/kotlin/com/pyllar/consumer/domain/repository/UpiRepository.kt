package com.pyllar.consumer.domain.repository

import com.pyllar.consumer.data.remote.model.dto.UpiVpaVerifyResponseDto
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.flow.Flow

interface UpiRepository {
    fun verifyVpa(vpa: String): Flow<Resource<UpiVpaVerifyResponseDto>>
}
