package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonPrimitive

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
    val params: Map<String, JsonElement>? = null,
    @SerialName("nextPayload")
    val nextPayload: Map<String, JsonElement>? = null,
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
    
    fun getParam(key: String): String? = try {
        params?.get(key)?.jsonPrimitive?.content
    } catch (e: Exception) {
        params?.get(key)?.toString()
    }
    
    fun requiresManualVerification(): Boolean = getParam("requiresManualVerification") == "true"
    fun getMessage(): String? = getParam("message")
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

