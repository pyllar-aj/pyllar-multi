package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
enum class UserDetailsFetchState {
    PENDING,
    IN_PROGRESS,
    SUCCESS,
    FAILED
}

@Serializable
data class UserDetailsFetchResponseDto(
    @SerialName("overallStatus")
    val overallStatus: UserDetailsFetchState = UserDetailsFetchState.PENDING,

    @SerialName("mobileAccountStatus")
    val mobileAccountStatus: UserDetailsFetchState = UserDetailsFetchState.PENDING,

    @SerialName("creditBureauStatus")
    val creditBureauStatus: UserDetailsFetchState = UserDetailsFetchState.PENDING,

    @SerialName("errorMessage")
    val errorMessage: String? = null
)
