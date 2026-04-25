package com.pyllar.consumer.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MinimalKycRequest(
    val userId: String,
    val name: String,
    val panNumber: String,
    val dateOfBirth: String,
    val emailAddress: String,
    val mobile: Mobile,
    val preVerificationId: String? = null
)

@Serializable
data class Mobile(
    val countryCode: String,
    val number: String
)

@Serializable
data class MinimalKycResponse(
    @SerialName("reUrl")
    val reUrl: String? = null,
    @SerialName("kycAttemptId")
    val kycAttemptId: String? = null
)

@Serializable
data class DigiLinkRequest(
    val userId: String,
    val name: String,
    val mobile: Mobile,
    val emailAddress: String,
    val dateOfBirth: String,
    val preVerificationId: String? = null,
    val docId: String? = null,
    val kycRequestId: String? = null
)

