package com.pyllar.consumer.update

import com.pyllar.consumer.navigation.ForceUpdateManager

/**
 * Platform-specific update check:
 *  - Android: no-op here — InAppUpdateManager in MainActivity handles Play Core.
 *  - iOS: fetches the latest version from the iTunes Search API and signals
 *         ForceUpdateManager when an update is needed.
 */
expect suspend fun checkPlatformForUpdates(manager: ForceUpdateManager)

/**
 * Called when the user dismisses the optional update bottom sheet.
 *  - Android: no-op.
 *  - iOS: records the current timestamp so the 72-hour throttle can skip
 *         the sheet on the next launch.
 */
expect fun onOptionalUpdateDismissed()
