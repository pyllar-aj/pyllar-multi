package com.pyllar.consumer.data.remote.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AdditionalKycRequest(
    @SerialName("marital_status")
    val maritalStatus: String,
    @SerialName("occupation_type")
    val occupationType: String,
    @SerialName("father_name")
    val fatherName: String,
    @SerialName("annual_income")
    val annualIncome: String,
    @SerialName("is_politically_exposed")
    val isPoliticallyExposed: Boolean,
    @SerialName("nationality_country")
    val nationalityCountry: String,
    @SerialName("place_of_birth")
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

