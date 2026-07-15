package com.pyllar.consumer.platform

import kotlinx.coroutines.suspendCancellableCoroutine
import platform.CoreLocation.CLAuthorizationStatus
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedAlways
import platform.CoreLocation.kCLAuthorizationStatusAuthorizedWhenInUse
import platform.CoreLocation.kCLAuthorizationStatusNotDetermined
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNUserNotificationCenter
import platform.darwin.NSObject
import platform.darwin.dispatch_semaphore_create
import platform.darwin.dispatch_semaphore_signal
import platform.darwin.dispatch_semaphore_wait
import platform.darwin.DISPATCH_TIME_FOREVER
import kotlin.coroutines.resume
import kotlinx.coroutines.launch

class IosPermissionManager : PermissionManager {

    private val locationManager = CLLocationManager()

    // Cached notification status — updated after every requestNotifications() call.
    // Starts as false (unknown → treat as not granted) for the initial checkStatus().
    private var cachedNotifGranted: Boolean = false

    override fun checkStatus(): PermissionStatus {
        val locationStatus = CLLocationManager.authorizationStatus()
        val locationGranted = locationStatus == kCLAuthorizationStatusAuthorizedWhenInUse ||
                locationStatus == kCLAuthorizationStatusAuthorizedAlways
        val gpsEnabled = CLLocationManager.locationServicesEnabled()

        UNUserNotificationCenter.currentNotificationCenter()
            .getNotificationSettingsWithCompletionHandler { settings ->
                if (settings != null) {
                    val isAuthorized = settings.authorizationStatus == UNAuthorizationStatusAuthorized
                    platform.darwin.dispatch_async(platform.darwin.dispatch_get_main_queue()) {
                        cachedNotifGranted = isAuthorized
                    }
                }
            }

        return PermissionStatus(
            notificationsGranted = cachedNotifGranted,
            locationGranted = locationGranted,
            gpsEnabled = gpsEnabled
        )
    }

    override suspend fun requestNotifications(): Boolean {
        return suspendCancellableCoroutine { cont ->
            UNUserNotificationCenter.currentNotificationCenter()
                .requestAuthorizationWithOptions(
                    UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
                ) { granted, _ ->
                    cachedNotifGranted = granted
                    if (cont.isActive) cont.resume(granted)
                }
        }
    }

    private var activeLocationDelegate: CLLocationManagerDelegateProtocol? = null

    override suspend fun requestLocation(): Boolean {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            val currentStatus = CLLocationManager.authorizationStatus()
            com.pyllar.consumer.util.platformLog("IosPermissionManager: requestLocation - currentStatus: $currentStatus")
            
            if (currentStatus == kCLAuthorizationStatusAuthorizedWhenInUse ||
                currentStatus == kCLAuthorizationStatusAuthorizedAlways
            ) {
                return@withContext true
            }
            if (currentStatus != kCLAuthorizationStatusNotDetermined) {
                com.pyllar.consumer.util.platformLog("IosPermissionManager: Permission already denied or restricted, returning false")
                return@withContext false
            }

            suspendCancellableCoroutine { cont ->
                val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
                    override fun locationManager(
                        manager: CLLocationManager,
                        didChangeAuthorizationStatus: CLAuthorizationStatus
                    ) {
                        com.pyllar.consumer.util.platformLog("IosPermissionManager delegate: didChangeAuthorizationStatus called with status: $didChangeAuthorizationStatus")
                        if (didChangeAuthorizationStatus == kCLAuthorizationStatusNotDetermined) return
                        val granted = didChangeAuthorizationStatus == kCLAuthorizationStatusAuthorizedWhenInUse ||
                                didChangeAuthorizationStatus == kCLAuthorizationStatusAuthorizedAlways
                        
                        activeLocationDelegate = null
                        locationManager.delegate = null
                        if (cont.isActive) cont.resume(granted)
                    }
                }
                activeLocationDelegate = delegate
                locationManager.delegate = delegate
                locationManager.desiredAccuracy = kCLLocationAccuracyBest
                locationManager.requestWhenInUseAuthorization()
                
                cont.invokeOnCancellation { 
                    activeLocationDelegate = null
                    locationManager.delegate = null 
                }
            }
        }
    }
}
