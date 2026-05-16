package com.pyllar.consumer.platform

/**
 * Platform-specific device information provider.
 *
 * Implementations should be lightweight wrappers around platform APIs
 * (e.g. Android [Build], [Settings.Secure], iOS UIDevice, etc.).
 */
interface DeviceInfoProvider {
    fun getDeviceId(): String?
    fun getOsName(): String
    fun getOsVersion(): String
    fun getAppVersion(): String?
}

/**
 * Abstraction over platform push-token storage.
 *
 * Implementations are responsible for returning the most recent
 * push token if available, or null when unknown.
 */
interface PushTokenProvider {
    suspend fun getPushToken(): String?
}

/**
 * Minimal analytics abstraction that shared ViewModels/screens
 * can depend on without importing platform SDKs directly.
 */
interface AnalyticsTracker {
    fun logEvent(name: String, params: Map<String, Any?> = emptyMap())
    fun logScreenView(screenName: String)
}

/**
 * Abstraction for triggering app-update checks from shared code.
 *
 * Platform implementations decide how to schedule or perform
 * the actual update check and UI.
 */
interface UpdateManager {
    /**
     * Request that the platform schedules or performs an update check.
     * The call should be safe to make repeatedly.
     */
    fun scheduleUpdateCheck()
}

/**
 * Snapshot of the current OS-level permission and GPS state.
 * All fields are synchronously readable on both Android and iOS.
 */
data class PermissionStatus(
    val notificationsGranted: Boolean,
    val locationGranted: Boolean,
    val gpsEnabled: Boolean
)

/**
 * Platform abstraction for runtime permission requests.
 *
 * - [checkStatus] is safe to call from a Composable (synchronous, no side effects).
 * - [requestNotifications] and [requestLocation] suspend until the OS dialog is
 *   dismissed. The flow always continues regardless of the user's choice.
 */
interface PermissionManager {
    fun checkStatus(): PermissionStatus
    suspend fun requestNotifications(): Boolean
    suspend fun requestLocation(): Boolean
}

/**
 * Data class for UPI app information
 */
data class UpiAppInfo(
    val packageName: String,
    val displayName: String,
    val icon: androidx.compose.ui.graphics.ImageBitmap? = null
)

/**
 * Interface for triggering platform-specific actions like opening URLs or sharing content.
 */
interface PlatformActions {
    fun openUrl(url: String)
    fun openUpiUrl(url: String, packageName: String? = null)
    fun shareText(text: String, title: String = "Share")
    fun openWhatsApp(phoneNumber: String, message: String)
    fun getInstalledUpiApps(): List<UpiAppInfo>
    fun openAppSettings()
}

/**
 * Platform-independent representation of geographic coordinates.
 */
data class LocationCoordinates(
    val latitude: Double,
    val longitude: Double
)

/**
 * Abstraction for fetching the current device location.
 *
 * Implementations should handle platform-specific location services
 * (e.g., FusedLocationProvider on Android, CLLocationManager on iOS).
 */
interface LocationProvider {
    /**
     * Attempts to fetch the current location.
     * Returns null if permissions are missing or location services are disabled.
     */
    suspend fun getCurrentLocation(): LocationCoordinates?
}
