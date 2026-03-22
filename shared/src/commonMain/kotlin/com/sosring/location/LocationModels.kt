package com.sosring.location

import kotlinx.coroutines.flow.Flow

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val isLive: Boolean = true
) {
    fun toShareText(): String =
        "https://maps.google.com/?q=$latitude,$longitude" +
        if (!isLive) " (last known)" else ""
}

sealed class LocationState {
    data class Available(val data: LocationData) : LocationState()
    object GpsDisabled : LocationState()
    object Unavailable : LocationState()
}

interface LocationProvider {
    val locationState: Flow<LocationState>
    suspend fun getLastKnownLocation(): LocationData?
}
