package com.pyllar.consumer.platform

import kotlinx.cinterop.useContents
import platform.CoreLocation.CLLocationManager
import platform.CoreLocation.CLLocationManagerDelegateProtocol
import platform.CoreLocation.kCLLocationAccuracyBest
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.suspendCancellableCoroutine
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
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                suspendCancellableCoroutine { continuation ->
                    val delegate = object : NSObject(), CLLocationManagerDelegateProtocol {
                        override fun locationManager(manager: CLLocationManager, didUpdateLocations: List<*>) {
                            val location = didUpdateLocations.lastOrNull() as? platform.CoreLocation.CLLocation
                            if (location != null) {
                                manager.stopUpdatingLocation()
                                currentDelegate = null
                                val lat = location.coordinate.useContents { latitude }
                                val lon = location.coordinate.useContents { longitude }
                                if (continuation.isActive) {
                                    continuation.resume(
                                        LocationCoordinates(
                                            latitude = lat,
                                            longitude = lon
                                        )
                                    )
                                }
                            }
                        }

                        override fun locationManager(manager: CLLocationManager, didFailWithError: platform.Foundation.NSError) {
                            manager.stopUpdatingLocation()
                            currentDelegate = null
                            if (continuation.isActive) {
                                continuation.resume(null)
                            }
                        }
                    }

                    currentDelegate = delegate
                    locationManager.delegate = delegate
                    locationManager.desiredAccuracy = kCLLocationAccuracyBest
                    locationManager.startUpdatingLocation()

                    continuation.invokeOnCancellation {
                        locationManager.stopUpdatingLocation()
                        if (locationManager.delegate === delegate) {
                            locationManager.delegate = null
                        }
                        if (currentDelegate === delegate) {
                            currentDelegate = null
                        }
                    }
                }
            }
        }
    }

    override suspend fun reverseGeocode(latitude: Double, longitude: Double): GeocodedAddress? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
        suspendCoroutine { continuation ->
            val geocoder = platform.CoreLocation.CLGeocoder()
            val location = platform.CoreLocation.CLLocation(latitude = latitude, longitude = longitude)
            geocoder.reverseGeocodeLocation(location) { placemarks, error ->
                val placemark = placemarks?.firstOrNull() as? platform.CoreLocation.CLPlacemark
                if (placemark != null) {
                    val city = placemark.locality ?: placemark.subAdministrativeArea ?: placemark.administrativeArea ?: ""
                    val pincode = placemark.postalCode ?: ""
                    continuation.resume(GeocodedAddress(city = city, pincode = pincode))
                } else {
                    continuation.resume(null)
                }
            }
        }
    }
}
