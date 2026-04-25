package com.pyllar.consumer.data.remote.requests

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request data class for creating nominee details
 * Matches the backend CreateNomineeRequest DTO
 */
@Serializable
data class CreateNomineeRequest(
    @SerialName("user_id")
    val userId: String,

    @SerialName("kyc_attempt_id")
    val kycAttemptId: String,

    @SerialName("investor_id")
    val investorId: String,

    // NEW: Add boolean field for user's choice
    @SerialName("wants_to_add_nominee")
    val wantsToAddNominee: Boolean = false,

    // Make nominee fields optional (nullable)
    @SerialName("nominee_name")
    val nomineeName: String? = null,

    @SerialName("nominee_relationship")
    val nomineeRelationship: String? = null,

    @SerialName("nominee_date_of_birth")
    val nomineeDateOfBirth: String? = null,

    @SerialName("nominee_pan_number")
    val nomineePanNumber: String? = null
)
