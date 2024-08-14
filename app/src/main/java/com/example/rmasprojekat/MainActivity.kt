package com.example.rmasprojekat

import android.Manifest
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.rmasprojekat.ui.AuthScreen
import com.example.rmasprojekat.ui.MainScreen
import com.example.rmasprojekat.ui.MapScreen
import com.example.rmasprojekat.ui.theme.RMASProjekatTheme
import com.example.rmasprojekat.viewmodel.AuthState
import com.example.rmasprojekat.viewmodel.AuthViewModel
import com.example.rmasprojekat.viewmodel.CanteenViewModel
import com.example.rmasprojekat.viewmodel.LocationViewModel
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsResponse
import com.google.android.gms.location.Priority
import com.google.android.gms.location.SettingsClient
import com.google.android.gms.tasks.Task

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()
    private val canteenViewModel: CanteenViewModel by viewModels()
    private val locationViewModel: LocationViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        ) {
            checkLocationEnabled()
        } else {
            Toast.makeText(this, "Permissions denied. Please allow location access.", Toast.LENGTH_LONG).show()
        }
    }

    private val enableLocationRequestLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            locationViewModel.setLocationEnabled(true)
            canteenViewModel.checkAndUpdateLocation(this)
        } else {
            locationViewModel.setLocationEnabled(false)
            Toast.makeText(this, "Location services are required to use this app.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installSplashScreen()
        checkPermissions()
        setContent {
            RMASProjekatTheme {
                val authState by authViewModel.authState.collectAsState()

                when (authState) {
                    AuthState.Authenticated ->  MainContent()
                    AuthState.Initial -> AuthScreen(authViewModel)
                    is AuthState.Error -> AuthScreen(authViewModel)
                    AuthState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }

    @Composable
    fun MainContent() {
        var showMap by remember { mutableStateOf(false) }

        if (showMap) {
            MapScreen(onBackClicked = { showMap = false })
        } else {
            MainScreen(
                isLocationEnabled = locationViewModel.isLocationEnabled.value ?: false,
                canteenViewModel = canteenViewModel,
                onShowMapClicked = { showMap = true },
                authViewModel = authViewModel
            )
        }
    }

    private fun checkPermissions() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                checkLocationEnabled()
            }
            else -> {
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    private fun checkLocationEnabled() {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) {
            enableLocationServices()
        } else {
            locationViewModel.setLocationEnabled(true)
            canteenViewModel.checkAndUpdateLocation(this)
        }
    }

    private fun enableLocationServices() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000L).build()

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        val settingsClient: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = settingsClient.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            locationViewModel.setLocationEnabled(true)
            canteenViewModel.checkAndUpdateLocation(this)
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    enableLocationRequestLauncher.launch(
                        IntentSenderRequest.Builder(exception.resolution).build()
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            } else {
                locationViewModel.setLocationEnabled(false)
                Toast.makeText(this, "Location services are required to use this app.", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            checkLocationEnabled()
        }
    }
}