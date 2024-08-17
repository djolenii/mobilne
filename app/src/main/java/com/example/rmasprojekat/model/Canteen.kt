package com.example.rmasprojekat.model

import androidx.annotation.Keep

@Keep
data class Canteen(
    var id: String? = null,
    val name: String = "",
    val nickname: String = "",
    val address: String = "",
    val city: String = "",
    val radnoVreme: String = "",
    val telefon: String = "",
    val kapacitet: Int = 0,
    val imageUrl: String = "",
    val order: Int = 0,
    var reviews: List<Review> = emptyList(),
    var statuses: List<Status> = emptyList()
)