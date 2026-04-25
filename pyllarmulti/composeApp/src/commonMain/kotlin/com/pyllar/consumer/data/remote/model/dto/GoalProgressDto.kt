package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.Serializable

@Serializable
data class GoalProgressDto(
    val goalId: String?,
    val goalName: String?,
    val targetAmount: Double?,
    val currentAmount: Double?,
    val progressPercentage: Double?,
    val targetDate: String?,
    val isAchieved: Boolean?,
    val daysRemaining: Int?
)
