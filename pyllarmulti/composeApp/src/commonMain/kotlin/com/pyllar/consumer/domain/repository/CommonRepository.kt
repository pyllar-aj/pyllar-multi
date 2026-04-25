package com.pyllar.consumer.domain.repository

import com.pyllar.consumer.data.remote.model.dto.ScreenDataResponseDto
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.flow.Flow

interface CommonRepository {
    fun fetchScreenData(screenName: String): Flow<Resource<ScreenDataResponseDto>>
}
