package com.pyllar.consumer.data.repository

import com.pyllar.consumer.data.remote.model.dto.UpiVpaVerifyRequestDto
import com.pyllar.consumer.data.remote.model.dto.UpiVpaVerifyResponseDto
import com.pyllar.consumer.data.remote.network.PyllarApiClient
import com.pyllar.consumer.domain.repository.UpiRepository
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class UpiRepositoryImpl(
    private val apiClient: PyllarApiClient
) : UpiRepository {
    override fun verifyVpa(vpa: String): Flow<Resource<UpiVpaVerifyResponseDto>> = flow {
        emit(Resource.Loading())
        val result = apiClient.post<UpiVpaVerifyResponseDto, UpiVpaVerifyRequestDto>(
            path = "api/upi/verify-vpa",
            body = UpiVpaVerifyRequestDto(vpa = vpa)
        )
        emit(result)
    }
}
