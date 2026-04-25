package com.pyllar.consumer.data.remote.model.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Serializable DTO for navigation info as returned by the server.
 * Kept for backwards compatibility with existing APIs.
 */
@Serializable
data class NavigationInfoDto(
    @SerialName("nextScreen")
    val nextScreen: String? = null,
    @SerialName("action")
    val action: String? = null,
    @SerialName("params")
    val params: Map<String, String>? = null
) {
    val hasNextScreen: Boolean
        get() = !nextScreen.isNullOrBlank()

    val hasParams: Boolean
        get() = !params.isNullOrEmpty()

    /**
     * Maps the raw string action to NavigationAction enum, defaulting to NAVIGATE.
     */
    val navigationAction: NavigationAction
        get() = when (action?.uppercase()) {
            "NAVIGATE" -> NavigationAction.NAVIGATE
            "REPLACE" -> NavigationAction.REPLACE
            "FINISH_FLOW" -> NavigationAction.FINISH_FLOW
            "RETRY" -> NavigationAction.RETRY
            "REDIRECT_AUTH" -> NavigationAction.REDIRECT_AUTH
            "STAY" -> NavigationAction.STAY
            "POLL" -> NavigationAction.POLL
            "FORCE_UPDATE" -> NavigationAction.FORCE_UPDATE
            else -> NavigationAction.NAVIGATE
        }
}

