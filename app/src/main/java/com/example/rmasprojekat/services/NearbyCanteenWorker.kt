package com.example.rmasprojekat.services
import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.work.*
import com.example.rmasprojekat.model.Canteen
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class NearbyCanteenWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    private val notificationService = NotificationService(context)
    private val geofencingClient = LocationServices.getGeofencingClient(context)

    override suspend fun doWork(): Result {
        val canteens = getCanteens()
        setupGeofences(canteens)
        return Result.success()
    }

    private suspend fun getCanteens(): List<Canteen> {
        // Implement this method to fetch canteens from your database
        return withContext(Dispatchers.IO) {
            Firebase.firestore.collection("canteens").get().await().toObjects(Canteen::class.java)
        }
    }

    private fun setupGeofences(canteens: List<Canteen>) {
        val geofenceList = canteens.map { canteen ->
            canteen.id?.let {
                Geofence.Builder()
                    .setRequestId(it)
                    .setCircularRegion(
                        canteen.coordinates?.latitude ?: 0.0,
                        canteen.coordinates?.longitude ?: 0.0,
                        300f // 300 meters radius
                    )
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                    .build()
            }
        }

        val geofencingRequest = GeofencingRequest.Builder().apply {
            setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            addGeofences(geofenceList)
        }.build()

        val geofencePendingIntent: PendingIntent by lazy {
            val intent = Intent(applicationContext, GeofenceBroadcastReceiver::class.java)
            PendingIntent.getBroadcast(applicationContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }

        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).addOnSuccessListener {
                // Geofences added successfully
            }.addOnFailureListener {
                // Failed to add geofences
            }
        }
    }
}