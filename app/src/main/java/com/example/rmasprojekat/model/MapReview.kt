package com.example.rmasprojekat.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference

@Keep
data class MapReview(
    var id: String? = null,
    val type: String = "",
    val foodRating: Int = 0,
    val serviceRating: Int = 0,
    val crowdRating: Int = 0,
    val user: DocumentReference? = null,
    val timestamp: Long = System.currentTimeMillis()
)

