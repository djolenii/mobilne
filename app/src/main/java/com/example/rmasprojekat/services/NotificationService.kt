package com.example.rmasprojekat.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.rmasprojekat.MainActivity
import com.example.rmasprojekat.R

class NotificationService(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Canteen Reviews",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for new canteen reviews"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun showNotification(canteenName: String, reviewType: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_like)
            .setContentTitle("New Review for $canteenName")
            .setContentText("A new $reviewType review has been added")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    companion object {
        private const val CHANNEL_ID = "CanteenReviewChannel"
    }
}
