package com.example.rmasprojekat.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.example.rmasprojekat.R
import com.example.rmasprojekat.model.Comment
import com.example.rmasprojekat.model.Status
import com.example.rmasprojekat.model.User
import com.example.rmasprojekat.viewmodel.AuthViewModel
import com.example.rmasprojekat.viewmodel.CanteenViewModel
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.Locale


@Composable
fun StatusItem(status: Status, authViewModel: AuthViewModel, canteenViewModel: CanteenViewModel, modifier: Modifier = Modifier) {
    var comment by remember { mutableStateOf("") }

    var username by remember { mutableStateOf("Loading...") }
    var userImage by remember { mutableStateOf<String?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var userDetails by remember { mutableStateOf<User?>(null) }

    val currentUser by authViewModel.currentUser.collectAsState()
    val isLiked by remember(status, currentUser) {
        derivedStateOf { status.likes.any { it.user?.id == currentUser?.id } }
    }

    var showAllComments by remember { mutableStateOf(false) }
    val sortedComments = remember(status.comments) {
        status.comments.sortedBy { it.timestamp }
    }
    val displayedComments = if (showAllComments) sortedComments else sortedComments.takeLast(2)


    // Fetch the username and image from the reference
    LaunchedEffect(status.user) {
        status.user?.let { userRef ->
            authViewModel.fetchUserDetails(userRef) { user ->
                user?.let {
                    username = it.username
                    userImage = it.imageUrl
                    userDetails = it
                } ?: run {
                    username = "Unknown"
                }
            }
        }
    }

    // Dialog for user details
    if (showDialog) {
        status.user?.let { userRef ->
            LaunchedEffect(userRef) {
                authViewModel.fetchUserDetails(userRef, forceRefresh = true) { updatedUser ->
                    updatedUser?.let {
                        userDetails = it
                        showDialog = true
                    }
                }
            }
        }
    }

    if (showDialog && userDetails != null) {
        UserDetailsDialog(user = userDetails!!, onDismiss = { showDialog = false })
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { showDialog = true } // Show dialog on click
                ) {
                    // Optional User Image
                    AsyncImage(
                        model = userImage ?: "https://via.placeholder.com/150",
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(text = username, fontWeight = FontWeight.Bold)
                        Text(
                            text = formatTimestamp(status.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
                // Like and Delete Buttons

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                currentUser?.let { user ->
                                    canteenViewModel.likeStatus(status, user, authViewModel)
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(
                                    id = R.drawable.ic_like
                                ),
                                contentDescription = "Like",
                                tint = if (isLiked) Color.Red else Color.Gray
                            )
                        }
                        Text(
                            text = "${status.likesCount}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                    if (currentUser?.id == status.user?.id) {
                        IconButton(onClick = {
                            authViewModel.deleteStatus(status = status, canteenViewModel = canteenViewModel)
                        }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color.Gray
                            )
                        }
                    }

            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(text = status.message)

            // Optional Status Image
            status.imageUrl?.let { imageUrl ->
                Spacer(modifier = Modifier.height(8.dp))
                Image(
                    painter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(true)
                            .scale(coil.size.Scale.FILL)
                            .build()
                    ),
                    contentDescription = "Status Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp) // Adjust height as needed
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp) // Smanjeno vertikalno padding
                    .height(50.dp), // Smanjeno visina input box-a
                placeholder = {
                    Text(
                        "Dodajte Komentar...",
                        style = MaterialTheme.typography.bodySmall // Manji font za placeholder
                    )
                },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (comment.isNotBlank()) {
                                currentUser?.let { user ->
                                    canteenViewModel.addComment(status, user, comment)
                                    comment = ""
                                }
                            }
                        },
                        enabled = comment.isNotBlank(),
                        modifier = Modifier.size(32.dp) // Smanjena veličina dugmeta
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send comment",
                            tint = if (comment.isNotBlank()) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(18.dp) // Smanjena veličina ikone
                        )
                    }
                },
                maxLines = 1,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                ),
                textStyle = MaterialTheme.typography.bodySmall
            )

            Spacer(modifier = Modifier.height(8.dp))

            displayedComments.forEach { comment ->
                CommentItem(comment, authViewModel, canteenViewModel, status)
            }

            if (status.comments.size > 2 && !showAllComments) {
                TextButton(onClick = { showAllComments = true }) {
                    Text("See all ${status.comments.size} comments")
                }
            }
        }
    }
}

@Composable
fun CommentItem(
    comment: Comment,
    authViewModel: AuthViewModel,
    canteenViewModel: CanteenViewModel,
    status: Status
) {
    var username by remember { mutableStateOf("Loading...") }
    var userImage by remember { mutableStateOf<String?>(null) }
    val currentUser by authViewModel.currentUser.collectAsState()

    LaunchedEffect(comment.user) {
        comment.user?.let { userRef ->
            authViewModel.fetchUserDetails(userRef) { user ->
                user?.let {
                    username = it.username
                    userImage = it.imageUrl
                } ?: run {
                    username = "Unknown"
                }
            }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        AsyncImage(
            model = userImage ?: "https://via.placeholder.com/150",
            contentDescription = "Profile Picture",
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = username,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = comment.comment,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatTimestamp(comment.timestamp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (currentUser?.id == comment.user?.id) {
                        IconButton(
                            onClick = {
                                canteenViewModel.deleteComment(status, comment)
                            },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete comment",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserDetailsDialog(
    user: User,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        },
        title = {
            Text(
                text = "Detalji o korisniku",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                AsyncImage(
                    model = user.imageUrl ?: "https://via.placeholder.com/150",
                    contentDescription = "Profile Picture",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = user.username,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Text(
                    text = "Član od: ${SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(user.memberSince.toDate())}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceAround,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Objave",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${user.postCount}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Sviđanja",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "${user.likesCount}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        },
        shape = MaterialTheme.shapes.medium,
        containerColor = MaterialTheme.colorScheme.surface
    )
}

fun formatTimestamp(timestamp: Timestamp): String {
        val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
        return sdf.format(timestamp.toDate())
    }