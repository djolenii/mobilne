package com.example.rmasprojekat.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.rmasprojekat.viewmodel.AuthViewModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilScreen(authViewModel: AuthViewModel) {
    val user by authViewModel.currentUser.collectAsState()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf("") }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
        uri?.let {
            isLoading = true
            feedbackMessage = ""
            uploadProfileImage(it, user?.id ?: "") { success, message, imageUrl ->
                isLoading = false
                feedbackMessage = message
                if (success && imageUrl != null) {
                    authViewModel.updateProfileImageUrl(imageUrl)
                }
            }
        }
    }


    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val formattedMemberSince = user?.memberSince?.toDate()?.let { dateFormat.format(it) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil") },
                actions = {
                    IconButton(onClick = { /* implementiraj uredi profil  */ }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
                    }
                    IconButton(onClick = { authViewModel.signOut() }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Sign Out")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = selectedImageUri ?: user?.imageUrl ?: "https://cdn.vectorstock.com/i/500p/08/19/gray-photo-placeholder-icon-design-ui-vector-35850819.jpg",
                contentDescription = "Profile Picture",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .clickable { imagePickerLauncher.launch("image/*") },
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = user?.username ?: "Username",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (feedbackMessage.isNotEmpty()) {
                Text(feedbackMessage, color = if (isLoading) Color.Blue else Color.Green)
            }


            Text(
                text = user?.email ?: "email@example.com",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            ProfileInfoCard(title = "Statusi", value = user?.postCount?.toString() ?: "0")
            ProfileInfoCard(title = "Sviđanja", value = user?.likesCount?.toString() ?: "0")
            ProfileInfoCard(title = "Član od", value = formattedMemberSince ?: "N/A")
        }
    }
}

@Composable
fun ProfileInfoCard(title: String, value: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

private fun uploadProfileImage(
    imageUri: Uri,
    userId: String,
    onComplete: (Boolean, String, String?) -> Unit
) {
    val storage = Firebase.storage
    val db = Firebase.firestore

    val imageRef = storage.reference.child("profile_images/$userId/${System.currentTimeMillis()}.jpg")
    imageRef.putFile(imageUri)
        .addOnSuccessListener {
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                db.collection("users").document(userId)
                    .update("imageUrl", uri.toString())
                    .addOnSuccessListener {
                        onComplete(true, "Profilna slika uspešno ažurirana", uri.toString())
                    }
                    .addOnFailureListener { e ->
                        onComplete(false, "Greška pri ažuriranju profilne slike: $e", null)
                    }
            }
        }
        .addOnFailureListener { e ->
            onComplete(false, "Greška pri upload-u slike: $e", null)
        }
}
