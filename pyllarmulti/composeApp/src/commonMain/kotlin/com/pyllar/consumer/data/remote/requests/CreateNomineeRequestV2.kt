package com.pyllar.consumer.data.remote.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request data class for creating nominee details (V2)
 * Matches the backend CreateNomineeRequestV2 DTO
 * Supports multiple nominees in a list
 */
@Serializable
data class CreateNomineeRequestV2(
    @SerialName("user_id")
    val userId: String,

    @SerialName("kyc_attempt_id")
    val kycAttemptId: String,

    @SerialName("investor_id")
    val investorId: String,

    @SerialName("wants_to_add_nominee")
    val wantsToAddNominee: Boolean = false,

    @SerialName("nomineeDetails")
    val nomineeDetails: List<NomineeDetailsRequest>? = null
)

