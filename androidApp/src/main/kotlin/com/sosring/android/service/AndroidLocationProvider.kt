package com.sosring.android.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.LocationManager
import com.google.android.gms.location.*
import com.sosring.location.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*

class AndroidLocationProvider(private val context: Context) : LocationProvider {
    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    override val locationState: Flow<LocationState> = callbackFlow {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
            !lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            trySend(LocationState.GpsDisabled)
        }
        val req = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10_000).build()
        val cb = object : LocationCallback() {
            override fun onLocationResult(r: LocationResult) {
                r.lastLocation?.let {
                    trySend(LocationState.Available(LocationData(it.latitude, it.longitude)))
                }
            }
        }
        fusedClient.requestLocationUpdates(req, cb, context.mainLooper)
        awaitClose { fusedClient.removeLocationUpdates(cb) }
    }.distinctUntilChanged()

    @SuppressLint("MissingPermission")
    override suspend fun getLastKnownLocation(): LocationData? {
        return try {
            val loc = fusedClient.lastLocation.result
            loc?.let { LocationData(it.latitude, it.longitude, isLive = false) }
        } catch (e: Exception) { null }
    }
}
