package com.pyllar.consumer.data.repository

import com.pyllar.consumer.data.remote.model.dto.ScreenDataResponseDto
import com.pyllar.consumer.data.remote.network.PyllarApiClient
import com.pyllar.consumer.domain.repository.CommonRepository
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class CommonRepositoryImpl(
    private val apiClient: PyllarApiClient
) : CommonRepository {
    override fun fetchScreenData(screenName: String): Flow<Resource<ScreenDataResponseDto>> = flow {
        emit(Resource.Loading())
        val result = apiClient.get<ScreenDataResponseDto>("api/screen-data/$screenName")
        emit(result)
    }
}
