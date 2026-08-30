package com.example.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.data.model.GeocodingResult
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

class LocationHelper(private val context: Context) {

    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    fun hasLocationPermission(): Boolean {
        val fineLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocationGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocationGranted || coarseLocationGranted
    }

    suspend fun getCurrentGpsLocation(): Location? = withContext(Dispatchers.IO) {
        if (!hasLocationPermission()) return@withContext null

        try {
            // Attempt 1: High accuracy current location
            val currentLocation = fetchCurrentLocationInternal()
            if (currentLocation != null) return@withContext currentLocation

            // Attempt 2: Last known location fallback
            return@withContext fetchLastLocationInternal()
        } catch (e: Exception) {
            try {
                return@withContext fetchLastLocationInternal()
            } catch (e2: Exception) {
                null
            }
        }
    }

    private suspend fun fetchCurrentLocationInternal(): Location? =
        suspendCancellableCoroutine { continuation ->
            try {
                if (!hasLocationPermission()) {
                    continuation.resume(null)
                    return@suspendCancellableCoroutine
                }

                val cts = CancellationTokenSource()
                continuation.invokeOnCancellation {
                    cts.cancel()
                }

                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cts.token
                ).addOnSuccessListener { location ->
                    if (continuation.isActive) {
                        continuation.resume(location)
                    }
                }.addOnFailureListener {
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }.addOnCanceledListener {
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
            } catch (e: SecurityException) {
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
        }

    private suspend fun fetchLastLocationInternal(): Location? =
        suspendCancellableCoroutine { continuation ->
            try {
                if (!hasLocationPermission()) {
                    continuation.resume(null)
                    return@suspendCancellableCoroutine
                }

                fusedLocationClient.lastLocation
                    .addOnSuccessListener { location ->
                        if (continuation.isActive) {
                            continuation.resume(location)
                        }
                    }
                    .addOnFailureListener {
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
                    .addOnCanceledListener {
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
            } catch (e: SecurityException) {
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            } catch (e: Exception) {
                if (continuation.isActive) {
                    continuation.resume(null)
                }
            }
        }

    suspend fun reverseGeocode(latitude: Double, longitude: Double): GeocodingResult =
        withContext(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                val addresses: List<Address>? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    suspendCancellableCoroutine { cont ->
                        geocoder.getFromLocation(latitude, longitude, 1) { list ->
                            if (cont.isActive) {
                                cont.resume(list)
                            }
                        }
                    }
                } else {
                    @Suppress("DEPRECATION")
                    geocoder.getFromLocation(latitude, longitude, 1)
                }

                val address = addresses?.firstOrNull()
                if (address != null) {
                    val cityName = address.locality
                        ?: address.subAdminArea
                        ?: address.adminArea
                        ?: address.featureName
                        ?: "My Location"
                    val countryName = address.countryName ?: "Current Location"
                    val countryCode = address.countryCode ?: ""
                    val adminArea = address.adminArea

                    return@withContext GeocodingResult(
                        name = cityName,
                        latitude = latitude,
                        longitude = longitude,
                        country = countryName,
                        countryCode = countryCode,
                        admin1 = adminArea
                    )
                }
            } catch (e: Exception) {
                // Ignore and use fallback
            }

            GeocodingResult(
                name = "Current Location",
                latitude = latitude,
                longitude = longitude,
                country = "GPS Detected",
                countryCode = "GPS",
                admin1 = null
            )
        }
}
