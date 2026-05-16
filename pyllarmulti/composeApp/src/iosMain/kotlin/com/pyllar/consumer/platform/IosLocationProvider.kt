package com.pyllar.consumer.platform

import kotlinx.cinterop.useContents
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.cinterop.ExperimentalForeignApi

@OptIn(ExperimentalForeignApi::class)
class IosLocationProvider : LocationProvider {

    private val locationManager = CLLocationManager()
    private var currentDelegate: CLLocationManagerDelegateProtocol? = null

    override suspend fun getCurrentLocation(): LocationCoordinates? {
        return withTimeoutOrNull(15000L) {
            suspendCoroutine { continuation ->
                val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
                    override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
                        val location = didUpdateLocations.lastOrNull() as? platform.CoreLocation.CLLocation
                        if (location != null) {
                            manager.stopUpdatingLocation()
                            location.coordinate.useContents {
                                continuation.resume(
                                    LocationCoordinates(
                                        latitude = latitude,
                                        longitude = longitude
                                    )
                                )
                            }
                        }
                    }

                    override fun locationManager(manager: CLLocationManager, didFailWithError: platform.Foundation.NSError) {
                        manager.stopUpdatingLocation()
                        continuation.resume(null)
                    }
                }

                currentDelegate = delegate
                locationManager.delegate = delegate
                locationManager.desiredAccuracy = kCLLocationAccuracyBest
                locationManager.startUpdatingLocation()
            }
        }
    }
}
