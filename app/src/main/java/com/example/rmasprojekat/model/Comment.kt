package com.example.rmasprojekat.model

import com.google.firebase.Timestamp
import androidx.annotation.Keep
import com.google.firebase.firestore.DocumentReference

@Keep
data class Comment(
    var id: String? = null,
    val user: DocumentReference? = null,
    val comment: String = "",
    val timestamp: Timestamp = Timestamp.now(),
)
