package com.pyllar.consumer.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AndroidPermissionManager(
    private val context: Context
) : PermissionManager {

    private var notifLauncher: ActivityResultLauncher<String>? = null
    private var locationLauncher: ActivityResultLauncher<String>? = null

    private var notifContinuation: ((Boolean) -> Unit)? = null
    private var locationContinuation: ((Boolean) -> Unit)? = null

    /**
     * Called from MainActivity.onCreate() after registering the launchers.
     */
    fun setLaunchers(
        notificationLauncher: ActivityResultLauncher<String>,
        locationLauncher: ActivityResultLauncher<String>
    ) {
        this.notifLauncher = notificationLauncher
        this.locationLauncher = locationLauncher
    }

    /** Called by the notification ActivityResultLauncher callback in MainActivity. */
    fun onNotificationResult(granted: Boolean) {
        notifContinuation?.invoke(granted)
        notifContinuation = null
    }

    /** Called by the location ActivityResultLauncher callback in MainActivity. */
    fun onLocationResult(granted: Boolean) {
        locationContinuation?.invoke(granted)
        locationContinuation = null
    }

    override fun checkStatus(): PermissionStatus {
        val notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }

        val locationGranted = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gpsEnabled = LocationManagerCompat.isLocationEnabled(locationManager)

        return PermissionStatus(
            notificationsGranted = notificationsGranted,
            locationGranted = locationGranted,
            gpsEnabled = gpsEnabled
        )
    }

    override suspend fun requestNotifications(): Boolean {
        // On API < 33 notifications don't require a runtime grant
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
        // Already granted — return immediately
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) return true

        val launcher = notifLauncher ?: return false

        return suspendCancellableCoroutine { cont ->
            notifContinuation = { granted -> cont.resume(granted) }
            cont.invokeOnCancellation { notifContinuation = null }
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override suspend fun requestLocation(): Boolean {
        // Already granted — return immediately
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) return true

        val launcher = locationLauncher ?: return false

        return suspendCancellableCoroutine { cont ->
            locationContinuation = { granted -> cont.resume(granted) }
            cont.invokeOnCancellation { locationContinuation = null }
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
}
