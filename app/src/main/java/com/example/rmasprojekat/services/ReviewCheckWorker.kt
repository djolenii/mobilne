package com.example.rmasprojekat.services
import android.content.Context
import androidx.work.*
import com.example.rmasprojekat.model.Canteen
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ReviewCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    private val notificationService = NotificationService(context)
    private val subscriptionManager = SubscriptionManager()
    private val db = Firebase.firestore

    override suspend fun doWork(): Result {
        val userId = inputData.getString("userId") ?: return Result.failure()

        val subscribedCanteens = subscriptionManager.getSubscribedCanteens(userId)
        subscribedCanteens.forEach { (canteenId, subscriptionTime) ->
            checkForNewReviews(userId, canteenId, subscriptionTime)
        }

        WorkManager.getInstance(applicationContext)
            .enqueue(OneTimeWorkRequestBuilder<ReviewCheckWorker>().build())

        return Result.success()
    }

    private suspend fun checkForNewReviews(userId: String, canteenId: String, subscriptionTime: Timestamp) {
        val newReviews = withContext(Dispatchers.IO) {
            db.collection("canteens").document(canteenId)
                .collection("mapReviews")
                .whereGreaterThan("timestamp", subscriptionTime.toDate())
                .get()
                .await()
        }

        if (!newReviews.isEmpty) {
            val canteen = withContext(Dispatchers.IO) {
                db.collection("canteens").document(canteenId).get().await().toObject(Canteen::class.java)
            }
            canteen?.let {
                notificationService.showNotification(
                    "New Review for ${it.name}",
                    "Someone added a new review for ${it.name}"
                )
            }
        }
    }
}