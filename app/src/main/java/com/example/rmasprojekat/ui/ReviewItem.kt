package com.example.rmasprojekat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.rmasprojekat.model.Review
import androidx.compose.runtime.*
import com.example.rmasprojekat.viewmodel.AuthViewModel
import com.example.rmasprojekat.viewmodel.CanteenViewModel

@Composable
fun ReviewItem(review: Review, authViewModel: AuthViewModel) {

    var username by remember { mutableStateOf("Loading...") }
    var userImage by remember { mutableStateOf<String?>(null) }

    // Fetch the username from the reference
    LaunchedEffect(review.user) {
        review.user?.let { userRef ->
            authViewModel.fetchUserDetails(userRef) { user ->
                user?.let {
                    username = it.username
//                    userImage = it.imageUrl
                } ?: run {
                    username = "Unknown"
                }
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = username, fontWeight = FontWeight.Bold)
                Text(
                    text = "${review.rating} / 5",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = review.comment)
        }
    }
}
