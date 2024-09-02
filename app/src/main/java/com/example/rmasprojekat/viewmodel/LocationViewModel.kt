package com.example.rmasprojekat.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.rmasprojekat.location.LocationManager
import com.google.android.gms.common.api.ResolvableApiException

class LocationViewModel(private val locationManager: LocationManager) : ViewModel() {
    val isLocationEnabled: LiveData<Boolean> = locationManager.isLocationEnabled
    val currentLocation = locationManager.location

    fun checkLocationPermission(): Boolean = locationManager.checkLocationPermission()

    fun checkLocationEnabled() = locationManager.checkLocationEnabled()

    fun requestLocationSettings(
        onSuccess: () -> Unit,
        onFailure: (ResolvableApiException) -> Unit
    ) = locationManager.requestLocationSettings(onSuccess, onFailure)

    fun startLocationUpdates(onUpdate: (android.location.Location) -> Unit) =
        locationManager.requestLocationUpdates(onUpdate)

    fun stopLocationUpdates() = locationManager.stopLocationUpdates()
}