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

        // Refresh notification cache synchronously via semaphore (quick OS cache read)
        val semaphore = dispatch_semaphore_create(0)
        UNUserNotificationCenter.currentNotificationCenter()
            .getNotificationSettingsWithCompletionHandler { settings ->
                cachedNotifGranted = settings?.authorizationStatus == UNAuthorizationStatusAuthorized
                dispatch_semaphore_signal(semaphore)
            }
        dispatch_semaphore_wait(semaphore, DISPATCH_TIME_FOREVER)

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
        val currentStatus = CLLocationManager.authorizationStatus()
        if (currentStatus == kCLAuthorizationStatusAuthorizedWhenInUse ||
            currentStatus == kCLAuthorizationStatusAuthorizedAlways
        ) {
            return true
        }
        if (currentStatus != kCLAuthorizationStatusNotDetermined) {
            return false
        }

        return suspendCancellableCoroutine { cont ->
            val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
                override fun locationManager(
                    manager: CLLocationManager,
                    didChangeAuthorizationStatus: CLAuthorizationStatus
                ) {
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
