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
import com.pyllar.consumer.config.IS_DEBUG

@OptIn(ExperimentalForeignApi::class)
class IosLocationProvider : LocationProvider {

    private val locationManager = CLLocationManager()
    private var currentDelegate: CLLocationManagerDelegateProtocol? = null

    private fun isSimulator(): Boolean {
        val name = platform.UIKit.UIDevice.currentDevice.name
        val isSimName = name.contains("Simulator", ignoreCase = true)
        val isSimEnv = try {
            val env = platform.Foundation.NSProcessInfo.processInfo.environment
            env["SIMULATOR_UDID"] != null || env["SIMULATOR_DEVICE_NAME"] != null
        } catch (e: Exception) {
            false
        }
        return isSimName || isSimEnv
    }

    override suspend fun getCurrentLocation(): LocationCoordinates? {
        if (IS_DEBUG && isSimulator()) {
            com.pyllar.consumer.util.platformLog("IosLocationProvider: Debug Simulator detected. Overriding location to India (Bangalore).")
            return LocationCoordinates(
                latitude = 12.971598,
                longitude = 77.594566
            )
        }

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
