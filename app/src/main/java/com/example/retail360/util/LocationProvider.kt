package com.example.retail360.util

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Simple lat/lng holder returned to the UI. */
data class LatLng(val lat: Double, val lng: Double, val accuracyM: Float = 0f)

/**
 * Thin coroutine wrapper over the fused location provider, plus the geofence
 * distance check the check-in screen relies on.
 */
class LocationProvider(private val context: Context) {

    @SuppressLint("MissingPermission") // caller must have requested ACCESS_FINE_LOCATION
    suspend fun current(): LatLng? = suspendCancellableCoroutine { cont ->
        val client = LocationServices.getFusedLocationProviderClient(context)
        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { loc ->
                cont.resume(loc?.let { LatLng(it.latitude, it.longitude, it.accuracy) })
            }
            .addOnFailureListener { cont.resume(null) }
    }

    companion object {
        /** Metres between two points (Android's Location.distanceBetween under the hood). */
        fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
            val out = FloatArray(1)
            Location.distanceBetween(lat1, lng1, lat2, lng2, out)
            return out[0]
        }
    }
}
