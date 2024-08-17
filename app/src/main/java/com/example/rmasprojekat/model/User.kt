package com.example.rmasprojekat.model

import com.google.firebase.Timestamp

import androidx.annotation.Keep

@Keep
data class User(
    val id: String = "",
    val username: String = "",
    val email: String = "",
    val imageUrl: String? = null,
    val memberSince: Timestamp = Timestamp.now(),
    val postCount: Int = 0,
    var likesCount: Int = 0
)
