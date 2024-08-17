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
import com.example.rmasprojekat.model.Comment
import com.example.rmasprojekat.model.Like
import com.example.rmasprojekat.model.User
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


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

    private val _currentStatuses = MutableStateFlow<List<Status>>(emptyList())
    val currentStatuses: StateFlow<List<Status>> = _currentStatuses

    private var loadedCity: String? = null

    private val _statuses = MutableStateFlow<Map<String, Status>>(emptyMap())
    val statuses: StateFlow<Map<String, Status>> = _statuses

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
                    statusDoc.toObject(Status::class.java)?.apply {
                        id = statusDoc.id
                        fetchLikesAndComments(this, statusDoc.reference)
                    }
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
        updateCurrentStatuses()
        _isLoading.value = false
    }

    private suspend fun fetchLikesAndComments(status: Status, statusRef: DocumentReference) {
        val likesRef = statusRef.collection("likes")
        val commentsRef = statusRef.collection("comments")

        try {
            val likesResult = likesRef.get().await()
            status.likes = likesResult.documents.mapNotNull { likeDoc ->
                likeDoc.toObject(Like::class.java)?.apply { id = likeDoc.id }
            }

            val commentsResult = commentsRef.get().await()
            status.comments = commentsResult.documents.mapNotNull { commentDoc ->
                commentDoc.toObject(Comment::class.java)?.apply { id = commentDoc.id }
            }

            status.likesCount = status.likes.size
        } catch (e: Exception) {
            // Handle failure to fetch likes or comments
            status.likes = emptyList()
            status.comments = emptyList()
            status.likesCount = 0
        }
    }

    fun likeStatus(status: Status, user: User, authViewModel: AuthViewModel) {
        viewModelScope.launch {
            val canteenId = getCurrentCanteen()?.id ?: return@launch
            val statusRef = db.collection("canteens/$canteenId/statuses").document(status.id ?: return@launch)
            val likesRef = statusRef.collection("likes")
            val authorRef = db.collection("users").document(status.user?.id ?: return@launch)

            val existingLike = status.likes.find { it.user?.id == user.id }
            val newLikes: List<Like>
            val newLikesCount: Int

            if (existingLike != null) {
                // Unlike
                likesRef.document(existingLike.id ?: "").delete().await()
                newLikes = status.likes.filter { it.id != existingLike.id }
                newLikesCount = status.likesCount - 1
                authorRef.update("likesCount", FieldValue.increment(-1)).await()
            } else {
                // Like
                val newLike = Like(user = db.document("users/${user.id}"))
                val addedLikeDoc = likesRef.add(newLike).await()
                newLike.id = addedLikeDoc.id
                newLikes = status.likes + newLike
                newLikesCount = status.likesCount + 1
                authorRef.update("likesCount", FieldValue.increment(1)).await()
            }

            // Update the status document with the new likes count
            statusRef.update("likesCount", newLikesCount).await()

            // Update local state immediately
            val updatedStatus = status.copy(
                likes = newLikes,
                likesCount = newLikesCount
            )
            _statuses.value = _statuses.value.toMutableMap().apply {
                put(status.id ?: "", updatedStatus)
            }

            // Refetch the author's data to reflect the new likesCount
            authViewModel.fetchUserDetails(authorRef, forceRefresh = true) { updatedUser ->
                updatedUser?.let {
                    authViewModel.userCache[it.id] = it // Update the cache with the new data
                }
            }
            // Refetch user information
            authViewModel.refreshCurrentUser()
        }
    }


    fun addComment(status: Status, user: User, commentText: String) {
        viewModelScope.launch {
            val canteenId = getCurrentCanteen()?.id ?: return@launch
            val statusRef = db.collection("canteens/$canteenId/statuses").document(status.id ?: return@launch)
            val commentsRef = statusRef.collection("comments")

            val newComment = Comment(
                user = db.document("users/${user.id}"),
                comment = commentText
            )

            val addedCommentDoc = commentsRef.add(newComment).await()
            newComment.id = addedCommentDoc.id

            // Update local state immediately
            val updatedStatus = status.copy(comments = status.comments + newComment)
            _statuses.value = _statuses.value.toMutableMap().apply {
                put(status.id ?: "", updatedStatus)
            }

            // Update the status document with the new comment
            statusRef.update("comments", FieldValue.arrayUnion(newComment)).await()

            // Optionally, you could update the user's comment count if you're tracking that
            // db.collection("users").document(user.id).update("commentCount", FieldValue.increment(1)).await()

            // Refresh the statuses for the current canteen to ensure consistency
            fetchStatusesForCurrentCanteen()
        }
    }

    fun deleteComment(status: Status, comment: Comment) {
        viewModelScope.launch {
            val canteenId = getCurrentCanteen()?.id ?: return@launch
            val statusRef = db.collection("canteens/$canteenId/statuses").document(status.id ?: return@launch)
            val commentsRef = statusRef.collection("comments")

            try {
                commentsRef.document(comment.id ?: return@launch).delete().await()

                // Update local state
                val updatedStatus = status.copy(
                    comments = status.comments.filter { it.id != comment.id }
                )
                _statuses.value = _statuses.value.toMutableMap().apply {
                    put(status.id ?: "", updatedStatus)
                }

                // Update the status document
                statusRef.update("comments", FieldValue.arrayRemove(comment)).await()

            } catch (e: Exception) {
                // Handle error (e.g., show a toast message)
                println("Error deleting comment: ${e.message}")
            }
        }
    }

    private fun updateCanteensAndStatuses(canteenId: String, updatedStatus: Status) {
        _canteens.value = _canteens.value?.map { canteen ->
            if (canteen.id == canteenId) {
                canteen.copy(statuses = canteen.statuses.map {
                    if (it.id == updatedStatus.id) updatedStatus else it
                })
            } else canteen
        }
        updateCurrentStatuses()
    }


    fun fetchStatusesForCurrentCanteen() {
        val currentCanteen = getCurrentCanteen() ?: return

        viewModelScope.launch {
            _isLoading.value = true

            try {
                val statusesRef = db.collection("canteens/${currentCanteen.id}/statuses")
                val statusesResult = statusesRef.get().await()

                val updatedStatuses = statusesResult.documents.mapNotNull { statusDoc ->
                    statusDoc.toObject(Status::class.java)?.apply {
                        id = statusDoc.id
                        fetchLikesAndComments(this, statusDoc.reference)
                    }
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

                updateCurrentStatuses()

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
            updateCurrentStatuses()
        }
    }

    fun setCurrentCity(city: String) {
        _currentCity.value = city
    }


    private fun updateCurrentStatuses() {
        val currentStatusMap = getCurrentCanteen()?.statuses
            ?.sortedByDescending { it.timestamp }
            ?.associateBy { it.id ?: "" } ?: emptyMap()
        _statuses.value = currentStatusMap
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