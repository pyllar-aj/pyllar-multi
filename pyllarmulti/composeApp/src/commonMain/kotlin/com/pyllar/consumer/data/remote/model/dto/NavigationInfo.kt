package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Navigation information coming from API responses.
 */
@Serializable
data class NavigationInfo(
    @SerialName("nextScreen")
    val nextScreen: String?,
    @SerialName("action")
    val action: NavigationAction?,
    @SerialName("params")
    val params: Map<String, String>? = null,
    @SerialName("nextPayload")
    val nextPayload: Map<String, String>? = null,
    @SerialName("payloadType")
    val payloadType: String? = null,
    @SerialName("payloadVersion")
    val payloadVersion: Int? = null
) {
    val hasNextScreen: Boolean
        get() = !nextScreen.isNullOrBlank()

    val hasParams: Boolean
        get() = !params.isNullOrEmpty()

    fun shouldNavigate(): Boolean = action == NavigationAction.NAVIGATE || action == NavigationAction.REPLACE
    fun shouldPoll(): Boolean = action == NavigationAction.POLL
    fun shouldStay(): Boolean = action == NavigationAction.STAY
    fun requiresManualVerification(): Boolean = params?.get("requiresManualVerification") == "true"
    fun getMessage(): String? = params?.get("message")
}

/**
 * Navigation actions that can be performed based on API responses.
 * Must match backend NavigationAction enum values.
 */
@Serializable
enum class NavigationAction {
    @SerialName("NAVIGATE")
    NAVIGATE,
    @SerialName("REPLACE")
    REPLACE,
    @SerialName("FINISH_FLOW")
    FINISH_FLOW,
    @SerialName("RETRY")
    RETRY,
    @SerialName("REDIRECT_AUTH")
    REDIRECT_AUTH,
    @SerialName("STAY")
    STAY,
    @SerialName("POLL")
    POLL,
    @SerialName("FORCE_UPDATE")
    FORCE_UPDATE
}

