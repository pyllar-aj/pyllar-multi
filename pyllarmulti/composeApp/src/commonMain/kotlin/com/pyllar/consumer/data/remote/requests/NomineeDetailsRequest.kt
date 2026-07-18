package com.pyllar.consumer.data.remote.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request data class for individual nominee details
 * Matches the backend NomineeDetailsRequest DTO
 */
@Serializable
data class NomineeDetailsRequest(
    @SerialName("nominee_name")
    val nomineeName: String? = null,

    @SerialName("nominee_relationship")
    val nomineeRelationship: String? = null,

    @SerialName("nominee_date_of_birth")
    val nomineeDateOfBirth: String? = null,

    @SerialName("nominee_pan_number")
    val nomineePanNumber: String? = null,

    @SerialName("percentage")
    val percentage: Int? = null,

    @SerialName("email_address")
    val emailAddress: String? = null,

    @SerialName("phone_number")
    val phoneNumber: String? = null,

    @SerialName("line1")
    val line1: String? = null,

    @SerialName("pincode")
    val pincode: String? = null
)



