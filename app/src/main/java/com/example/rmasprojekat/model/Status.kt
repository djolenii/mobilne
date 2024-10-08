package com.example.rmasprojekat.model

import com.google.firebase.Timestamp
import androidx.annotation.Keep
import com.google.firebase.firestore.DocumentReference

@Keep
data class Status(
    var id: String? = null,
    val user: DocumentReference? = null,
    val message: String = "",
    val timestamp: Timestamp = Timestamp.now(),
    val imageUrl: String? = null,
    var likesCount: Int = 0,
    var likes: List<Like> = emptyList(),
    var comments: List<Comment> = emptyList()
)
