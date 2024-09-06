package com.example.rmasprojekat.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.rmasprojekat.model.Review
import com.example.rmasprojekat.model.User
import com.example.rmasprojekat.viewmodel.AuthViewModel
import com.example.rmasprojekat.viewmodel.CanteenViewModel

@Composable
fun ReviewSection(
    canteenId: String,
    reviews: List<Review>,
    canteenViewModel: CanteenViewModel,
    authViewModel: AuthViewModel,
    currentUser: User?
) {
    var showAddReviewForm by remember { mutableStateOf(false) }
    var userHasReviewed by remember { mutableStateOf(false) }

    LaunchedEffect(reviews, currentUser) {
        userHasReviewed = reviews.any { it.user?.id == currentUser?.id }
    }

    Column {
        Button(
            onClick = { showAddReviewForm = !showAddReviewForm },
            enabled = !userHasReviewed,
            modifier = Modifier.align(Alignment.End).padding(8.dp)
        ) {
            Text(if (showAddReviewForm) "Cancel" else "Add Review")
        }

        if (showAddReviewForm) {
            AddReviewForm(
                canteenId = canteenId,
                canteenViewModel = canteenViewModel,
                onReviewAdded = {
                    showAddReviewForm = false
                    userHasReviewed = true
                },
                currentUser = currentUser
            )
        }

        LazyColumn {
            items(reviews) { review ->
                ReviewItem(review, authViewModel)
            }
        }
    }
}

@Composable
fun ReviewItem(review: Review, authViewModel: AuthViewModel) {
    var username by remember { mutableStateOf("Loading...") }

    LaunchedEffect(review.user) {
        review.user?.let { userRef ->
            authViewModel.fetchUserDetails(userRef) { user ->
                username = user?.username ?: "Unknown"
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

@Composable
fun AddReviewForm(
    canteenId: String,
    canteenViewModel: CanteenViewModel,
    onReviewAdded: () -> Unit,
    currentUser: User?
) {
    var rating by remember { mutableStateOf(0f) }
    var comment by remember { mutableStateOf("") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Add Your Review", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            Text("Rating: ${rating.toInt()}/5")
            Slider(
                value = rating,
                onValueChange = { rating = it },
                valueRange = 0f..5f,
                steps = 4
            )

            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = comment,
                onValueChange = { comment = it },
                label = { Text("Your comment") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val newReview = Review(
                        rating = rating,
                        comment = comment
                    )
                    canteenViewModel.addReview(canteenId, newReview, currentUser)
                    onReviewAdded()
                },
                modifier = Modifier.align(Alignment.End)
            ) {
                Text("Submit Review")
            }
        }
    }
}