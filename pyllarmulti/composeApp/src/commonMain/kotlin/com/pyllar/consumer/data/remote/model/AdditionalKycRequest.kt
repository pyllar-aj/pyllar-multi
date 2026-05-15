package com.pyllar.consumer.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AdditionalKycRequest(
    @SerialName("maritalStatus")
    val maritalStatus: String,
    @SerialName("occupationType")
    val occupationType: String,
    @SerialName("fatherName")
    val fatherName: String,
    @SerialName("annualIncome")
    val annualIncome: String,
    @SerialName("isPoliticallyExposed")
    val isPoliticallyExposed: Boolean,
    @SerialName("nationality")
    val nationalityCountry: String,
    @SerialName("placeOfBirth")
    val placeOfBirth: String,
    val gender: String,
    val city: String,
    val pincode: String,
    @SerialName("addressLine1")
    val addressLine1: String? = null,
    @SerialName("addressLine2")
    val addressLine2: String? = null,
    @SerialName("addressLine3")
    val addressLine3: String? = null,
    @SerialName("geolocation")
    val geolocation: String? = null
)

