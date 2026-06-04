package com.pyllar.consumer.domain.repository

import com.pyllar.consumer.data.remote.model.dto.ReferralCodeDto
import com.pyllar.consumer.data.remote.model.dto.ReferralDashboardDto
import com.pyllar.consumer.data.remote.model.dto.ReferralStatsOnlyDto
import com.pyllar.consumer.util.Resource
import kotlinx.coroutines.flow.Flow

interface ReferralRepository {
    fun getMyCode(userId: String): Flow<Resource<ReferralCodeDto>>
    fun getMyStats(userId: String): Flow<Resource<ReferralStatsOnlyDto>>
    fun getMyDashboard(userId: String): Flow<Resource<ReferralDashboardDto>>
}
