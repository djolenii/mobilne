package com.example.rmasprojekat.model

import androidx.annotation.Keep
import com.google.firebase.firestore.DocumentReference

@Keep
data class Review(
    var id: String? = null,
    val user: DocumentReference? = null,
    val rating: Float = 0f,
    val comment: String = ""
)
