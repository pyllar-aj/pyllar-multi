package com.pyllar.consumer.platform

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

class AndroidLocationProvider(
    private val context: Context
) : LocationProvider {

    private val fusedLocationClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): LocationCoordinates? {
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
}
