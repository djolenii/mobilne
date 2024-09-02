package com.example.rmasprojekat.location

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.os.Build
import androidx.annotation.RequiresApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Locale

object LocationUtils {
    suspend fun getCityName(context: Context, latitude: Double, longitude: Double): String = withContext(Dispatchers.IO) {
        val geocoder = Geocoder(context, Locale.getDefault())
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                getCityNameAsync(geocoder, latitude, longitude)
            } else {
                getCityNameSync(geocoder, latitude, longitude)
            }
        } catch (e: IOException) {
            "Error: ${e.message}"
        }
    }

    private suspend fun getCityNameSync(geocoder: Geocoder, latitude: Double, longitude: Double): String {
        val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)
        return addresses?.firstOrNull()?.locality ?: "Unknown"
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private suspend fun getCityNameAsync(geocoder: Geocoder, latitude: Double, longitude: Double): String {
        val result = CompletableDeferred<String>()
        geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
            result.complete(addresses.firstOrNull()?.locality ?: "Unknown")
        }
        return result.await()
    }
}