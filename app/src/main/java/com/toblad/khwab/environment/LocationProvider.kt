package com.toblad.khwab.environment

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Retrieves the device's approximate location using the
 * platform LocationManager. No Google Play Services
 * dependency is required.
 */
class LocationProvider(
    context: Context
) {

    private val appContext = context.applicationContext

    private val locationManager: LocationManager? =
        appContext.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    fun hasLocationPermission(): Boolean {

        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    appContext,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Returns the freshest last-known location from any
     * enabled provider, or null if none is available.
     */
    fun lastKnownLocation(): Location? {

        val manager = locationManager ?: return null

        if (!hasLocationPermission()) return null

        return try {
            manager.getProviders(true)
                .mapNotNull { provider -> manager.getLastKnownLocation(provider) }
                .maxByOrNull { it.time }
        } catch (e: SecurityException) {
            null
        }
    }

    /**
     * Requests a single fresh location fix, suspending until
     * a fix is received or the request fails. Falls back to
     * the last known location if a live fix cannot be made.
     */
    suspend fun requestSingleUpdate(): Location? {

        val manager = locationManager ?: return null

        if (!hasLocationPermission()) return null

        val provider = when {
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) ->
                LocationManager.GPS_PROVIDER

            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ->
                LocationManager.NETWORK_PROVIDER

            else -> return lastKnownLocation()
        }

        return suspendCancellableCoroutine { continuation ->

            val listener = object : LocationListener {

                override fun onLocationChanged(location: Location) {
                    manager.removeUpdates(this)
                    if (continuation.isActive) {
                        continuation.resume(location)
                    }
                }
            }

            try {
                manager.requestSingleUpdate(
                    provider,
                    listener,
                    Looper.getMainLooper()
                )
            } catch (e: SecurityException) {
                if (continuation.isActive) {
                    continuation.resume(lastKnownLocation())
                }
                return@suspendCancellableCoroutine
            }

            continuation.invokeOnCancellation {
                manager.removeUpdates(listener)
            }
        }
    }

    /**
     * Returns the freshest coordinate available: a live fix
     * if one can be obtained, falling back to the last known
     * location.
     */
    suspend fun getCurrentLocation(): Location? {
        return requestSingleUpdate() ?: lastKnownLocation()
    }
}