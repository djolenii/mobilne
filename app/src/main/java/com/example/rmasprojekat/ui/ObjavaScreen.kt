package com.example.rmasprojekat.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import coil.compose.rememberImagePainter
import com.example.rmasprojekat.viewmodel.CanteenViewModel
import com.example.rmasprojekat.viewmodel.AuthViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

@Composable
fun ObjavaScreen(canteenViewModel: CanteenViewModel, authViewModel: AuthViewModel) {
    var statusText by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    var isLoading by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf("") }

    val currentUser = authViewModel.currentUser.collectAsState().value
    val currentCanteen = canteenViewModel.getCurrentCanteen()

    val canteenName = currentCanteen?.name ?: "N/A"
    val canteenAddress = currentCanteen?.address ?: "N/A"

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "$canteenName \n ($canteenAddress)",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(16.dp))

        currentUser?.let { user ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = rememberAsyncImagePainter(model = user.imageUrl),
                    contentDescription = "User Profile Picture",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .padding(8.dp)
                )

                OutlinedTextField(
                    value = statusText,
                    onValueChange = { statusText = it },
                    label = { Text("Unesite status") },
                    modifier = Modifier.fillMaxWidth().weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedImageUri != null) {
                Image(
                    painter = rememberAsyncImagePainter(model = selectedImageUri),
                    contentDescription = "Selected Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = { imagePickerLauncher.launch("image/*") }) {
                Text("Odaberi sliku")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    isLoading = true
                    feedbackMessage = ""
                    postStatusToFirestore(
                        statusText,
                        selectedImageUri,
                        user.id,
                        currentCanteen?.id ?: ""
                    ){ success, message ->
                        isLoading = false
                        feedbackMessage = message
                        if (success) {
                            // Clear the text and image
                            statusText = ""
                            selectedImageUri = null

                            // Refetch statuses (this will depend on how you've set up the CanteenViewModel)
                            canteenViewModel.fetchStatusesForCurrentCanteen()
                        }
                    }
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Green)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White)
                } else {
                    Text("Objavi")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (feedbackMessage.isNotEmpty()) {
                Text(feedbackMessage, color = if (feedbackMessage == "Status successfully added!") Color.Green else Color.Red)
            }
        }
    }
}

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
    val status = hashMapOf(
        "user" to db.collection("users").document(userId),
        "message" to statusText,
        "timestamp" to com.google.firebase.Timestamp.now(),
        "imageUrl" to imageUrl,
        "likes" to 0
    )
    db.collection("canteens").document(canteenId)
        .collection("statuses")
        .add(status)
        .addOnSuccessListener {
            onComplete(true, "Status successfully added!")
        }
        .addOnFailureListener { e ->
            onComplete(false, "Error adding status: $e")
        }
}