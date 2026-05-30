package com.pyllar.consumer.navigation

import androidx.compose.runtime.*
import com.pyllar.consumer.presentation.components.ForceUpdateInfo

/**
 * Manages force update state across the application.
 * Used to trigger force update dialog when API responses indicate update is required.
 */
class ForceUpdateManager {

    // Force update (blocking dialog, cannot be dismissed)
    private var _forceUpdateInfo: ForceUpdateInfo? by mutableStateOf(null)

    val forceUpdateInfo: ForceUpdateInfo?
        get() = _forceUpdateInfo

    val requiresForceUpdate: Boolean
        get() = _forceUpdateInfo != null

    // Optional update (dismissible bottom sheet — iOS App Store, set by IosAppStoreUpdateChecker)
    private var _optionalUpdateUrl: String? by mutableStateOf(null)

    val optionalUpdateUrl: String?
        get() = _optionalUpdateUrl

    fun setForceUpdate(updateUrl: String?, webUrl: String?, message: String?) {
        _forceUpdateInfo = ForceUpdateInfo(
            updateUrl = updateUrl ?: "market://details?id=com.pyllar.consumer",
            webUrl = webUrl ?: "https://play.google.com/store/apps/details?id=com.pyllar.consumer&hl=en_IN",
            message = message ?: "A new version of the app is available. Please update to continue using Pyllar."
        )
    }

    fun clearForceUpdate() {
        _forceUpdateInfo = null
    }

    fun setOptionalUpdate(storeUrl: String) {
        _optionalUpdateUrl = storeUrl
    }

    fun clearOptionalUpdate() {
        _optionalUpdateUrl = null
    }
}
