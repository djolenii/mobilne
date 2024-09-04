package com.example.rmasprojekat.services

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class SubscriptionManager {
    private val db = Firebase.firestore

    suspend fun subscribeToCanteen(userId: String, canteenId: String) {
        val userDoc = db.collection("subscriptions").document(userId)
        val snapshot = userDoc.get().await()

        if (!snapshot.exists()) {
            // If the document doesn't exist, create it with the subscription
            userDoc.set(mapOf(canteenId to FieldValue.serverTimestamp()), SetOptions.merge()).await()
        } else {
            // If the document exists, update it
            userDoc.update(mapOf(canteenId to FieldValue.serverTimestamp())).await()
        }
    }

    suspend fun unsubscribeFromCanteen(userId: String, canteenId: String) {
        db.collection("subscriptions").document(userId).update(
            mapOf(canteenId to FieldValue.delete())
        ).await()
    }

    suspend fun isSubscribedToCanteen(userId: String, canteenId: String): Boolean {
        val doc = db.collection("subscriptions").document(userId).get().await()
        return doc.exists() && doc.get(canteenId) != null
    }

    suspend fun getSubscribedCanteens(userId: String): Map<String, Timestamp> {
        val doc = db.collection("subscriptions").document(userId).get().await()
        return if (doc.exists()) {
            doc.data?.mapValues { (_, value) -> value as Timestamp } ?: emptyMap()
        } else {
            emptyMap()
        }
    }
}