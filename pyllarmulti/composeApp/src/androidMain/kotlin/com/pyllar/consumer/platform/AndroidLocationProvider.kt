package com.pyllar.consumer.platform

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import com.pyllar.consumer.BuildConfig

import android.location.Geocoder
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidLocationProvider(
    private val context: Context
) : LocationProvider {

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    private fun isEmulator(): Boolean {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_gphone")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator")
    }

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): LocationCoordinates? {
        if (BuildConfig.DEBUG && isEmulator()) {
            android.util.Log.d("AndroidLocationProvider", "Debug Emulator detected. Overriding location to India (Bangalore).")
            return LocationCoordinates(
                latitude = 12.971598,
                longitude = 77.594566
            )
        }

        return try {
            // 1. Try to get last known location first (instant)
            val lastLocation = fusedLocationClient.lastLocation.await()
            if (lastLocation != null) {
                return LocationCoordinates(
                    latitude = lastLocation.latitude,
                    longitude = lastLocation.longitude
                )
            }

            // 2. If last location is null, request a fresh location with timeout
            val cts = CancellationTokenSource()
            val freshLocation = withTimeoutOrNull(15000L) {
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    cts.token
                ).await()
            }

            freshLocation?.let {
                LocationCoordinates(
                    latitude = it.latitude,
                    longitude = it.longitude
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun reverseGeocode(latitude: Double, longitude: Double): GeocodedAddress? = withContext(Dispatchers.IO) {
        try {
            if (!Geocoder.isPresent()) {
                return@withContext null
            }
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val city = address.locality ?: address.subAdminArea ?: address.adminArea ?: ""
                val pincode = address.postalCode ?: ""
                GeocodedAddress(city = city, pincode = pincode)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
