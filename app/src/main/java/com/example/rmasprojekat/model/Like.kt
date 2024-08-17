package com.example.rmasprojekat.model

import com.google.firebase.Timestamp
import androidx.annotation.Keep
import com.google.firebase.firestore.DocumentReference

@Keep
data class Like(
    var id: String? = null,
    val user: DocumentReference? = null,
    val timestamp: Timestamp = Timestamp.now(),
)
