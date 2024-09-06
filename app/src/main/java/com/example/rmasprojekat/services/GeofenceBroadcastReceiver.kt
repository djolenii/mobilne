package com.example.rmasprojekat.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.*
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent



class GeofenceBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)

        if (geofencingEvent != null && geofencingEvent.hasError()) {
            Log.e("GeofenceReceiver", "Geofencing error: ${geofencingEvent.errorCode}")
            return
        }

        geofencingEvent?.let {
            when (geofencingEvent.geofenceTransition) {
                Geofence.GEOFENCE_TRANSITION_ENTER -> {
                    val triggeringGeofences = geofencingEvent.triggeringGeofences
                    triggeringGeofences?.let {
                        sendNotification(context, it)
                    }
                }
                Geofence.GEOFENCE_TRANSITION_EXIT -> {
                    // Handle exit transition if necessary
                }

                else -> {Log.d("GeofenceReceiver", "Unknown transition: ${geofencingEvent.geofenceTransition}")
                }
            }
        }
    }

    private fun sendNotification(context: Context, triggeringGeofences: List<Geofence>) {
        val notificationService = NotificationService(context)
        triggeringGeofences.forEach { geofence ->
            // Fetch canteen name using geofence.requestId (which is canteen ID)
            // For simplicity, we're using the ID as the name here
            notificationService.showNotification(
                "Nearby Canteen",
                "${geofence.requestId} is nearby."
            )
        }
    }
}