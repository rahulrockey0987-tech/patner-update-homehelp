package com.example.util

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

data class DistanceResult(
    val distanceKm: Double,
    val estimatedMinutes: Int,
    val isRoadEstimated: Boolean,
    val formattedDistance: String,
    val formattedDuration: String
)

object LocationHelper {

    /**
     * Calculates distance between partner GPS and customer GPS using Haversine formula
     * with an urban road winding factor (1.28x) to closely estimate realistic road driving distance.
     */
    fun calculateDistance(
        startLat: Double,
        startLng: Double,
        endLat: Double,
        endLng: Double
    ): DistanceResult {
        val earthRadiusKm = 6371.0

        val dLat = Math.toRadians(endLat - startLat)
        val dLng = Math.toRadians(endLng - startLng)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(startLat)) * cos(Math.toRadians(endLat)) *
                sin(dLng / 2) * sin(dLng / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        val straightLineKm = earthRadiusKm * c

        // In urban city driving, road distance is typically ~1.25x - 1.35x straight line
        val roadDistanceKm = (straightLineKm * 1.28)
        val roundedKm = (roadDistanceKm * 10).roundToInt() / 10.0

        // Average city transit speed: ~18-22 km/h in dense areas
        val averageSpeedKmH = 20.0
        val travelHours = roadDistanceKm / averageSpeedKmH
        val minutes = (travelHours * 60).roundToInt().coerceAtLeast(3)

        val distString = if (roundedKm < 1.0) {
            "${(roundedKm * 1000).toInt()} m away"
        } else {
            "%.1f km away".format(roundedKm)
        }

        val durString = if (minutes < 60) "$minutes mins" else "${minutes / 60}h ${minutes % 60}m"

        return DistanceResult(
            distanceKm = roundedKm,
            estimatedMinutes = minutes,
            isRoadEstimated = true,
            formattedDistance = distString,
            formattedDuration = durString
        )
    }

    /**
     * Intent to open navigation in Google Maps
     */
    fun openGoogleMapsNavigation(context: Context, destLat: Double, destLng: Double, label: String) {
        val uri = Uri.parse("google.navigation:q=$destLat,$destLng&mode=d")
        val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }
        if (mapIntent.resolveActivity(context.packageManager) != null) {
            context.startActivity(mapIntent)
        } else {
            // Fallback to browser or generic map viewer
            val fallbackUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$destLat,$destLng")
            context.startActivity(Intent(Intent.ACTION_VIEW, fallbackUri))
        }
    }

    @SuppressLint("MissingPermission")
    fun fetchCurrentLocation(
        context: Context,
        onLocationReceived: (lat: Double, lng: Double) -> Unit,
        onError: () -> Unit
    ) {
        try {
            val fusedLocationClient: FusedLocationProviderClient =
                LocationServices.getFusedLocationProviderClient(context)

            val cts = CancellationTokenSource()
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                .addOnSuccessListener { loc: Location? ->
                    if (loc != null) {
                        onLocationReceived(loc.latitude, loc.longitude)
                    } else {
                        // Try last location
                        fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                            if (lastLoc != null) {
                                onLocationReceived(lastLoc.latitude, lastLoc.longitude)
                            } else {
                                onError()
                            }
                        }.addOnFailureListener {
                            onError()
                        }
                    }
                }
                .addOnFailureListener {
                    onError()
                }
        } catch (_: Exception) {
            onError()
        }
    }
}
