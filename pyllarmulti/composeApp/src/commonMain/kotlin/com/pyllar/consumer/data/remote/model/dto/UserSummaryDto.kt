package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserSummaryDto(
    val userId: String?,
    val name: String?,
    val email: String?,
    val phoneNumber: String?,
    val profileStatus: String?,
    val lastLoginAt: String?,
    val investorId: String?,
    val riskProfile: String?
)
