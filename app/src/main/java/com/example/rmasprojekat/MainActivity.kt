package com.example.rmasprojekat

import android.Manifest
import android.annotation.SuppressLint
import android.content.IntentSender
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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.rmasprojekat.location.LocationManager
import com.example.rmasprojekat.services.ReviewListenerService
import com.example.rmasprojekat.ui.AuthScreen
import com.example.rmasprojekat.ui.MainScreen
import com.example.rmasprojekat.ui.MapScreen
import com.example.rmasprojekat.ui.theme.RMASProjekatTheme
import com.example.rmasprojekat.viewmodel.AuthState
import com.example.rmasprojekat.viewmodel.AuthViewModel
import com.example.rmasprojekat.viewmodel.CanteenViewModel
import com.example.rmasprojekat.viewmodel.LocationViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels()
    private val canteenViewModel: CanteenViewModel by viewModels()
    private lateinit var locationManager: LocationManager
    private lateinit var locationViewModel: LocationViewModel
    private lateinit var reviewListenerService: ReviewListenerService

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
            locationViewModel.checkLocationEnabled()
            canteenViewModel.checkAndUpdateLocation(this)
        } else {
            Toast.makeText(this, "Location services are required to use this app.", Toast.LENGTH_LONG).show()
        }
    }

    @SuppressLint("StateFlowValueCalledInComposition")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        locationManager = LocationManager(this)
        locationViewModel = LocationViewModel(locationManager)
        reviewListenerService = ReviewListenerService(this)

        installSplashScreen()
        checkLocationPermission()

        setContent {

            RMASProjekatTheme {
                val authState by authViewModel.authState.collectAsState()

                when (authState) {
                    is AuthState.Authenticated -> {
                        MainContent()
                        // Start listening for reviews when the user is authenticated
                        authViewModel.currentUser.value?.id?.let { userId ->
                            reviewListenerService.startListening(userId)
                        }
                    }
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
            MapScreen(onBackClicked = { showMap = false }, canteenViewModel = canteenViewModel, authViewModel = authViewModel)
        } else {
            MainScreen(
                isLocationEnabled = locationViewModel.isLocationEnabled.value ?: false,
                canteenViewModel = canteenViewModel,
                onShowMapClicked = { showMap = true },
                authViewModel = authViewModel
            )
        }
    }

    private fun checkLocationPermission() {
        if (locationViewModel.checkLocationPermission()) {
            checkLocationEnabled()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun checkLocationEnabled() {
        locationViewModel.checkLocationEnabled()
        if (locationViewModel.isLocationEnabled.value == true) {
            canteenViewModel.checkAndUpdateLocation(this)
        } else {
            enableLocationServices()
        }
    }

    private fun enableLocationServices() {
        locationViewModel.requestLocationSettings(
            onSuccess = {
                canteenViewModel.checkAndUpdateLocation(this)
            },
            onFailure = { exception ->
                try {
                    enableLocationRequestLauncher.launch(
                        IntentSenderRequest.Builder(exception.resolution).build()
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        )
    }

    override fun onResume() {
        super.onResume()
        if (locationViewModel.checkLocationPermission()) {
            checkLocationEnabled()
        }
    }
}