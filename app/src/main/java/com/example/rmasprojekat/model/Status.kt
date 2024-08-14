package com.example.rmasprojekat.model

import com.google.firebase.Timestamp
import androidx.annotation.Keep
import com.google.firebase.firestore.DocumentReference

@Keep
data class Status(
    val user: DocumentReference? = null,
    val message: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val imageUrl: String? = null,
    val likes: Int = 0
)
