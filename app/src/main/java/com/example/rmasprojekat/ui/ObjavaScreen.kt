package com.example.rmasprojekat.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.rmasprojekat.viewmodel.CanteenViewModel
import com.example.rmasprojekat.viewmodel.AuthViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ObjavaScreen(canteenViewModel: CanteenViewModel, authViewModel: AuthViewModel) {
    var statusText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf("") }

    val currentUser = authViewModel.currentUser.collectAsState().value
    val currentCanteen = canteenViewModel.getCurrentCanteen()

    val canteenName = currentCanteen?.name ?: "N/A"
    val canteenNickname = currentCanteen?.nickname ?: "N/A"

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = canteenName,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = canteenNickname,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.height(42.dp))

            currentUser?.let { user ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Image(
                        painter = rememberAsyncImagePainter(model = user.imageUrl),
                        contentDescription = "User Profile Picture",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    TextField(
                        value = statusText,
                        onValueChange = { statusText = it },
                        placeholder = { Text("Šta se jede danas?") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        colors = TextFieldDefaults.textFieldColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedImageUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(model = selectedImageUri),
                    contentDescription = "Selected Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                OutlinedButton(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = "Add Photo")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Dodaj sliku")
                }

                Spacer(modifier = Modifier.width(16.dp))

                Button(
                    onClick = {
                        isLoading = true
                        feedbackMessage = ""
                        postStatusToFirestore(
                            statusText,
                            selectedImageUri,
                            currentUser?.id ?: "",
                            currentCanteen?.id ?: ""
                        ) { success, message ->
                            isLoading = false
                            feedbackMessage = message
                            if (success) {
                                statusText = ""
                                selectedImageUri = null
                                authViewModel.refreshCurrentUser()
                                canteenViewModel.fetchStatusesForCurrentCanteen()
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier.weight(1f)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Objavi")
                    }
                }
            }

            if (feedbackMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = feedbackMessage,
                    color = if (feedbackMessage.contains("successfully")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// Funkcije postStatusToFirestore i saveStatusToFirestore ostaju iste

private fun postStatusToFirestore(
    statusText: String,
    imageUri: Uri?,
    userId: String,
    canteenId: String,
    onComplete: (Boolean, String) -> Unit
) {
    val db = Firebase.firestore
    val storage = Firebase.storage

    if (imageUri != null) {
        val imageRef = storage.reference.child("images/${userId}/${System.currentTimeMillis()}.jpg")
        imageRef.putFile(imageUri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    saveStatusToFirestore(db, statusText, uri.toString(), userId, canteenId, onComplete)
                }
            }
            .addOnFailureListener { e ->
                onComplete(false, "Error uploading image: $e")
            }
    } else {
        saveStatusToFirestore(db, statusText, null, userId, canteenId, onComplete)
    }
}

private fun saveStatusToFirestore(
    db: FirebaseFirestore,
    statusText: String,
    imageUrl: String?,
    userId: String,
    canteenId: String,
    onComplete: (Boolean, String) -> Unit
) {
    val userRef = db.collection("users").document(userId)

    // First, read the user's current postCount
    userRef.get().addOnSuccessListener { document ->
        val currentPostCount = document.getLong("postCount") ?: 0

        // Start the transaction
        db.runTransaction { transaction ->
            // Create the status data
            val status = hashMapOf(
                "user" to userRef,
                "message" to statusText,
                "timestamp" to com.google.firebase.Timestamp.now(),
                "imageUrl" to imageUrl,
                "likesCount" to 0
            )

            // Add the status to the canteen's statuses collection
            val canteenStatusRef = db.collection("canteens").document(canteenId)
                .collection("statuses").document()
            transaction.set(canteenStatusRef, status)

            // Increment the user's postCount
            transaction.update(userRef, "postCount", currentPostCount + 1)

        }.addOnSuccessListener {
            onComplete(true, "Status successfully added!")
        }.addOnFailureListener { e ->
            onComplete(false, "Error adding status: $e")
        }

    }.addOnFailureListener { e ->
        onComplete(false, "Error fetching user data: $e")
    }
}
