package com.example.rmasprojekat.viewmodel

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rmasprojekat.model.Canteen
import com.example.rmasprojekat.model.Review
import com.example.rmasprojekat.model.Status
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.IOException
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.CompletableDeferred
import java.util.Locale
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.example.rmasprojekat.model.User
import com.google.firebase.firestore.DocumentReference


class CanteenViewModel : ViewModel() {
    private val db = Firebase.firestore

    private val _canteens = MutableLiveData<List<Canteen>>()
    val canteens: LiveData<List<Canteen>> = _canteens
    private val _currentCanteenIndex = MutableLiveData<Int>(0)
    val currentCanteenIndex: LiveData<Int> = _currentCanteenIndex

    private val _isLoading = MutableLiveData<Boolean>(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _currentCity = MutableLiveData<String>("Loading...")
    val currentCity: LiveData<String> = _currentCity

    private var loadedCity: String? = null


    fun fetchCanteensForCity(city: String) {
        if (city == loadedCity && _canteens.value?.isNotEmpty() == true) {
            return  // Data already loaded for this city
        }

        viewModelScope.launch {
            _isLoading.value = true
            loadedCity = city

            try {
                val result = db.collection("canteens")
                    .whereEqualTo("city", city)
                    .orderBy("order")
                    .get()
                    .await()

                val canteenList = result.documents.map { document ->
                    document.toObject(Canteen::class.java)!!.apply {
                        id = document.id
                    }
                }

                fetchStatusesAndReviews(canteenList)
            } catch (e: Exception) {
                // Handle error
                _isLoading.value = false
            }
        }
    }

    private suspend fun fetchStatusesAndReviews(canteenList: List<Canteen>) {
        val updatedCanteenList = canteenList.map { canteen ->
            val reviewsRef = db.collection("canteens/${canteen.id}/reviews")
            val statusesRef = db.collection("canteens/${canteen.id}/statuses")

            try {
                val reviewsResult = reviewsRef.get().await()
                canteen.reviews = reviewsResult.documents.mapNotNull { reviewDoc ->
                    reviewDoc.toObject(Review::class.java)
                }

                val statusesResult = statusesRef.get().await()
                canteen.statuses = statusesResult.documents.mapNotNull { statusDoc ->
                    statusDoc.toObject(Status::class.java)
                }
            } catch (e: Exception) {
                // Handle failure to fetch reviews or statuses
                canteen.reviews = emptyList()
                canteen.statuses = emptyList()
            }

            canteen
        }

        _canteens.value = updatedCanteenList
        _currentCanteenIndex.value = 0
        _isLoading.value = false
    }

    fun fetchStatusesForCurrentCanteen() {
        val currentCanteen = getCurrentCanteen() ?: return

        viewModelScope.launch {
            _isLoading.value = true

            try {
                val statusesRef = db.collection("canteens/${currentCanteen.id}/statuses")
                val statusesResult = statusesRef.get().await()

                val updatedStatuses = statusesResult.documents.mapNotNull { statusDoc ->
                    statusDoc.toObject(Status::class.java)
                }

                currentCanteen.statuses = updatedStatuses

                // Update the canteen list with the new statuses
                _canteens.value = _canteens.value?.map { canteen ->
                    if (canteen.id == currentCanteen.id) {
                        canteen.copy(statuses = updatedStatuses)
                    } else {
                        canteen
                    }
                }

            } catch (e: Exception) {
                // Handle failure to fetch statuses
                currentCanteen.statuses = emptyList()
            }

            _isLoading.value = false
        }
    }

    fun getCurrentCanteen(): Canteen? {
        return canteens.value?.getOrNull(_currentCanteenIndex.value ?: 0)
    }

    fun setCurrentCanteenIndex(index: Int) {
        if (index in 0 until (canteens.value?.size ?: 0)) {
            _currentCanteenIndex.value = index
        }
    }

    fun setCurrentCity(city: String) {
        _currentCity.value = city
    }





    fun checkAndUpdateLocation(context: Context) {
        viewModelScope.launch {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // If we don't have permission, we can't get the location
                return@launch
            }

            val locationClient = LocationServices.getFusedLocationProviderClient(context)
            try {
                val location = locationClient.lastLocation.await()
                location?.let {
                    val detectedCity = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        getCityNameAsync(context, it.latitude, it.longitude)
                    } else {
                        getCityName(context, it.latitude, it.longitude)
                    }
                    if (_currentCity.value != detectedCity) {
                        setCurrentCity(detectedCity)
                        fetchCanteensForCity(detectedCity)
                    }
                }
            } catch (e: Exception) {
                // Handle any errors
            }
        }
    }
}


suspend fun getCityName(context: Context, latitude: Double, longitude: Double): String = withContext(
    Dispatchers.IO) {
    val geocoder = Geocoder(context, Locale.getDefault())
    try {
        val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
        return@withContext addresses?.firstOrNull()?.locality ?: "Unknown"
    } catch (e: IOException) {
        return@withContext "Error: ${e.message}"
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
suspend fun getCityNameAsync(context: Context, latitude: Double, longitude: Double): String = withContext(Dispatchers.IO) {
    val geocoder = Geocoder(context, Locale.getDefault())
    try {
        val result = CompletableDeferred<String>()
        geocoder.getFromLocation(latitude, longitude, 1, object : Geocoder.GeocodeListener {
            override fun onGeocode(addresses: List<Address>) {
                result.complete(addresses.firstOrNull()?.locality ?: "Unknown")
            }
        })
        return@withContext result.await()
    } catch (e: IOException) {
        return@withContext "Error: ${e.message}"
    }
}

