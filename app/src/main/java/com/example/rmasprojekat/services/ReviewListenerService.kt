package com.example.rmasprojekat.services

import android.content.Context
import androidx.lifecycle.ViewModel
import com.example.rmasprojekat.model.MapReview
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReviewListenerService(private val context: Context)  {
    private val db = Firebase.firestore
    private val notificationService = NotificationService(context)
    private val subscriptionManager = SubscriptionManager()
    private val scope = CoroutineScope(Dispatchers.IO)


    fun startListening(userId: String) {
        scope.launch {
            val subscribedCanteens = subscriptionManager.getSubscribedCanteens(userId)
            subscribedCanteens.forEach { (canteenId, subscriptionTime) ->
                listenForNewReviews(userId, canteenId, subscriptionTime)
            }

            // Listen for changes in subscriptions
            withContext(Dispatchers.Main) {
                db.collection("subscriptions").document(userId)
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            return@addSnapshotListener
                        }

                        snapshot?.data?.forEach { (canteenId, timestamp) ->
                            if (timestamp is Timestamp && (subscribedCanteens[canteenId] == null || timestamp > subscribedCanteens[canteenId]!!)) {
                                listenForNewReviews(userId, canteenId, timestamp)
                            }
                        }
                    }
            }
        }
    }

    private fun listenForNewReviews(userId: String, canteenId: String, subscriptionTime: Timestamp) {
        db.collection("canteens").document(canteenId)
            .collection("mapReviews")
            .whereGreaterThan("timestamp", subscriptionTime.toDate())
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    return@addSnapshotListener
                }

                snapshot?.documentChanges?.forEach { change ->
                    if (change.type == DocumentChange.Type.ADDED) {
                        val review = change.document.toObject(MapReview::class.java)
                        if (review.user?.id != userId) { // Don't notify for user's own reviews
                            notificationService.showNotification(canteenId, review.type)
                        }
                    }
                }
            }
    }
}