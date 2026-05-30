package com.pyllar.consumer.update

import com.pyllar.consumer.navigation.ForceUpdateManager

// Android update flow is owned by InAppUpdateManager called from MainActivity.
actual suspend fun checkPlatformForUpdates(manager: ForceUpdateManager) = Unit
actual fun onOptionalUpdateDismissed() = Unit
